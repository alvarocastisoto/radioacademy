package com.radioacademy.backend.controller;

import com.radioacademy.backend.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Map;

@RestController
@RequestMapping("/api/media")
@CrossOrigin(origins = "http://localhost:4200")
public class MediaController {

    @Autowired
    private StorageService storageService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        // 1. Guardamos el archivo
        String filename = storageService.store(file);

        // 2. Generamos la URL pública para acceder a él
        // Esto crea algo como: http://localhost:8080/uploads/nombre-fichero.jpg
        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/images/")
                .path(filename)
                .toUriString();

        return ResponseEntity.ok(Map.of("url", url));
    }
}