package com.kika.order_service.service;

import com.kika.inventory.grpc.Inventory;
import com.kika.inventory.grpc.InventoryServiceGrpc;
import org.junit.jupiter.api.extension.ExtendWith;

import com.kika.order_service.dto.OrderItemDto;
import com.kika.order_service.dto.OrderRequestDto;
import com.kika.order_service.dto.OrderResponseDto;
import com.kika.order_service.entity.Order;
import com.kika.order_service.entity.OrderItem;
import com.kika.order_service.entity.Status;
import com.kika.order_service.mapper.OrderMapper;
import com.kika.order_service.repository.OrderItemRepository;
import com.kika.order_service.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private InventoryServiceGrpc.InventoryServiceBlockingStub inventoryStub;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void create_whenNoExistingOrder_shouldCreateNew() {
        OrderRequestDto dto = OrderRequestDto.builder().userId(1L).build();
        Order newOrder = new Order();
        newOrder.setId(1L);
        newOrder.setUserId(1L);
        newOrder.setStatus(Status.NEW);
        OrderResponseDto responseDto = new OrderResponseDto(1L, 1L, Status.NEW,
                0.0, null, null);

        when(orderRepository.findByIdAndStatus(1L, Status.NEW)).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });
        when(orderMapper.toOrderResponseDto(any(Order.class))).thenReturn(responseDto);

        OrderResponseDto result = orderService.create(dto);

        assertNotNull(result);
    }

    @Test
    void addProduct_shouldAddItemAndUpdateTotal() {
        Order order = new Order();
        order.setId(1L);
        order.setTotalPrice(0.0);
        OrderItemDto dto = new OrderItemDto("prod1", 2, 50.0);
        OrderResponseDto responseDto = new OrderResponseDto(1L, 1L, Status.NEW, 100.0, null, null);

        when(orderRepository.findByIdAndStatus(1L, Status.NEW)).thenReturn(Optional.of(order));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toOrderResponseDto(order)).thenReturn(responseDto);

        OrderResponseDto result = orderService.addProduct(1L, dto);

        assertEquals(100.0, order.getTotalPrice());
        assertEquals(responseDto, result);
        verify(orderItemRepository).save(any(OrderItem.class));
    }

    @Test
    void reserve_whenSuccess_shouldSetReservedAndPublishCreated() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(100L);
        order.setStatus(Status.NEW);
        OrderItem item = new OrderItem();
        item.setProductId("prod1");
        item.setQuantity(1);

        Inventory.ReserveStockResponse successResponse = Inventory.ReserveStockResponse.newBuilder()
                .setSuccess(true)
                .build();

        when(orderRepository.findByIdAndStatus(1L, Status.NEW)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(item));
        when(inventoryStub.reserveStock(any())).thenReturn(successResponse);
        when(orderRepository.save(order)).thenReturn(order);

        orderService.reserve(1L, 1L);

        assertEquals(Status.RESERVED, order.getStatus());
        verify(kafkaTemplate).send(eq("order.created"), eq("1"), any());
    }

    @Test
    void reserve_whenFail_shouldSetCancelled() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(100L);
        order.setStatus(Status.NEW);
        OrderItem item = new OrderItem();
        item.setProductId("prod1");
        item.setQuantity(1);

        Inventory.ReserveStockResponse failResponse = Inventory.ReserveStockResponse.newBuilder()
                .setSuccess(false)
                .build();

        when(orderRepository.findByIdAndStatus(1L, Status.NEW)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(item));
        when(inventoryStub.reserveStock(any())).thenReturn(failResponse);

        orderService.reserve(1L, 1L);

        assertEquals(Status.CANCELLED, order.getStatus());
    }

    @Test
    void pay_shouldCommitReservationAndSetPaid() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(100L);
        order.setStatus(Status.RESERVED);
        OrderItem item = new OrderItem();
        item.setProductId("prod1");
        item.setQuantity(1);

        when(orderRepository.findByIdAndStatus(1L, Status.RESERVED)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(item));
        when(inventoryStub.commitReservation(any())).thenReturn(Inventory.CommitReservationResponse.getDefaultInstance());

        orderService.pay(1L, 1L);

        assertEquals(Status.PAID, order.getStatus());
        verify(kafkaTemplate).send(eq("order.status.changed"), eq("1"), any());
    }

    @Test
    void cancelOrder_whenReserved_shouldCancelReservation() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(100L);
        order.setStatus(Status.RESERVED);
        OrderItem item = new OrderItem();
        item.setProductId("prod1");
        item.setQuantity(1);

        when(orderRepository.findByIdAndStatus(1L, Status.NEW)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(item));

        orderService.cancelOrder(1L, 1L);

        assertEquals(Status.CANCELLED, order.getStatus());
        verify(inventoryStub).cancelReservation(any());
    }
}