package com.radioacademy.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        userEmail = jwtService.extractUsername(jwt);

        // ... dentro de doFilterInternal ...

        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 1. Cargamos el usuario "sucio" (conectado a BD)
            UserDetails dbUser = this.userDetailsService.loadUserByUsername(userEmail);

            if (jwtService.isTokenValid(jwt, dbUser)) {

                // 👇👇👇 EL CAMBIO MAGICO EMPIEZA AQUI 👇👇👇
                // Creamos un usuario "limpio" que es una copia tonta, sin conexión a BD.
                // Esto evita que Spring intente leer la base de datos cuando ya se ha cerrado.

                UserDetails safeUser = org.springframework.security.core.userdetails.User.builder()
                        .username(dbUser.getUsername())
                        .password(dbUser.getPassword())
                        .authorities(dbUser.getAuthorities()) // Copiamos los roles
                        .accountExpired(!dbUser.isAccountNonExpired())
                        .accountLocked(!dbUser.isAccountNonLocked())
                        .credentialsExpired(!dbUser.isCredentialsNonExpired())
                        .disabled(!dbUser.isEnabled())
                        .build();
                // 👆👆👆 EL CAMBIO MAGICO TERMINA AQUI 👆👆👆

                // AHORA USAMOS 'safeUser' EN LUGAR DE 'dbUser'
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        safeUser, // <--- Aquí va el usuario seguro
                        null,
                        safeUser.getAuthorities());

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        // ... resto del código igual ...

        filterChain.doFilter(request, response);
    }
}