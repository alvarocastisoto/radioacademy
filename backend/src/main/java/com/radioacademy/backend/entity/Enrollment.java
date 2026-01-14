package com.radioacademy.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
@Table(name = "enrollments")
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Relación: Una matrícula pertenece a UN usuario
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties("enrollments")
    private User user;

    // Relación: Una matrícula pertenece a UN curso
    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    // Datos extra de la compra
    private BigDecimal amountPaid; // Cuánto pagó exactamente
    private String paymentId; // ID de transacción de Stripe

    @CreationTimestamp
    private LocalDateTime enrolledAt; // Fecha y hora de compra
}