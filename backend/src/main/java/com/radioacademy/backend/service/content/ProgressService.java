package com.radioacademy.backend.service.content;

import com.radioacademy.backend.dto.course.CourseProgressResponse;
import com.radioacademy.backend.dto.lesson.ToggleProgressResponse;
import com.radioacademy.backend.entity.Lesson;
import com.radioacademy.backend.entity.LessonProgress;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.repository.LessonProgressRepository;
import com.radioacademy.backend.repository.LessonRepository;
import com.radioacademy.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final LessonProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;

    // 1. MARCAR / DESMARCAR (TOGGLE)
    @Transactional
    public ToggleProgressResponse toggleProgress(UUID lessonId, String userEmail) {
        // Obtenemos usuario
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        // Buscamos si ya existe progreso
        LessonProgress progress = progressRepository.findByUserIdAndLessonId(user.getId(), lessonId)
                .orElse(null);

        boolean isCompletedNow;

        if (progress == null) {
            // CREAR: Primera vez
            Lesson lesson = lessonRepository.findById(lessonId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lección no encontrada"));

            // Usamos Builder si tienes @Builder en la entidad, si no usa new
            // LessonProgress()
            progress = LessonProgress.builder()
                    .user(user)
                    .lesson(lesson)
                    .isCompleted(true)
                    .build();
            isCompletedNow = true;
        } else {
            // ACTUALIZAR: Invertir estado
            isCompletedNow = !progress.isCompleted();
            progress.setCompleted(isCompletedNow);
        }

        progressRepository.save(progress);

        String msg = isCompletedNow ? "Lección completada" : "Lección pendiente";
        return new ToggleProgressResponse(lessonId, isCompletedNow, msg);
    }

    // 2. OBTENER PROGRESO CURSO
    @Transactional(readOnly = true)
    public CourseProgressResponse getCourseProgress(UUID courseId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        // Query optimizada que devuelve solo IDs
        Set<UUID> completedIds = progressRepository.findCompletedLessonIdsByCourse(user.getId(), courseId);

        return new CourseProgressResponse(
                courseId,
                completedIds.size(),
                completedIds);
    }
}