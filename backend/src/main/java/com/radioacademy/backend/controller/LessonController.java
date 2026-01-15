package com.radioacademy.backend.controller;

import com.radioacademy.backend.entity.Lesson;
import com.radioacademy.backend.entity.Module;
import com.radioacademy.backend.repository.LessonRepository;
import com.radioacademy.backend.repository.ModuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/lessons")
public class LessonController {

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    // ✅ CAMBIO AQUÍ: Ahora guardamos en una subcarpeta específica
    private static final String UPLOAD_DIR = "uploads/pdfs";

    // 1. GET: Lecciones por módulo
    @GetMapping("/module/{moduleId}")
    public ResponseEntity<List<Lesson>> getLessonsByModule(@PathVariable UUID moduleId) {
        List<Lesson> lessons = lessonRepository.findByModuleIdOrderByOrderIndexAsc(moduleId);
        return ResponseEntity.ok(lessons);
    }

    // 2. GET: Una lección
    @GetMapping("/{id}")
    public ResponseEntity<Lesson> getLessonById(@PathVariable UUID id) {
        return lessonRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 3. POST: Crear lección
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Lesson> createLesson(
            @RequestParam("title") String title,
            @RequestParam("videoUrl") String videoUrl,
            @RequestParam("moduleId") UUID moduleId,
            @RequestParam("orderIndex") Integer orderIndex,
            @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {

        Optional<Module> moduleOptional = moduleRepository.findById(moduleId);

        if (moduleOptional.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Lesson newLesson = new Lesson();
        newLesson.setTitle(title);
        newLesson.setVideoUrl(videoUrl);
        newLesson.setOrderIndex(orderIndex);
        newLesson.setModule(moduleOptional.get());

        // GUARDADO DE ARCHIVO
        if (file != null && !file.isEmpty()) {
            String fileName = saveFile(file);
            newLesson.setPdfUrl(fileName);
        }

        Lesson savedLesson = lessonRepository.save(newLesson);
        return new ResponseEntity<>(savedLesson, HttpStatus.CREATED);
    }

    // 4. PUT: Actualizar lección
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Lesson> updateLesson(
            @PathVariable UUID id,
            @RequestParam("title") String title,
            @RequestParam("videoUrl") String videoUrl,
            @RequestParam("orderIndex") Integer orderIndex,
            @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {

        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lección no encontrada"));

        lesson.setTitle(title);
        lesson.setVideoUrl(videoUrl);
        lesson.setOrderIndex(orderIndex);

        if (file != null && !file.isEmpty()) {
            String fileName = saveFile(file);
            lesson.setPdfUrl(fileName);
        }

        return ResponseEntity.ok(lessonRepository.save(lesson));
    }

    // 5. DELETE: Borrar lección
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLesson(@PathVariable UUID id) {
        if (!lessonRepository.existsById(id)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        lessonRepository.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // ==========================================
    // 🛠️ MÉTODOS AUXILIARES
    // ==========================================

    private String saveFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty())
            return null;

        String originalFilename = file.getOriginalFilename();
        String sanitizedFilename = sanitizeFileName(originalFilename);
        String uniqueFileName = UUID.randomUUID().toString() + "_" + sanitizedFilename;

        String projectRoot = System.getProperty("user.dir");
        Path uploadPath = Paths.get(projectRoot, UPLOAD_DIR);

        // Esto crea "uploads" Y TAMBIÉN "uploads/pdfs" si no existen
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        System.out.println("✅ Archivo guardado en: " + filePath.toString());

        return uniqueFileName;
    }

    private String sanitizeFileName(String originalFilename) {
        if (originalFilename == null)
            return "archivo_sin_nombre";

        String extension = "";
        int i = originalFilename.lastIndexOf('.');
        String nameWithoutExtension = originalFilename;

        if (i > 0) {
            extension = originalFilename.substring(i);
            nameWithoutExtension = originalFilename.substring(0, i);
        }

        String normalized = Normalizer.normalize(nameWithoutExtension, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String slug = pattern.matcher(normalized).replaceAll("");

        slug = slug.toLowerCase()
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-z0-9\\-_]", "");

        return slug + extension.toLowerCase();
    }
}