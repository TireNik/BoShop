package com.kika.order_service.service;

import com.kika.avro.order.OrderCreatedEvent;
import com.kika.avro.order.OrderStatusChangedEvent;
import com.kika.inventory.grpc.Inventory;
import com.kika.inventory.grpc.InventoryServiceGrpc;
import com.kika.order_service.dto.OrderItemDto;
import com.kika.order_service.dto.OrderRequestDto;
import com.kika.order_service.dto.OrderResponseDto;
import com.kika.order_service.entity.Order;
import com.kika.order_service.entity.OrderItem;
import com.kika.order_service.entity.Status;
import com.kika.order_service.mapper.OrderMapper;
import com.kika.order_service.repository.OrderItemRepository;
import com.kika.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderItemRepository orderItemRepository;
    private final InventoryServiceGrpc.InventoryServiceBlockingStub inventoryServiceBlockingStub;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public PagedModel<OrderResponseDto> getAll(Pageable pageable) {
        Page<Order> orders = orderRepository.findAll(pageable);
        Page<OrderResponseDto> dtoPage = orders.map(orderMapper::toOrderResponseDto);
        return new PagedModel<>(dtoPage);
    }

    @Override
    public OrderResponseDto getOne(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + id));
        return orderMapper.toOrderResponseDto(order);
    }

    @Override
    public List<OrderResponseDto> getMany(List<Long> ids) {
        return orderRepository.findAllById(ids).stream()
                .map(orderMapper::toOrderResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public OrderResponseDto create(OrderRequestDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        Order order = findByUserIdAndStatus(dto.getUserId(), Status.NEW)
                .orElse(null);

        if (order == null) {
            order = Order.builder()
                    .userId(dto.getUserId())
                    .status(Status.NEW)
                    .totalPrice(0.0)
                    .build();
        }
        orderRepository.save(order);
        log.info("Order created: {}", order.getId());
        return orderMapper.toOrderResponseDto(order);
    }

    @Override
    @Transactional
    public OrderResponseDto addProduct(Long id, OrderItemDto dto) {
        Order order = findByUserIdAndStatus(id, Status.NEW)
                .orElse(null);

        if (order == null) {
            order = Order.builder()
                    .userId(id)
                    .status(Status.NEW)
                    .build();
        }

        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .productId(dto.getProductId())
                .quantity(dto.getQuantity())
                .price(dto.getPrice())
                .build();

        orderItemRepository.save(orderItem);
        order.setTotalPrice(order.getTotalPrice() + orderItem.getPrice() * orderItem.getQuantity());
        orderRepository.save(order);
        log.info("Product added to order: {}", order.getId());
        return orderMapper.toOrderResponseDto(order);
    }

    @Override
    @Transactional
    public OrderResponseDto reserve(Long orderId, Long userId) {
        Order order = findByUserIdAndStatus(orderId, Status.NEW)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

        var request = Inventory.ReserveStockRequest.newBuilder()
                .setOrderId(orderId)
                .addAllItems(orderItems.stream()
                        .map(item -> Inventory.StockItem.newBuilder()
                                .setProductId(item.getProductId())
                                .setQuantity(item.getQuantity())
                                .build())
                        .toList())
                .build();

        Inventory.ReserveStockResponse response = inventoryServiceBlockingStub.reserveStock(request);

        if (response.getSuccess()) {
            order.setStatus(Status.RESERVED);
        } else {
            order.setStatus(Status.CANCELLED);
        }
        orderRepository.save(order);

        publishCreated(order);

        return orderMapper.toOrderResponseDto(order);
    }

    @Override
    @Transactional
    public OrderResponseDto pay(Long orderId, Long userId) {
        Order order = findByUserIdAndStatus(orderId, Status.RESERVED)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));


        String oldStatus = order.getStatus().name();

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        var commitRequest = Inventory.CommitReservationRequest.newBuilder()
                .setOrderId(orderId)
                .addAllItems(orderItems.stream()
                        .map(item -> Inventory.StockItem.newBuilder()
                                .setProductId(item.getProductId())
                                .setQuantity(item.getQuantity())
                                .build())
                        .toList())
                .build();

        inventoryServiceBlockingStub.commitReservation(commitRequest);


        order.setStatus(Status.PAID);
        orderRepository.save(order);

        publishStatusChanged(order, oldStatus);

        return orderMapper.toOrderResponseDto(order);
    }

    @Override
    public OrderResponseDto cancelOrder(Long orderId, Long userId) {
        Order order = findByUserIdAndStatus(orderId, Status.NEW)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!List.of(Status.NEW, Status.RESERVED).contains(order.getStatus())) {
            throw new IllegalArgumentException("Order cannot be cancelled in this status: " + order.getStatus());
        }

        String oldStatus = order.getStatus().name();
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

        if (order.getStatus() == Status.RESERVED) {
            var request = Inventory.CancelReservationRequest.newBuilder()
                    .setOrderId(orderId)
                    .addAllItems(orderItems.stream()
                            .map(item -> Inventory.StockItem.newBuilder()
                                    .setProductId(item.getProductId())
                                    .setQuantity(item.getQuantity())
                                    .build())
                            .toList())
                    .build();

            inventoryServiceBlockingStub.cancelReservation(request);
        }
        order.setStatus(Status.CANCELLED);
        orderRepository.save(order);

        publishStatusChanged(order, oldStatus);
        return orderMapper.toOrderResponseDto(order);
    }

    @Scheduled(fixedRate = 900000)
    @Transactional
    public void cleanup() {
        Instant threshold = Instant.now().minus(15, ChronoUnit.MINUTES);

        List<Order> orders = orderRepository.findByStatusAndUpdatedAtBefore(Status.NEW, threshold);

        for (Order order : orders) {
            cancelOrder(order.getId(), order.getUserId());
        }
    }

    private Optional<Order> findByUserIdAndStatus(Long userId, Status status) {
        return orderRepository.findByIdAndStatus(userId, status);
    }

    private void publishCreated(Order order) {
        var event = OrderCreatedEvent.newBuilder()
                .setOrderId(order.getId())
                .setUserId(order.getUserId())
                .setStatus(order.getStatus().name())
                .setOccurredAt(Instant.now())
                .build();

        kafkaTemplate.send("order.created", String.valueOf(order.getId()), event);
    }

    private void publishStatusChanged(Order order, String oldStatus) {
        var event = OrderStatusChangedEvent.newBuilder()
                .setOrderId(order.getId())
                .setUserId(order.getUserId())
                .setOldStatus(oldStatus)
                .setNewStatus(order.getStatus().name())
                .setOccurredAt(Instant.now())
                .build();
        kafkaTemplate.send("order.status.changed", String.valueOf(order.getId()), event);
    }

}