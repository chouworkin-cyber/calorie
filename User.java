package com.example.calories.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "Users", schema = "dbo")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private Integer totalPoints = 0;

    @Column(name = "target_kcal") // เพิ่มเพื่อให้รับกับ UI ใน templates
    private Integer targetKcal;

    private Double weight;

    private String bmiStatus;

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime lastLogin;
}