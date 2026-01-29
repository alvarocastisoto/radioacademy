package com.radioacademy.backend.security;

import com.radioacademy.backend.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class CustomUserDetails implements UserDetails {

    private final UUID id;
    private final String email;
    private final String password;
    private final String nombre;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.nombre = user.getName();
        this.authorities = Collections.singletonList(
                new SimpleGrantedAuthority(user.getRole().name()));
    }

    // Getters personalizados
    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    // Métodos obligatorios de UserDetails
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    // Lógica de estado del usuario (Cambiamos UnsupportedOperation por true)
    @Override
    public boolean isAccountNonExpired() {
        return true; // La cuenta nunca expira
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // La cuenta no se bloquea
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Las credenciales no caducan
    }

    @Override
    public boolean isEnabled() {
        return true; // El usuario está habilitado
    }
}