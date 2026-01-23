package com.radioacademy.backend.service.content;

import com.radioacademy.backend.dto.course.CourseProgressResponse;
import com.radioacademy.backend.dto.lesson.ToggleProgressResponse;
import com.radioacademy.backend.entity.Lesson;
import com.radioacademy.backend.entity.LessonProgress;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.repository.EnrollmentRepository;
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
        private final EnrollmentRepository enrollmentRepository;

        // 1. MARCAR / DESMARCAR (TOGGLE)
        @Transactional
        public ToggleProgressResponse toggleProgress(UUID lessonId, String userEmail) {

                // 1. Obtener Usuario
                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Usuario no encontrado"));

                // 2. Obtener Lección (Necesario para saber el curso)
                Lesson lesson = lessonRepository.findById(lessonId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Lección no encontrada"));

                // 3. SEGURIDAD: Validar matrícula
                // Navegamos Lesson -> Module -> Course -> ID
                UUID courseId = lesson.getModule().getCourse().getId();

                boolean enrolled = enrollmentRepository.existsByUserIdAndCourseId(user.getId(), courseId);
                if (!enrolled) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                        "No tienes acceso a este curso (no matriculado).");
                }

                // 4. Lógica Toggle (Estilo limpio con orElseGet)
                LessonProgress progress = progressRepository.findByUserIdAndLessonId(user.getId(), lessonId)
                                .orElseGet(() -> {
                                        // Si no existe, creamos uno nuevo inicializado en false (para invertirlo luego)
                                        LessonProgress newP = new LessonProgress();
                                        newP.setUser(user);
                                        newP.setLesson(lesson);
                                        newP.setCompleted(false);
                                        return newP;
                                });

                // 5. Invertir estado y Guardar
                boolean newState = !progress.isCompleted();
                progress.setCompleted(newState);

                progressRepository.save(progress);

                // 6. Respuesta
                String msg = newState ? "Lección completada" : "Lección pendiente";
                return new ToggleProgressResponse(lessonId, newState, msg);
        }

        // 2. OBTENER PROGRESO CURSO
        @Transactional(readOnly = true)
        public CourseProgressResponse getCourseProgress(UUID courseId, String userEmail) {
                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Usuario no encontrado"));

                // Query optimizada que devuelve solo IDs
                Set<UUID> completedIds = progressRepository.findCompletedLessonIdsByCourse(user.getId(), courseId);

                return new CourseProgressResponse(
                                courseId,
                                completedIds.size(),
                                completedIds);
        }
}