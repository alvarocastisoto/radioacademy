package com.radioacademy.backend.controller;

import com.radioacademy.backend.service.media.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    /**
     * SUBIDA (Solo ADMIN)
     */
    @PreAuthorize("hasAnyAuthority('ADMIN','ROLE_ADMIN')")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {

        // Delegamos validación y guardado
        String storedPath = mediaService.uploadMedia(file);

        return ResponseEntity.ok(Map.of("path", storedPath));
    }

    /**
     * DESCARGA (Genérica Autenticada)
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> download(
            @RequestParam("path") String path,
            @RequestParam(defaultValue = "true") boolean download) {

        // 1. Obtener el recurso validado
        Resource resource = mediaService.loadMediaResource(path);

        // 2. Preparar Headers HTTP (Esto es responsabilidad del Controller)
        String filename = resource.getFilename() != null ? resource.getFilename() : "file";
        String disposition = (download ? "attachment" : "inline") + "; filename=\"" + filename + "\"";
        MediaType mediaType = mediaService.determineMediaType(filename);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(mediaType)
                .body(resource);
    }
}