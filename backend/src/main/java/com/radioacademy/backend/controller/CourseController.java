package com.radioacademy.backend.controller;

import com.radioacademy.backend.dto.CourseDTO;
import com.radioacademy.backend.dto.CourseDetailDTO;
import com.radioacademy.backend.dto.CreateCourseRequest;
import com.radioacademy.backend.dto.CourseDashboardDTO; // 👈 Usamos el DTO que creaste antes
import com.radioacademy.backend.entity.Course;
import com.radioacademy.backend.entity.Enrollment;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.repository.CourseRepository;
import com.radioacademy.backend.repository.EnrollmentRepository;
import com.radioacademy.backend.repository.UserRepository;
import com.radioacademy.backend.service.StorageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private StorageService storageService;

    // 1. OBTENER CURSOS (CATÁLOGO PÚBLICO)
    @GetMapping
    public ResponseEntity<List<CourseDTO>> getAllCourses(Authentication authentication) {
        List<Course> courses = courseRepository.findAll();
        Set<UUID> purchasedCourseIds = new HashSet<>();

        if (authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser")) {

            userRepository.findByEmail(authentication.getName()).ifPresent(user -> {
                enrollmentRepository.findByUserId(user.getId())
                        .forEach(enrollment -> purchasedCourseIds.add(enrollment.getCourse().getId()));
            });
        }

        List<CourseDTO> dtos = courses.stream().map(course -> new CourseDTO(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getCoverImage(),
                course.getPrice(),
                course.getHours(),
                purchasedCourseIds.contains(course.getId()))).toList();

        return ResponseEntity.ok(dtos);
    }

    // 2. CREAR CURSO (POST) - ADMIN
    @PostMapping
    public ResponseEntity<CourseDetailDTO> createCourse(@Valid @RequestBody CreateCourseRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User teacher = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Course newCourse = new Course();
        newCourse.setTitle(request.title());
        newCourse.setDescription(request.description());
        newCourse.setPrice(request.price());
        newCourse.setHours(request.hours());
        newCourse.setActive(true);
        newCourse.setTeacher(teacher);
        newCourse.setCoverImage(request.coverImage());

        Course savedCourse = courseRepository.save(newCourse);

        // 🔄 Devolvemos DTO
        return new ResponseEntity<>(mapToDetailDTO(savedCourse), HttpStatus.CREATED);
    }

    // 3. ACTUALIZAR CURSO (PUT) - ADMIN
    @PutMapping("/{id}")
    public ResponseEntity<CourseDetailDTO> updateCourse(@PathVariable UUID id,
            @Valid @RequestBody CreateCourseRequest request) {

        Course existingCourse = courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado"));

        // Lógica de borrado de imagen antigua (Garbage Collection)
        if (request.coverImage() != null && !request.coverImage().equals(existingCourse.getCoverImage())) {
            String oldImage = existingCourse.getCoverImage();
            if (oldImage != null && !oldImage.isEmpty()) {
                try {
                    String filename = oldImage.substring(oldImage.lastIndexOf("/") + 1);
                    storageService.delete(filename);
                } catch (Exception e) {
                    System.err.println("⚠️ No se pudo borrar la imagen antigua: " + e.getMessage());
                }
            }
            existingCourse.setCoverImage(request.coverImage());
        }

        existingCourse.setTitle(request.title());
        existingCourse.setDescription(request.description());
        existingCourse.setPrice(request.price());
        existingCourse.setHours(request.hours());

        Course updatedCourse = courseRepository.save(existingCourse);

        // 🔄 Devolvemos DTO
        return ResponseEntity.ok(mapToDetailDTO(updatedCourse));
    }

    // 4. BORRAR CURSO (DELETE) - ADMIN
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable UUID id) {

        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado"));

        // Borrar imagen asociada
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
        return ResponseEntity.noContent().build();
    }

    // 5. OBTENER MIS CURSOS (DASHBOARD ALUMNO)
    // Usamos el DTO CourseDashboardDTO que ya tenías
    @GetMapping("/mine")
    public ResponseEntity<List<CourseDashboardDTO>> getMyCourses(Authentication authentication) {

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        List<Enrollment> enrollments = enrollmentRepository.findByUserId(user.getId());

        List<CourseDashboardDTO> response = enrollments.stream().map(enrollment -> {
            Course course = enrollment.getCourse();

            // Aquí deberías calcular el progreso real si lo tienes implementado
            // int progress = progressRepository.count... (como hicimos en el
            // DashboardController)
            int progress = 0;

            return new CourseDashboardDTO(
                    course.getId(),
                    course.getTitle(),
                    course.getDescription(),
                    course.getCoverImage(),
                    progress);
        }).toList();

        return ResponseEntity.ok(response);
    }

    // --- Helper para mapear ---
    private CourseDetailDTO mapToDetailDTO(Course course) {
        return new CourseDetailDTO(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getCoverImage(),
                course.getPrice(),
                course.getHours(),
                course.getActive());
    }
}