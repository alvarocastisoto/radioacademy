package com.radioacademy.backend.controller;

import com.radioacademy.backend.service.StorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    private static final long MAX_BYTES = 20L * 1024 * 1024; // 20 MB

    private final StorageService storageService;

    public MediaController(StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * SUBIDA (solo ADMIN)
     * - Valida tamaño y tipo (PDF / imágenes).
     * - Devuelve una "path" relativa, NO una URL pública.
     *
     * Requiere que tengas habilitado method security:
     * 
     * @EnableMethodSecurity en tu config.
     */
    @PreAuthorize("hasAnyAuthority('ADMIN','ROLE_ADMIN')")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El archivo está vacío"));
        }

        if (file.getSize() > MAX_BYTES) {
            return ResponseEntity.badRequest().body(Map.of("error", "Archivo demasiado grande (máx 20MB)"));
        }

        String contentType = file.getContentType();
        if (contentType == null || !(contentType.equals("application/pdf") || contentType.startsWith("image/"))) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Tipo de archivo no permitido (solo PDF o imágenes)"));
        }

        // IMPORTANTE:
        // - store() debería devolver una ruta RELATIVA tipo:
        // "uploads/pdfs/<uuid>.pdf" o "uploads/images/<uuid>.jpg"
        // - No devuelvas "http://localhost:8080/..." desde el backend.
        String storedPath = storageService.store(file);

        return ResponseEntity.ok(Map.of("path", storedPath));
    }

    /**
     * DESCARGA (autenticado)
     * - Este endpoint NO hace check de matrícula porque MediaController no sabe si
     * el fichero pertenece
     * a un curso/lección. Para PDFs de lecciones, lo correcto es un endpoint en
     * StudentController
     * que valide matrícula y luego llame a storageService.
     *
     * Si aun así quieres permitir descargar media "genérica" autenticada por path,
     * aquí va.
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> download(
            @RequestParam("path") String path,
            @RequestParam(defaultValue = "true") boolean download) {
        if (path == null || path.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        // Anti path traversal básico: solo permitimos rutas bajo "uploads/"
        String normalized = Paths.get(path).normalize().toString().replace("\\", "/");
        if (!normalized.startsWith("uploads/")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        Resource resource = storageService.loadAsResource(normalized);
        if (resource == null || !resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String filename = resource.getFilename() != null ? resource.getFilename() : "file";
        String disposition = (download ? "attachment" : "inline") + "; filename=\"" + filename + "\"";

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (filename.toLowerCase().endsWith(".pdf")) {
            mediaType = MediaType.APPLICATION_PDF;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(mediaType)
                .body(resource);
    }
}
