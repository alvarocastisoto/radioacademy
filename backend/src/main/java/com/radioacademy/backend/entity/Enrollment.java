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

    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties("enrollments")
    private User user;

    
    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    
    private BigDecimal amountPaid; 
    private String paymentId; 

    @CreationTimestamp
    private LocalDateTime enrolledAt; 
}