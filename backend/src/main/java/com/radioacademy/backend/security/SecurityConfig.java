package com.radioacademy.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Value;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomUserDetailsService customUserDetailsService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService, JwtAuthenticationFilter jwtAuthFilter) {
        this.customUserDetailsService = customUserDetailsService;
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. RUTAS PÚBLICAS Y DE SISTEMA
                        .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                        .requestMatchers("/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                        .requestMatchers("/error").permitAll() // 👈 IMPORTANTE: Para ver los mensajes de error reales

                        // 2. STATIC RESOURCES (Archivos)
                        .requestMatchers(HttpMethod.GET, "/uploads/images/**").permitAll() // Imágenes públicas
                        .requestMatchers("/uploads/pdfs/**").denyAll() // 🔒 PDFs físicos BLOQUEADOS (nadie entra por
                                                                       // aquí)

                        // 3. CATÁLOGO PÚBLICO
                        .requestMatchers(HttpMethod.GET, "/api/courses/**").permitAll()

                        // 4. ACCESO A CONTENIDO PROTEGIDO (PDFs mediante Controlador)
                        .requestMatchers("/api/student/lessons/**") // 👈 Tu nuevo controlador
                        .hasAnyAuthority("STUDENT", "ROLE_STUDENT", "ADMIN", "ROLE_ADMIN")

                        // 5. SUBIDA DE ARCHIVOS (MEDIA)
                        .requestMatchers("/api/media/**").authenticated()

                        // 6. PAGOS
                        .requestMatchers("/api/payment/**")
                        .hasAnyAuthority("STUDENT", "ROLE_STUDENT", "ADMIN", "ROLE_ADMIN")

                        // 7. ZONA ADMIN
                        .requestMatchers("/api/admin/**").hasAnyAuthority("ADMIN", "ROLE_ADMIN")

                        // Gestión de cursos (Admin)
                        .requestMatchers(HttpMethod.POST, "/api/courses/**", "/api/modules/**", "/api/lessons/**")
                        .hasAnyAuthority("ADMIN", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/courses/**", "/api/modules/**", "/api/lessons/**")
                        .hasAnyAuthority("ADMIN", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/courses/**", "/api/modules/**", "/api/lessons/**")
                        .hasAnyAuthority("ADMIN", "ROLE_ADMIN")
                        .requestMatchers("/api/quizzes/**")
                        .hasAnyAuthority("ADMIN", "ROLE_ADMIN")

                        // 8. RESTO
                        .anyRequest().authenticated())

                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(frontendUrl));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // ⚠️ CAMBIO RECOMENDADO PARA ARCHIVOS:
        // A veces la subida de archivos (Multipart) envía cabeceras extra o boundaries.
        // Poner "*" en headers es más seguro para evitar errores de CORS raros al subir
        // fotos.
        configuration.setAllowedHeaders(List.of("*"));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
}