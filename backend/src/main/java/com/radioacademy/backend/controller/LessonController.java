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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/lessons")
public class LessonController {

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    // Carpeta donde se guardarán los archivos (en la raíz del proyecto)
    private static final String UPLOAD_DIR = "uploads/";

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

    // 3. POST: Crear lección (CON GUARDADO DE ARCHIVO REAL)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Lesson> createLesson(
            @RequestParam("title") String title,
            @RequestParam("videoUrl") String videoUrl,
            @RequestParam("moduleId") UUID moduleId,
            @RequestParam("orderIndex") Integer orderIndex,
            @RequestParam(value = "file", required = false) MultipartFile file) throws IOException { // Se añade throws
                                                                                                     // IOException para
                                                                                                     // el manejo de
                                                                                                     // archivos

        Optional<Module> moduleOptional = moduleRepository.findById(moduleId);

        if (moduleOptional.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Lesson newLesson = new Lesson();
        newLesson.setTitle(title);
        newLesson.setVideoUrl(videoUrl);
        newLesson.setOrderIndex(orderIndex);
        newLesson.setModule(moduleOptional.get());

        // LÓGICA DE GUARDADO DE ARCHIVO
        if (file != null && !file.isEmpty()) {
            String fileName = saveFile(file); // Guarda en disco
            newLesson.setPdfUrl(fileName); // Guarda la ruta en BD
        }

        Lesson savedLesson = lessonRepository.save(newLesson);
        return new ResponseEntity<>(savedLesson, HttpStatus.CREATED);
    }

    // 4. PUT: Actualizar lección (CON GUARDADO DE ARCHIVO REAL)
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Lesson> updateLesson(
            @PathVariable UUID id,
            @RequestParam("title") String title,
            @RequestParam("videoUrl") String videoUrl,
            @RequestParam("orderIndex") Integer orderIndex,
            @RequestParam(value = "file", required = false) MultipartFile file) throws IOException { // Se añade throws
                                                                                                     // IOException

        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lección no encontrada"));

        lesson.setTitle(title);
        lesson.setVideoUrl(videoUrl);
        lesson.setOrderIndex(orderIndex);

        // Si envían un archivo nuevo, lo guardamos y actualizamos la URL
        // Si no envían nada, mantenemos el PDF que ya tenía (no hacemos nada)
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
        // Opcional: Aquí podrías añadir lógica para borrar también el archivo físico
        // del disco si quisieras
        lessonRepository.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // --- MÉTODO PRIVADO AUXILIAR PARA GUARDAR EN DISCO ---
    private String saveFile(MultipartFile file) throws IOException {
        // DIAGNÓSTICO 1: ¿Llega el archivo?
        if (file == null || file.isEmpty()) {
            System.out.println("🔴 ERROR: El archivo ha llegado NULO o VACÍO al método saveFile.");
            return null;
        }

        System.out.println("🟢 Archivo recibido: " + file.getOriginalFilename() + " (" + file.getSize() + " bytes)");

        // 1. Verificar/Crear directorio
        // Usamos user.dir para asegurarnos de que apunta a la raíz del proyecto
        String projectRoot = System.getProperty("user.dir");
        Path uploadPath = Paths.get(projectRoot, UPLOAD_DIR);

        // DIAGNÓSTICO 2: ¿Dónde está intentando guardar EXACTAMENTE?
        System.out.println("📂 Ruta absoluta de guardado: " + uploadPath.toAbsolutePath().toString());

        if (!Files.exists(uploadPath)) {
            System.out.println("✨ La carpeta no existía. Creándola ahora...");
            Files.createDirectories(uploadPath);
        } else {
            System.out.println("✅ La carpeta ya existe.");
        }

        // 2. Generar nombre único
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

        // 3. Ruta completa
        Path filePath = uploadPath.resolve(fileName);

        // 4. Copiar archivo
        try {
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("🚀 ÉXITO: Archivo guardado en: " + filePath.toAbsolutePath());
        } catch (IOException e) {
            System.out.println("🔥 ERROR CRÍTICO al escribir en disco: " + e.getMessage());
            throw e;
        }

        // 5. Retornar la ruta relativa
        return UPLOAD_DIR + fileName;
    }
}