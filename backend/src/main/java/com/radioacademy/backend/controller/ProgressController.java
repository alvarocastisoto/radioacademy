package com.radioacademy.backend.controller;

import com.radioacademy.backend.dto.course.CourseProgressResponse;
import com.radioacademy.backend.dto.lesson.ToggleProgressResponse;
import com.radioacademy.backend.security.CustomUserDetails;
import com.radioacademy.backend.service.content.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor 
public class ProgressController {

    private final ProgressService progressService;

    
    @PostMapping("/{lessonId}/toggle")
    public ResponseEntity<ToggleProgressResponse> toggleProgress(
            @PathVariable UUID lessonId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(progressService.toggleProgress(lessonId, userDetails));
    }

    
    @GetMapping("/course/{courseId}")
    public ResponseEntity<CourseProgressResponse> getCourseProgress(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(progressService.getCourseProgress(courseId, userDetails));
    }
}