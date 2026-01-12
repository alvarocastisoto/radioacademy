package com.radioacademy.backend.controller;

import com.radioacademy.backend.dto.CreateCourseRequest;
import com.radioacademy.backend.entity.Course;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.repository.CourseRepository;
import com.radioacademy.backend.repository.UserRepository;
import com.radioacademy.backend.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses")
@CrossOrigin(origins = "http://localhost:4200")
public class CourseController {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StorageService storageService;

    @GetMapping
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    // ==========================================
    // 1. CREAR CURSO (POST)
    // ==========================================
    @PostMapping
    public ResponseEntity<Course> createCourse(@RequestBody CreateCourseRequest request) {

        // 🕵️‍♂️ DEBUG: ¿Llega la ?
        System.out.println(">>> CREANDO CURSO: " + request.title());
        System.out.println(">>> IMAGEN RECIBIDA: " + request.coverImage());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User teacher = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Course newCourse = new Course();
        newCourse.setTitle(request.title());
        newCourse.setDescription(request.description());
        newCourse.setPrice(request.price());
        newCourse.setHours(request.hours());
        newCourse.setActive(true);
        newCourse.setTeacher(teacher);

        // 👇 ASIGNACIÓN DIRECTA
        newCourse.setCoverImage(request.coverImage());

        Course savedCourse = courseRepository.save(newCourse);
        return new ResponseEntity<>(savedCourse, HttpStatus.CREATED);
    }

    // ==========================================
    // 2. ACTUALIZAR CURSO (PUT)
    // ==========================================
    @PutMapping("/{id}")
    public ResponseEntity<Course> updateCourse(@PathVariable String id, @RequestBody CreateCourseRequest request) {
        UUID uuid = UUID.fromString(id);
        Course existingCourse = courseRepository.findById(uuid)
                .orElseThrow(() -> new RuntimeException("Curso no encontrado"));

        // 🕵️‍♂️ DEBUG
        System.out.println(">>> ACTUALIZANDO CURSO: " + existingCourse.getTitle());
        System.out.println(">>> IMAGEN ANTIGUA: " + existingCourse.getCoverImage());
        System.out.println(">>> IMAGEN NUEVA: " + request.coverImage());

        // --- LÓGICA DE LIMPIEZA ROBUSTA ---
        String newImage = request.coverImage();
        String oldImage = existingCourse.getCoverImage();

        // Si la imagen ha cambiado (es distinta a la que había)
        if (!Objects.equals(newImage, oldImage)) {

            // 1. Si había una imagen vieja, bórrala del disco
            if (oldImage != null && !oldImage.isEmpty()) {
                try {
                    String filename = oldImage.substring(oldImage.lastIndexOf("/") + 1);
                    storageService.delete(filename);
                } catch (Exception e) {
                    System.err.println("⚠️ No se pudo borrar la imagen antigua: " + e.getMessage());
                }
            }

            // 2. Asigna la nueva (aunque sea null, así permitimos borrarla)
            existingCourse.setCoverImage(newImage);
        }
        // -------------------------------------

        existingCourse.setTitle(request.title());
        existingCourse.setDescription(request.description());
        existingCourse.setPrice(request.price());
        existingCourse.setHours(request.hours());

        Course updatedCourse = courseRepository.save(existingCourse);
        return new ResponseEntity<>(updatedCourse, HttpStatus.OK);
    }

    // ==========================================
    // 3. BORRAR CURSO (DELETE)
    // ==========================================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable String id) {
        UUID uuid = UUID.fromString(id);

        return courseRepository.findById(uuid).map(course -> {
            String imageUrl = course.getCoverImage();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                try {
                    String filename = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
                    storageService.delete(filename);
                } catch (Exception e) {
                    System.err.println("⚠️ Error borrando imagen: " + e.getMessage());
                }
            }
            courseRepository.delete(course);
            return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
        }).orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}