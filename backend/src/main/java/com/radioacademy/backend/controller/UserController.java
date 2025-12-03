package com.radioacademy.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // Esto le dice a Spring: Aquí hay URLS para la api
@RequestMapping("/api/users") // Todas las URLs de este controlador empiezan por /api/users
public class UserController {

    @Autowired // Inyección de dependencias: Spring se encarga de crear el objeto
               // UserRepository
    private UserRepository userRepository;

    // Obtener todos los usuarios (GET /api/users)
    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Crear un nuevo usuario (POST /api/users)
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        // Aquí validamos si el email o el DNI ya existen
        try {
            User savedUser = userRepository.save(user);
            return new ResponseEntity<>(savedUser, HttpStatus.CREATED); // Devuelve el usuario creado con código 201
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR); // Devuelve código 500 en caso de error

        }
    }
}
