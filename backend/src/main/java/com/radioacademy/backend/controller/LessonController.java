package com.radioacademy.backend.controller;

import com.radioacademy.backend.dto.LessonResponse;
import com.radioacademy.backend.entity.Lesson;
import com.radioacademy.backend.entity.Module;
import com.radioacademy.backend.repository.LessonRepository;
import com.radioacademy.backend.repository.ModuleRepository;
import com.radioacademy.backend.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/lessons")
public class LessonController {

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private StorageService storageService;

    // 1. GET: Lecciones por módulo (DTOs)
    @GetMapping("/module/{moduleId}")
    public ResponseEntity<List<LessonResponse>> getLessonsByModule(@PathVariable UUID moduleId) {
        List<Lesson> lessons = lessonRepository.findByModuleIdOrderByOrderIndexAsc(moduleId);
        List<LessonResponse> response = lessons.stream().map(this::mapToDTO).toList();
        return ResponseEntity.ok(response);
    }

    // 2. GET: Una lección (DTO)
    @GetMapping("/{id}")
    public ResponseEntity<LessonResponse> getLessonById(@PathVariable UUID id) {
        return lessonRepository.findById(id)
                .map(this::mapToDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 3. POST: Crear lección
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LessonResponse> createLesson(
            @RequestParam("title") String title,
            @RequestParam(value = "videoUrl", required = false) String videoUrl,
            @RequestParam("moduleId") UUID moduleId,
            @RequestParam("orderIndex") Integer orderIndex,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Módulo no encontrado"));

        Lesson newLesson = new Lesson();
        newLesson.setTitle(title);
        newLesson.setVideoUrl(videoUrl);
        newLesson.setOrderIndex(orderIndex);
        newLesson.setModule(module);

        // PDF opcional: guardamos PATH RELATIVO "uploads/pdfs/<uuid>.pdf"
        if (file != null && !file.isEmpty()) {
            requirePdf(file);
            String storedPath = storageService.store(file);
            newLesson.setPdfUrl(storedPath);
        }

        Lesson saved = lessonRepository.save(newLesson);
        return new ResponseEntity<>(mapToDTO(saved), HttpStatus.CREATED);
    }

    // 4. PUT: Actualizar lección
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LessonResponse> updateLesson(
            @PathVariable UUID id,
            @RequestParam("title") String title,
            @RequestParam(value = "videoUrl", required = false) String videoUrl,
            @RequestParam("orderIndex") Integer orderIndex,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lección no encontrada"));

        lesson.setTitle(title);
        lesson.setVideoUrl(videoUrl);
        lesson.setOrderIndex(orderIndex);

        // Si suben PDF nuevo: borramos el anterior (si existe) y guardamos el nuevo
        if (file != null && !file.isEmpty()) {
            requirePdf(file);

            String oldPdf = lesson.getPdfUrl();
            if (oldPdf != null && !oldPdf.isBlank()) {
                storageService.delete(oldPdf); // acepta path relativo o URL legacy
            }

            String storedPath = storageService.store(file);
            lesson.setPdfUrl(storedPath);
        }

        Lesson saved = lessonRepository.save(lesson);
        return ResponseEntity.ok(mapToDTO(saved));
    }

    // 5. DELETE: borra lección + su PDF asociado (si existe)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLesson(@PathVariable UUID id) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lección no encontrada"));

        String pdf = lesson.getPdfUrl();
        if (pdf != null && !pdf.isBlank()) {
            storageService.delete(pdf);
        }

        lessonRepository.delete(lesson);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // Helpers
    // ==========================================
    private LessonResponse mapToDTO(Lesson lesson) {
        return new LessonResponse(
                lesson.getId(),
                lesson.getTitle(),
                lesson.getVideoUrl(),
                lesson.getPdfUrl(), // "uploads/pdfs/<uuid>.pdf"
                lesson.getOrderIndex(),
                lesson.getModule().getId());
    }

    private void requirePdf(MultipartFile file) {
        String ct = file.getContentType();
        if (ct == null || !ct.equalsIgnoreCase("application/pdf")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se permite PDF (application/pdf).");
        }
    }
}
