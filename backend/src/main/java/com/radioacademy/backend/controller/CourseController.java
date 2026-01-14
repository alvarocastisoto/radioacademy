package com.radioacademy.backend.controller;

import com.radioacademy.backend.dto.CourseDTO;
import com.radioacademy.backend.dto.CreateCourseRequest;
import com.radioacademy.backend.entity.Course;
import com.radioacademy.backend.entity.Enrollment;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.repository.CourseRepository;
import com.radioacademy.backend.repository.EnrollmentRepository;
import com.radioacademy.backend.repository.UserRepository;
import com.radioacademy.backend.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/courses")
@CrossOrigin(origins = "http://localhost:4200")
public class CourseController {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private StorageService storageService;

    // ==========================================
    // 1. OBTENER CURSOS (CATÁLOGO PÚBLICO)
    // ==========================================
    @GetMapping
    public ResponseEntity<List<CourseDTO>> getAllCourses(Authentication authentication) {

        // 1. Traemos todos los cursos
        List<Course> courses = courseRepository.findAll();

        // 2. Preparamos el conjunto de IDs comprados
        Set<UUID> purchasedCourseIds;

        if (authentication != null && authentication.isAuthenticated() &&
                !authentication.getPrincipal().equals("anonymousUser")) {

            String email = authentication.getName();
            User user = userRepository.findByEmail(email).orElse(null);

            if (user != null) {
                // ✅ CORRECCIÓN: Usamos el método del repositorio.
                // Esto delega la búsqueda a SQL, evitando errores de comparación en Java.
                List<Enrollment> userEnrollments = enrollmentRepository.findByUserId(user.getId());

                purchasedCourseIds = userEnrollments.stream()
                        .map(enrollment -> enrollment.getCourse().getId())
                        .collect(Collectors.toSet());
            } else {
                purchasedCourseIds = Collections.emptySet();
            }
        } else {
            purchasedCourseIds = Collections.emptySet();
        }

        // 3. Convertimos a DTO (igual que antes)
        List<CourseDTO> dtos = courses.stream().map(course -> {
            // Al usar un Set y UUIDs, el .contains() funciona perfecto
            boolean isPurchased = purchasedCourseIds.contains(course.getId());

            return new CourseDTO(
                    course.getId(),
                    course.getTitle(),
                    course.getDescription(),
                    course.getCoverImage(),
                    course.getPrice(),
                    course.getHours(),
                    isPurchased);
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // ==========================================
    // 2. CREAR CURSO (POST) - ADMIN
    // ==========================================
    @PostMapping
    public ResponseEntity<Course> createCourse(@RequestBody CreateCourseRequest request) {

        System.out.println(">>> CREANDO CURSO: " + request.title());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User teacher = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Course newCourse = new Course();
        // Aquí SÍ usamos setters porque 'Course' es una Entidad, no un Record.
        newCourse.setTitle(request.title());
        newCourse.setDescription(request.description());
        newCourse.setPrice(request.price());
        newCourse.setHours(request.hours());
        newCourse.setActive(true);
        newCourse.setTeacher(teacher);
        newCourse.setCoverImage(request.coverImage());

        Course savedCourse = courseRepository.save(newCourse);
        return new ResponseEntity<>(savedCourse, HttpStatus.CREATED);
    }

    // ==========================================
    // 3. ACTUALIZAR CURSO (PUT) - ADMIN
    // ==========================================
    @PutMapping("/{id}")
    public ResponseEntity<Course> updateCourse(@PathVariable String id, @RequestBody CreateCourseRequest request) {
        UUID uuid = UUID.fromString(id);
        Course existingCourse = courseRepository.findById(uuid)
                .orElseThrow(() -> new RuntimeException("Curso no encontrado"));

        String newImage = request.coverImage();
        String oldImage = existingCourse.getCoverImage();

        if (!Objects.equals(newImage, oldImage)) {
            if (oldImage != null && !oldImage.isEmpty()) {
                try {
                    String filename = oldImage.substring(oldImage.lastIndexOf("/") + 1);
                    storageService.delete(filename);
                } catch (Exception e) {
                    System.err.println("⚠️ No se pudo borrar la imagen antigua: " + e.getMessage());
                }
            }
            existingCourse.setCoverImage(newImage);
        }

        existingCourse.setTitle(request.title());
        existingCourse.setDescription(request.description());
        existingCourse.setPrice(request.price());
        existingCourse.setHours(request.hours());

        Course updatedCourse = courseRepository.save(existingCourse);
        return new ResponseEntity<>(updatedCourse, HttpStatus.OK);
    }

    // ==========================================
    // 4. BORRAR CURSO (DELETE) - ADMIN
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

    // 5. OBTENER MIS CURSOS (DASHBOARD ALUMNO)
    // ==========================================
    @GetMapping("/mine")
    public ResponseEntity<List<Map<String, Object>>> getMyCourses(Authentication authentication) {
        // 1. Obtener usuario
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 2. Buscar matrículas DIRECTAMENTE en el repositorio de enrollments
        List<Enrollment> enrollments = enrollmentRepository.findByUserId(user.getId());

        // 3. Mapear a lo que espera tu StudentDashboard (DashboardCourse)
        List<Map<String, Object>> response = enrollments.stream().map(enrollment -> {
            Course course = enrollment.getCourse();

            // Construimos un mapa simple (o podrías crear un DTO DashboardCourseDTO)
            Map<String, Object> map = new HashMap<>();
            map.put("id", course.getId());
            map.put("title", course.getTitle());
            map.put("description", course.getDescription());
            map.put("coverImage", course.getCoverImage());
            map.put("pdfUrl", null); // O course.getPdfUrl() si lo tienes
            map.put("progress", 0); // O enrollment.getProgress() si lo tienes implementado

            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}