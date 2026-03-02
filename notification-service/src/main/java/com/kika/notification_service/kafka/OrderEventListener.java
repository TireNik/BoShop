package com.kika.notification_service.kafka;

import com.kika.avro.order.OrderCreatedEvent;
import com.kika.avro.order.OrderStatusChangedEvent;
import com.kika.notification_service.entity.Notification;
import com.kika.notification_service.dto.NotificationDto;
import com.kika.notification_service.repository.NotificationRepository;
import com.kika.notification_service.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final NotificationRepository notificationRepository;
    private final RateLimiterService rateLimiterService;

    @KafkaListener(topics = "order.created")
    public void handleOrderCreated(OrderCreatedEvent event) {
        NotificationDto dto = NotificationDto.builder()
                .userId(event.getUserId())
                .orderId(event.getOrderId())
                .type("ORDER_CREATED")
                .status("NEW")
                .message("Your order has been created successfully.")
                .build();
        sendNotification(dto);
    }

    @KafkaListener(topics = "order.status.changed")
    public void handleStatusChanged(OrderStatusChangedEvent event) {
        String message = switch (event.getNewStatus().toString()) {
            case "RESERVED" -> "Заказ #" + event.getOrderId() + " зарезервирован. Ожидаем оплату.";
            case "PAID" -> "Заказ #" + event.getOrderId() + " оплачен! Скоро отправим.";
            case "CANCELLED" -> "Заказ #" + event.getOrderId() + " отменён.";
            default -> "Статус заказа #" + event.getOrderId() + " изменён на " + event.getNewStatus();
        };

        NotificationDto dto = NotificationDto.builder()
                .userId(event.getUserId())
                .orderId(event.getOrderId())
                .type("STATUS_CHANGED")
                .status(event.getNewStatus().toString())
                .message(message)
                .build();
        sendNotification(dto);
    }

    private void sendNotification(NotificationDto dto) {
        if (!rateLimiterService.isAllowed(dto.getUserId())) {
            log.info("Rate limit exceeded for user: {}", dto.getUserId());
            return;
        }

        Notification notification = Notification.builder()
                .userId(dto.getUserId())
                .orderId(dto.getOrderId())
                .type(dto.getType())
                .status(dto.getStatus())
                .message(dto.getMessage())
                .sent(false)
                .createdAt(Instant.now())
                .build();

        notificationRepository.save(notification);
        log.info("Notification saved: {}", notification);
    }
}
