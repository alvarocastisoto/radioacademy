package com.radioacademy.backend.controller;

import com.radioacademy.backend.dto.course.CourseDTO;
import com.radioacademy.backend.dto.course.CourseDashboardDTO;
import com.radioacademy.backend.dto.course.CourseDetailDTO;
import com.radioacademy.backend.dto.course.CreateCourseRequest;
import com.radioacademy.backend.service.content.CourseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor // 👈 Lombok hace la magia del constructor
public class CourseController {

    private final CourseService courseService;

    // 1. OBTENER CURSOS (CATÁLOGO PÚBLICO)
    @GetMapping
    public ResponseEntity<List<CourseDTO>> getAllCourses(Authentication authentication) {
        return ResponseEntity.ok(courseService.getAllCourses(authentication));
    }

    // 2. CREAR CURSO (POST) - ADMIN
    @PostMapping
    public ResponseEntity<CourseDetailDTO> createCourse(
            @Valid @RequestBody CreateCourseRequest request,
            Authentication authentication) {

        return new ResponseEntity<>(
                courseService.createCourse(request, authentication.getName()),
                HttpStatus.CREATED);
    }

    // 3. ACTUALIZAR CURSO (PUT) - ADMIN
    @PutMapping("/{id}")
    public ResponseEntity<CourseDetailDTO> updateCourse(
            @PathVariable UUID id,
            @Valid @RequestBody CreateCourseRequest request) {

        return ResponseEntity.ok(courseService.updateCourse(id, request));
    }

    // 4. BORRAR CURSO (DELETE) - ADMIN
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable UUID id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }

    // 5. OBTENER MIS CURSOS (DASHBOARD ALUMNO)
    @GetMapping("/mine")
    public ResponseEntity<List<CourseDashboardDTO>> getMyCourses(Authentication authentication) {
        return ResponseEntity.ok(courseService.getMyCourses(authentication.getName()));
    }
}