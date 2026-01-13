package com.radioacademy.backend.controller;

import com.radioacademy.backend.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/media")
@CrossOrigin(origins = "http://localhost:4200")
public class MediaController {

    @Autowired
    private StorageService storageService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {

        // 1. El servicio ya nos devuelve la URL COMPLETA
        // (http://localhost:8080/uploads/images/foto.jpg)
        String fullUrl = storageService.store(file);

        // 2. Devolvemos la URL tal cual (Texto plano).
        // NADA de Maps, NADA de JSON, NADA de ServletUriComponentsBuilder aquí.
        return ResponseEntity.ok(fullUrl);
    }
}