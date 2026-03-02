package com.kika.notification_service.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Document(collection = "notifications")
public class Notification {

    @Id
    private String id;

    private Long userId;
    private Long orderId;
    private String type;
    private String status;
    private String message;
    private boolean sent;
    private Instant createdAt;
}