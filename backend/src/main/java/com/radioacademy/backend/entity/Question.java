package com.radioacademy.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Integer points;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    @JsonIgnore
    private Quiz quiz;

    // 👇 CAMBIO 1: orphanRemoval = false (OBLIGATORIO)
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<Option> options = new ArrayList<>();

    // ✅ Identidad basada en ID
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Question question = (Question) o;
        return id != null && Objects.equals(id, question.id);
    }

    // 👇 CAMBIO 2: HashCode constante (OBLIGATORIO para JPA)
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}