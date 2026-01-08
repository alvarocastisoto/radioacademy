package com.radioacademy.backend.controller;

import com.radioacademy.backend.dto.CreateCourseRequest;
import com.radioacademy.backend.entity.Course;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.repository.CourseRepository;
import com.radioacademy.backend.repository.UserRepository;
import com.radioacademy.backend.service.StorageService; // 👈 IMPRESCINDIBLE

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;

@RestController
@RequestMapping("/api/courses")
@CrossOrigin(origins = "http://localhost:4200")
public class CourseController {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    // 👇 Inyectamos el servicio de archivos para poder borrar
    @Autowired
    private StorageService storageService;

    @GetMapping
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<Course> createCourse(@RequestBody CreateCourseRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // 1. Buscamos al profesor
        String userEmail = auth.getName();
        User teacher = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado en BD"));

        // 2. Creamos el curso
        Course newCourse = new Course();
        newCourse.setTitle(request.title());
        newCourse.setDescription(request.description());
        newCourse.setPrice(request.price());
        newCourse.setHours(request.hours());
        // 👇 Guardamos la imagen si viene en el request
        newCourse.setCoverImage(request.coverImage());
        newCourse.setActive(true);
        newCourse.setTeacher(teacher);

        Course savedCourse = courseRepository.save(newCourse);
        return new ResponseEntity<>(savedCourse, HttpStatus.CREATED);
    }

    // 1. MÉTODO BORRAR (DELETE) - CON LIMPIEZA DE IMAGEN
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable String id) {
        java.util.UUID uuid = java.util.UUID.fromString(id);

        // Recuperamos el curso para ver si tiene imagen antes de borrarlo
        return courseRepository.findById(uuid).map(course -> {

            // 🗑️ Si tiene portada, borramos el archivo físico
            String imageUrl = course.getCoverImage();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                try {
                    String filename = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
                    storageService.delete(filename);
                } catch (Exception e) {
                    System.err.println("⚠️ Error borrando imagen al eliminar curso: " + e.getMessage());
                }
            }

            courseRepository.delete(course);
            return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
        }).orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // 2. MÉTODO EDITAR (PUT) - CON GARBAGE COLLECTION
    @PutMapping("/{id}")
    public ResponseEntity<Course> updateCourse(@PathVariable String id, @RequestBody CreateCourseRequest request) {
        java.util.UUID uuid = java.util.UUID.fromString(id);

        Course existingCourse = courseRepository.findById(uuid)
                .orElseThrow(() -> new RuntimeException("Curso no encontrado"));

        // --- LÓGICA DE LIMPIEZA DE IMAGEN ---
        // Si nos envían una nueva imagen y es distinta a la que había...
        if (request.coverImage() != null && !request.coverImage().isEmpty()
                && !request.coverImage().equals(existingCourse.getCoverImage())) {

            // Borramos la vieja del disco
            String oldUrl = existingCourse.getCoverImage();
            if (oldUrl != null && !oldUrl.isEmpty()) {
                try {
                    String filename = oldUrl.substring(oldUrl.lastIndexOf("/") + 1);
                    storageService.delete(filename);
                } catch (Exception e) {
                    System.err.println("⚠️ Error borrando imagen antigua del curso");
                }
            }
            // Asignamos la nueva
            existingCourse.setCoverImage(request.coverImage());
        }
        // -------------------------------------

        existingCourse.setTitle(request.title());
        existingCourse.setDescription(request.description());
        existingCourse.setPrice(request.price());
        existingCourse.setHours(request.hours());

        Course updatedCourse = courseRepository.save(existingCourse);
        return new ResponseEntity<>(updatedCourse, HttpStatus.OK);
    }
}