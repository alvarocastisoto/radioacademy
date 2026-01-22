package com.radioacademy.backend.controller;

import com.radioacademy.backend.dto.course.CourseProgressResponse;
import com.radioacademy.backend.dto.lesson.ToggleProgressResponse;
import com.radioacademy.backend.entity.Lesson;
import com.radioacademy.backend.entity.LessonProgress;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.repository.LessonProgressRepository;
import com.radioacademy.backend.repository.LessonRepository;
import com.radioacademy.backend.repository.UserRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor; // 👈 Lombok te ahorra los @Autowired
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor // Inyección por constructor automática
public class ProgressController {

    private final LessonProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;

    // 1. MARCAR / DESMARCAR LECCIÓN
    @PostMapping("/{lessonId}/toggle")
    public ResponseEntity<ToggleProgressResponse> toggleProgress(@Valid @PathVariable UUID lessonId) {

        // Obtener usuario (Extraído a método helper para no repetir código)
        User user = getAuthenticatedUser();

        // Buscamos si ya existe el registro de progreso
        LessonProgress progress = progressRepository.findByUserIdAndLessonId(user.getId(), lessonId)
                .orElse(null);

        boolean isCompletedNow;

        if (progress == null) {
            // CREAR (Primera vez que la ve)
            Lesson lesson = lessonRepository.findById(lessonId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lección no encontrada"));

            progress = LessonProgress.builder()
                    .user(user)
                    .lesson(lesson)
                    .isCompleted(true) // Por defecto true al crear
                    .build();
            isCompletedNow = true;
        } else {
            // ACTUALIZAR (Toggle)
            isCompletedNow = !progress.isCompleted(); // Invertimos
            progress.setCompleted(isCompletedNow);
        }

        progressRepository.save(progress);

        // Devolvemos DTO limpio
        String msg = isCompletedNow ? "Lección marcada como vista" : "Lección marcada como no vista";
        return ResponseEntity.ok(new ToggleProgressResponse(lessonId, isCompletedNow, msg));
    }

    // 2. OBTENER PROGRESO DE UN CURSO
    @GetMapping("/course/{courseId}")
    public ResponseEntity<CourseProgressResponse> getCourseProgress(@Valid @PathVariable UUID courseId) {

        User user = getAuthenticatedUser();

        // Usamos la Query optimizada que devuelve solo los IDs (Set<UUID>)
        // Esto es mucho más rápido que traerse las entidades enteras
        Set<UUID> completedIds = progressRepository.findCompletedLessonIdsByCourse(user.getId(), courseId);

        return ResponseEntity.ok(new CourseProgressResponse(
                courseId,
                completedIds.size(),
                completedIds));
    }

    // --- Helper Privado ---
    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }
}