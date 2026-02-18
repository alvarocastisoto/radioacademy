package com.radioacademy.backend.controller;

import com.radioacademy.backend.service.media.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@EnableMethodSecurity
public class MediaController {

    private final MediaService mediaService;

    /**
     * SUBIDA (Solo ADMIN)
     * Ahora acepta un parámetro opcional "folder" (ej: "courses", "users")
     */
    @PreAuthorize("hasAnyAuthority('ADMIN','ROLE_ADMIN')")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", required = false) String folder 
    ) {

        
        String storedPath = mediaService.uploadMedia(file, folder);

        
        return ResponseEntity.ok(Map.of("path", storedPath));
    }

    /**
     * DESCARGA (Genérica Autenticada)
     * Este método no cambia, ya que el "path" incluye la carpeta
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> download(
            @RequestParam("path") String path,
            @RequestParam(defaultValue = "true") boolean download) {

        
        Resource resource = mediaService.loadMediaResource(path);

        
        String filename = resource.getFilename() != null ? resource.getFilename() : "file";
        String disposition = (download ? "attachment" : "inline") + "; filename=\"" + filename + "\"";
        MediaType mediaType = mediaService.determineMediaType(filename);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(mediaType)
                .body(resource);
    }
}