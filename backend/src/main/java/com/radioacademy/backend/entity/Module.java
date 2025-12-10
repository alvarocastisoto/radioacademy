package com.radioacademy.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "modules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Module {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private Integer orderIndex;

    // RELACIÓN: Un módulo pertenece a un Curso
    @ManyToOne(fetch = FetchType.LAZY) // LAZY: No te traigas el curso entero si no te lo pido
    @JoinColumn(name = "course_id", nullable = false)
    @JsonIgnore // Para evitar problemas de serialización (circular reference)
    private Course course;

}
