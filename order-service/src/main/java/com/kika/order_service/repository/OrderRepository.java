package com.kika.order_service.repository;

import com.kika.order_service.entity.Order;
import com.kika.order_service.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByIdAndStatus(Long id, Status status);

    List<Order> findByStatusAndUpdatedAtBefore(Status status, Instant updatedAt);
}