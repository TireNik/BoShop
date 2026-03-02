package com.kika.user_service.service.listener;

import com.kika.avro.UserCreatedEvent;
import com.kika.user_service.dto.UserDto;
import com.kika.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserListener {

    private final UserService userService;

    @KafkaListener(topics = "user-created", groupId = "user-service-group")
    public void handleUserCreatedEvent(UserCreatedEvent event) {
        UserDto userDto = new UserDto(
                event.getName().toString(),
                event.getEmail().toString()
        );
        userService.create(userDto);
    }
}
