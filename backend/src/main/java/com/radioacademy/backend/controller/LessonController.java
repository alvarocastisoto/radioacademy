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

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer; // 👈 IMPORTANTE
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern; // 👈 IMPORTANTE

@RestController
@RequestMapping("/api/lessons")
public class LessonController {

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    // Carpeta física (sin barra inicial para que sea relativa a la raíz)
    private static final String UPLOAD_DIR = "uploads";

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
            // saveFile ahora devuelve SOLO el nombre (ej: "uuid_alvaro.pdf")
            // sin la carpeta "uploads/" delante.
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
                .orElseThrow(() -> new RuntimeException("Lección no encontrada"));

        lesson.setTitle(title);
        lesson.setVideoUrl(videoUrl);
        lesson.setOrderIndex(orderIndex);

        if (file != null && !file.isEmpty()) {
            // Borrar archivo antiguo si quisieras (Opcional, para no llenar el disco)
            // deleteFile(lesson.getPdfUrl());

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
        // Aquí podrías borrar el archivo físico si quisieras
        lessonRepository.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // ==========================================
    // 🛠️ MÉTODOS AUXILIARES (LA MAGIA)
    // ==========================================

    private String saveFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty())
            return null;

        // 1. Obtener nombre original y limpiarlo
        String originalFilename = file.getOriginalFilename();
        String sanitizedFilename = sanitizeFileName(originalFilename);

        // 2. Generar nombre único: UUID + Nombre Limpio
        // Resultado: "550e8400..._alvaro_castineira.pdf"
        String uniqueFileName = UUID.randomUUID().toString() + "_" + sanitizedFilename;

        // 3. Preparar ruta
        String projectRoot = System.getProperty("user.dir");
        Path uploadPath = Paths.get(projectRoot, UPLOAD_DIR);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 4. Guardar
        Path filePath = uploadPath.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        System.out.println("✅ Archivo guardado: " + uniqueFileName);

        // 5. RETORNO IMPORTANTE:
        // Devolvemos SOLO el nombre del archivo.
        // NO devolvemos "uploads/" + nombre.
        // Así la base de datos queda limpia: "uuid_archivo.pdf"
        return uniqueFileName;
    }

    /**
     * Limpia el nombre del archivo:
     * - Quita acentos (Á -> A)
     * - Quita caracteres raros (ñ -> n)
     * - Reemplaza espacios por guiones bajos
     * - Mantiene solo letras, números, puntos, guiones y guiones bajos
     */
    private String sanitizeFileName(String originalFilename) {
        if (originalFilename == null)
            return "archivo_sin_nombre";

        // 1. Separar extensión
        String extension = "";
        int i = originalFilename.lastIndexOf('.');
        String nameWithoutExtension = originalFilename;

        if (i > 0) {
            extension = originalFilename.substring(i); // .pdf
            nameWithoutExtension = originalFilename.substring(0, i);
        }

        // 2. Normalizar (Álvaro -> Alvaro)
        String normalized = Normalizer.normalize(nameWithoutExtension, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String slug = pattern.matcher(normalized).replaceAll("");

        // 3. Reemplazar espacios y caracteres no permitidos
        // Todo a minúsculas, espacios a _, quita todo lo que no sea a-z, 0-9, - ó _
        slug = slug.toLowerCase()
                .replaceAll("\\s+", "_") // Espacios a guion bajo
                .replaceAll("[^a-z0-9\\-_]", ""); // Borrar caracteres raros

        // 4. Reconstruir
        return slug + extension.toLowerCase();
    }
}