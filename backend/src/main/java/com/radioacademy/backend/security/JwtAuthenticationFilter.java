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

        // 1. Buscamos el token en la cabecera "Authorization"
        final String authHeader = request.getHeader("Authorization");
        System.out.println("1. Cabecera recibida: " + authHeader);
        final String jwt;
        final String userEmail;

        // Si no hay cabecera o no empieza por "Bearer ", no es para nosotros. Sigue.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extraemos el token (quitamos "Bearer ")
        jwt = authHeader.substring(7);

        // 3. Extraemos el email del token
        userEmail = jwtService.extractUsername(jwt);
        System.out.println("3. Token detectado. Usuario extraído: " + userEmail);
        // 4. Si hay email y el usuario no está ya autenticado en el contexto...
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Cargamos los datos del usuario de la BD
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // 5. Validamos si el token es correcto
            if (jwtService.isTokenValid(jwt, userDetails)) {
                System.out.println("4. ¡Token VÁLIDO! Autenticando usuario...");
                // 6. Creamos la "Ficha de Autenticación" oficial de Spring
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 7. ¡MAGIA! Ponemos al usuario en el Contexto de Seguridad
                // A partir de aquí, Spring sabe quién es y qué roles tiene.
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                System.out.println("4. Token INVÁLIDO o Caducado.");
            }
        }

        // Continuamos con el siguiente filtro
        filterChain.doFilter(request, response);
    }
}