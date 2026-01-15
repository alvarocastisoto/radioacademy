package com.radioacademy.backend.controller;

import com.radioacademy.backend.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    @Autowired
    private StorageService storageService;

    @PostMapping("/upload")
    // 1. Quitamos @Valid (no sirve aquí).
    // 2. @RequestParam ya obliga a que venga el parámetro "file"
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {

        // Seguridad extra: ¿El archivo pesa 0 bytes?
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El archivo está vacío"));
        }

        String fullUrl = storageService.store(file);

        // Devuelve JSON: { "url": "http://..." }
        return ResponseEntity.ok(Map.of("url", fullUrl));
    }
}