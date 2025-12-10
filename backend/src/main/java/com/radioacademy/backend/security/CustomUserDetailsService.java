package com.radioacademy.backend.security;

import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. Buscamos el usuario en TU base de datos por email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + email));

        // 2. Lo traducimos al idioma de Spring Security
        // (Spring usa "username", "password" y "authorities")
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword()) // OJO: Spring espera que esto esté cifrado (BCrypt)
                .roles(user.getRole().name()) // Convertimos tu Enum ROLE a String
                .build();
    }
}