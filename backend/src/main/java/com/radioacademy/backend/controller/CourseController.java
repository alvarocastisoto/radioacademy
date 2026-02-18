package com.radioacademy.backend.controller;

import com.radioacademy.backend.dto.course.CourseDTO;
import com.radioacademy.backend.dto.course.CourseDashboardDTO;
import com.radioacademy.backend.dto.course.CourseDetailDTO;
import com.radioacademy.backend.dto.course.CreateCourseRequest;
import com.radioacademy.backend.security.CustomUserDetails;
import com.radioacademy.backend.service.content.CourseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor 
public class CourseController {

    private final CourseService courseService;

    
    @GetMapping
    public ResponseEntity<List<CourseDTO>> getAllCourses(Authentication authentication) {
        return ResponseEntity.ok(courseService.getAllCourses(authentication));
    }

    
    @PostMapping
    public ResponseEntity<CourseDetailDTO> createCourse(
            @Valid @RequestBody CreateCourseRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return new ResponseEntity<>(
                courseService.createCourse(request, userDetails),
                HttpStatus.CREATED);
    }

    
    @PutMapping("/{id}")
    public ResponseEntity<CourseDetailDTO> updateCourse(
            @PathVariable UUID id,
            @Valid @RequestBody CreateCourseRequest request) {

        return ResponseEntity.ok(courseService.updateCourse(id, request));
    }

    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable UUID id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }

    
    @GetMapping("/mine")
    public ResponseEntity<List<CourseDashboardDTO>> getMyCourses(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(courseService.getMyCourses(userDetails));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseDetailDTO> getCourseById(@PathVariable UUID id) {
        return ResponseEntity.ok(courseService.getCourseById(id));
    }
}