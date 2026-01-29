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

        // 1. Si no hay token, pasa la pelota (Correcto)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Intentamos procesar el token de forma SEGURA
        try {
            jwt = authHeader.substring(7);
            userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails dbUser = this.userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, dbUser)) {

                    // 💡 Usa directamente dbUser, que ya es tu CustomUserDetails
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            dbUser, // <--- PASAMOS EL OBJETO COMPLETO CON ID Y NOMBRE
                            null,
                            dbUser.getAuthorities());

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // 🔥 AQUÍ ESTÁ EL ARREGLO 🔥
            // Si el token está caducado o mal formado, NO lanzamos error.
            // Simplemente no autenticamos y dejamos que la petición siga.
            // Si va a /register (público), pasará. Si va a /admin (privado), rebotará
            // después.
            System.out.println("⚠️ Token inválido ignorado: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}