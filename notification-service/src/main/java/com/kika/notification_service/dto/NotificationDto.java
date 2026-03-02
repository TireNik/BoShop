package com.kika.notification_service.dto;

import com.kika.notification_service.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * DTO for {@link Notification}
 */
@AllArgsConstructor
@Getter
@Builder
public class NotificationDto {
    private final Long userId;
    private final Long orderId;
    private final String type;
    private final String status;
    private final String message;
}