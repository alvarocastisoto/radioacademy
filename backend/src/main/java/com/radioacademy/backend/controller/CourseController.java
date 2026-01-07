package com.radioacademy.backend.controller;

import com.radioacademy.backend.dto.CreateCourseRequest;
import com.radioacademy.backend.entity.Course;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.repository.CourseRepository;
import com.radioacademy.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/courses")
@CrossOrigin(origins = "http://localhost:4200")
public class CourseController {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<Course> createCourse(@RequestBody CreateCourseRequest request) {

        // --- 🕵️‍♂️ ZONA DE DEBUG (CHIVATOS) ---
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        System.out.println(">>> INTENTO DE CREAR CURSO");
        System.out.println(">>> USUARIO: " + auth.getName());
        System.out.println(">>> ROLES DETECTADOS: " + auth.getAuthorities());
        // ------------------------------------

        // 1. Buscamos al profesor usando el email del token (Mucho más seguro)
        String userEmail = auth.getName();
        User teacher = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado en BD"));

        // 2. Creamos el curso
        Course newCourse = new Course();
        newCourse.setTitle(request.title());
        newCourse.setDescription(request.description());
        newCourse.setPrice(request.price());
        newCourse.setHours(request.hours());
        // newCourse.setLevel(request.level()); // Descomenta si tu Entidad Course tiene
        // este campo
        newCourse.setActive(true);
        newCourse.setTeacher(teacher); // Asignamos el profesor que hemos encontrado

        Course savedCourse = courseRepository.save(newCourse);
        return new ResponseEntity<>(savedCourse, HttpStatus.CREATED);
    }

    // En CourseController.java

    // ... (Métodos anteriores: getAllCourses y createCourse) ...

    // 1. MÉTODO BORRAR (DELETE)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable String id) {
        // Convertimos el String "abc-123..." a un objeto UUID real
        java.util.UUID uuid = java.util.UUID.fromString(id);

        // Verificamos si existe antes de borrar (opcional, pero buena práctica)
        if (!courseRepository.existsById(uuid)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        courseRepository.deleteById(uuid);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT); // 204: Todo bien, pero no devuelvo nada
    }

    // 2. MÉTODO EDITAR (PUT)
    @PutMapping("/{id}")
    public ResponseEntity<Course> updateCourse(@PathVariable String id, @RequestBody CreateCourseRequest request) {
        java.util.UUID uuid = java.util.UUID.fromString(id);

        // Buscamos el curso existente
        Course existingCourse = courseRepository.findById(uuid)
                .orElseThrow(() -> new RuntimeException("Curso no encontrado"));

        // Actualizamos solo los datos que nos envían
        existingCourse.setTitle(request.title());
        existingCourse.setDescription(request.description());
        existingCourse.setPrice(request.price());
        existingCourse.setHours(request.hours());
        // existingCourse.setLevel(request.level()); // Descomenta si usas level

        // Guardamos los cambios
        Course updatedCourse = courseRepository.save(existingCourse);

        return new ResponseEntity<>(updatedCourse, HttpStatus.OK);
    }

}