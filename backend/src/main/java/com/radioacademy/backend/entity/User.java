package com.radioacademy.backend.entity;

import com.radioacademy.backend.enums.Role;
import com.fasterxml.jackson.annotation.JsonIgnore; // Importante para evitar bucles
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
public class User implements UserDetails { // ✅ Implementamos la interfaz

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

    // ✅ RELACIÓN CON CURSOS (La habías perdido, la vuelvo a poner)
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "course_students", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "course_id"))
    @JsonIgnore // Evita que al pedir el usuario se traiga los cursos infinitamente
    private Set<Course> enrolledCourses = new HashSet<>();

    // =================================================================
    // 👇 MÉTODOS OBLIGATORIOS DE USER DETAILS (Para quitar el error) 👇
    // =================================================================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null)
            return List.of();
        // Convertimos tu Enum ROLE a lo que Spring entiende
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getUsername() {
        // Spring usa "Username" para el login, pero nosotros usamos el "email"
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // La cuenta nunca caduca
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // La cuenta nunca se bloquea
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // La contraseña no caduca
    }

    @Override
    public boolean isEnabled() {
        return true; // El usuario está activo
    }

    // =================================================================
    // 👆 FIN DE MÉTODOS OBLIGATORIOS 👆
    // =================================================================

    // Tu setter personalizado para limpiar el teléfono
    public void setPhone(String phone) {
        if (phone != null) {
            this.phone = phone.trim().replaceAll("\\s+", "");
        }
    }
}