package com.kika.inventory_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inventory")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", length = 255, nullable = false)
    private String productId;

    @Column(name = "available", nullable = false)
    private Integer available = 0;

    @Column(name = "reserved", nullable = false)
    private Integer reserved = 0;

}