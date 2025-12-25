package com.radioacademy.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashSet;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Set;

@Entity
@Table(name = "courses")
@Data // Genera getters, setters, toString, equals, y hashCode
@NoArgsConstructor // Constructor sin argumentos
@AllArgsConstructor // Constructor con todos los argumentos
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT") // Permite textos largos
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer hours; // Duración estimada

    @Column(nullable = false)
    private Boolean active; // Para ocultar cursos sin borrarlos

    // La relación
    // Un curso tiene UN PROFESOR
    // FetchType.EAGER: Siempre que se cargue un curso, se carga también su profesor

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "teacher_id", nullable = false)
    @JsonIgnore // Para evitar problemas de serialización (circular reference)
    private User teacher;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Module> modules;

    public List<Module> getModules() {
        return modules;
    }

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "course_students", // Nombre de la tabla intermedia
            joinColumns = @JoinColumn(name = "course_id"), // Columna del curso
            inverseJoinColumns = @JoinColumn(name = "user_id") // Columna del alumno
    )
    private Set<User> students = new HashSet<>();
}
