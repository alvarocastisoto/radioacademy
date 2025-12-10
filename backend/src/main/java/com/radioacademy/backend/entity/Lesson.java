package com.radioacademy.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "lessons")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    private String videoUrl; // Soi hay URL del video se guarda (oculta)

    private String pdfUrl; // Si hay URL del PDF se guarda (oculta)

    @Column(nullable = false)
    private Integer orderIndex;

    // RELACIÓN: Una lección pertenece a un Módulo
    @ManyToOne(fetch = FetchType.LAZY) // LAZY: No te traigas el módulo entero si no te lo pido
    @JoinColumn(name = "module_id", nullable = false)
    @JsonIgnore // Para evitar problemas de serialización (circular reference)
    private Module module;

}
