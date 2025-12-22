package com.radioacademy.backend.entity;

import com.radioacademy.backend.enums.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true, length = 9)
    private String dni;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String surname;

    @Column(nullable = false, length = 9)
    private String phone;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private String region;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "terms_accepted", nullable = false)
    private boolean termsAccepted;

    // ✅ CORREGIDO: Relación bidireccional optimizada
    // 1. fetch = LAZY: Para que no cargue los cursos al listar usuarios (Rápido)
    // 2. mappedBy = "students": Porque la configuración de la tabla ya está en
    // Course.java
    @ManyToMany(mappedBy = "students", fetch = FetchType.LAZY)
    @JsonIgnore // Evita bucle infinito JSON y carga innecesaria
    private Set<Course> enrolledCourses = new HashSet<>();

    // =================================================================
    // 👇 MÉTODOS OBLIGATORIOS DE USER DETAILS 👇
    // =================================================================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null)
            return List.of();
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    // 🔥 SEGURIDAD: Evitamos que el hash del password viaje al frontend
    @Override
    @JsonIgnore
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    // =================================================================
    // 👆 FIN DE MÉTODOS OBLIGATORIOS 👆
    // =================================================================

    public void setPhone(String phone) {
        if (phone != null) {
            this.phone = phone.trim().replaceAll("\\s+", "");
        }
    }
}