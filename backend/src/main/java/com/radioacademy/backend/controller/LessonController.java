package com.radioacademy.backend.controller;

import com.radioacademy.backend.dto.lesson.LessonResponse;
import com.radioacademy.backend.service.content.LessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor // Inyección automática
public class LessonController {

    private final LessonService lessonService;

    // 1. GET: Lecciones por módulo
    @GetMapping("/module/{moduleId}")
    public ResponseEntity<List<LessonResponse>> getLessonsByModule(@PathVariable UUID moduleId) {
        return ResponseEntity.ok(lessonService.getLessonsByModule(moduleId));
    }

    // 2. GET: Una lección
    @GetMapping("/{id}")
    public ResponseEntity<LessonResponse> getLessonById(@PathVariable UUID id) {
        return ResponseEntity.ok(lessonService.getLessonById(id));
    }

    // 3. POST: Crear lección (Recibe parámetros sueltos por ser Multipart)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LessonResponse> createLesson(
            @RequestParam("title") String title,
            @RequestParam(value = "videoUrl", required = false) String videoUrl,
            @RequestParam("moduleId") UUID moduleId,
            @RequestParam("orderIndex") Integer orderIndex,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        return new ResponseEntity<>(
                lessonService.createLesson(title, videoUrl, moduleId, orderIndex, file),
                HttpStatus.CREATED);
    }

    // 4. PUT: Actualizar lección
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LessonResponse> updateLesson(
            @PathVariable UUID id,
            @RequestParam("title") String title,
            @RequestParam(value = "videoUrl", required = false) String videoUrl,
            @RequestParam("orderIndex") Integer orderIndex,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        return ResponseEntity.ok(lessonService.updateLesson(id, title, videoUrl, orderIndex, file));
    }

    // 5. DELETE: Borrar lección
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLesson(@PathVariable UUID id) {
        lessonService.deleteLesson(id);
        return ResponseEntity.noContent().build();
    }
}