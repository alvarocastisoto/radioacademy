package com.radioacademy.backend.controller;

import com.radioacademy.backend.entity.Lesson;
import com.radioacademy.backend.entity.LessonProgress;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.repository.LessonProgressRepository;
import com.radioacademy.backend.repository.LessonRepository;
import com.radioacademy.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    @Autowired
    private LessonProgressRepository progressRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LessonRepository lessonRepository;

    // 1. MARCAR / DESMARCAR LECCIÓN
    @PostMapping("/{lessonId}/toggle")
    public ResponseEntity<?> toggleProgress(@PathVariable UUID lessonId) {
        // Obtenemos usuario del Token
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        LessonProgress progress = progressRepository.findByUserIdAndLessonId(user.getId(), lessonId);

        if (progress == null) {
            // Si no existe, lo creamos (Marcar como visto)
            Lesson lesson = lessonRepository.findById(lessonId)
                    .orElseThrow(() -> new RuntimeException("Lección no existe"));

            progress = LessonProgress.builder()
                    .user(user)
                    .lesson(lesson)
                    .isCompleted(true)
                    .build();
        } else {
            // Si ya existe, invertimos el estado (Visto <-> No Visto)
            progress.setCompleted(!progress.isCompleted());
            progress.setCompletedAt(LocalDateTime.now());
        }

        progressRepository.save(progress);
        return ResponseEntity.ok(progress.isCompleted()); // Devolvemos true/false
    }

    // 2. OBTENER PROGRESO DE UN CURSO (Ids de lecciones vistas)
    // Esto lo llamará Angular al entrar al curso para pintar los checks verdes
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<UUID>> getCourseProgress(@PathVariable UUID courseId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<LessonProgress> progressList = progressRepository.findByUserIdAndCourseId(user.getId(), courseId);

        // Devolvemos solo una lista de IDs de las lecciones que están completadas
        List<UUID> completedLessonIds = progressList.stream()
                .filter(LessonProgress::isCompleted)
                .map(p -> p.getLesson().getId())
                .collect(Collectors.toList());

        return ResponseEntity.ok(completedLessonIds);
    }
}