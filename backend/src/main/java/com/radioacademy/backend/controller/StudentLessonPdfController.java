package com.radioacademy.backend.controller;

import com.radioacademy.backend.repository.EnrollmentRepository;
import com.radioacademy.backend.repository.LessonRepository;
import com.radioacademy.backend.repository.UserRepository;
import com.radioacademy.backend.dto.internal.LessonPdfInfo;
import com.radioacademy.backend.service.StorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/api/student")
public class StudentLessonPdfController {

    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    public StudentLessonPdfController(LessonRepository lessonRepository,
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository,
            StorageService storageService) {
        this.lessonRepository = lessonRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    /**
     * Ver (inline) o Descargar (attachment) el PDF de una lección SOLO si estás
     * matriculado.
     *
     * - inline: GET /api/student/lessons/{lessonId}/pdf
     * - download: GET /api/student/lessons/{lessonId}/pdf?download=true
     */
    @GetMapping("/lessons/{lessonId}/pdf")
    public ResponseEntity<Resource> getLessonPdf(
            @PathVariable UUID lessonId,
            @RequestParam(defaultValue = "false") boolean download,
            Authentication authentication) {
        UUID userId = resolveUserIdFromAuth(authentication);

        LessonPdfInfo info = lessonRepository.findPdfInfo(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lección no encontrada"));

        String pdfPath = info.pdfUrl();
        if (pdfPath == null || pdfPath.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Esta lección no tiene PDF");
        }

        // Matricula obligatoria
        boolean enrolled = enrollmentRepository.existsByUserIdAndCourseId(userId, info.courseId());
        if (!enrolled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No matriculado en este curso");
        }

        // Sirve el fichero desde disco (path relativo tipo "uploads/pdfs/uuid.pdf")
        Resource resource = storageService.loadAsResource(pdfPath);

        String filename = inferFilename(pdfPath, lessonId);
        String disposition = (download ? "attachment" : "inline") + "; filename=\"" + safeFilename(filename) + "\"";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .cacheControl(CacheControl.noStore()) // contenido premium
                .body(resource);
    }

    private UUID resolveUserIdFromAuth(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }

        // Con tu JwtAuthenticationFilter, getName() == email
        String email = authentication.getName();
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Principal inválido");
        }

        return userRepository.findByEmail(email)
                .map(u -> u.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
    }

    private String inferFilename(String pdfPath, UUID lessonId) {
        try {
            String name = Paths.get(pdfPath).getFileName().toString();
            if (name != null && !name.isBlank())
                return name;
        } catch (Exception ignored) {
        }
        return "lesson-" + lessonId + ".pdf";
    }

    private String safeFilename(String name) {
        return name.replace("\"", "").replace("\r", "").replace("\n", "");
    }
}
