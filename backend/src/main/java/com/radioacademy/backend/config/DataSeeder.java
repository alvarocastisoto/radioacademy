package com.radioacademy.backend.config;

import com.radioacademy.backend.repository.UserRepository;

import java.time.LocalDateTime;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.enums.Role;

import org.flywaydb.core.FlywayExecutor.Command;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Inyectamos los valores del properties
    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Verificamos si el usuario admin ya existe
        if (!userRepository.existsByEmail(adminEmail)) {
            // 2. Si NO existe, lo creamos
            User admin = new User();
            admin.setName("Super");
            admin.setSurname("Admin");
            admin.setEmail(adminEmail);
            admin.setDni("00000000X"); // DNI dummy para admin
            admin.setRegion("CyberSpace");
            admin.setPhone("000000000");
            admin.setTermsAccepted(true);
            admin.setCreatedAt(LocalDateTime.now());

            admin.setRole(Role.ADMIN);
            // Encriptamos la contraseña antes de guardarla
            admin.setPassword(passwordEncoder.encode(adminPassword));
            userRepository.save(admin);
            System.out.println("Admin user created with email: " + adminEmail);
        } else {
            System.out.println("Admin user already exists with email: " + adminEmail);
        }

    }

}
