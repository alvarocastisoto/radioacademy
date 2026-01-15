package com.radioacademy.backend.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class StorageService {

    // 📍 ANCLAJE ROBUSTO:
    // "user.dir" es la raíz de tu proyecto (donde está el pom.xml).
    // Creamos la estructura: Proyecto/uploads/images
    private final Path rootLocation = Paths.get(System.getProperty("user.dir"), "uploads", "images");

    @PostConstruct
    public void init() throws IOException {
        // Crea la carpeta si no existe (incluyendo subcarpetas)
        Files.createDirectories(rootLocation);
        System.out.println("✅ ALMACENAMIENTO LISTO EN: " + rootLocation.toAbsolutePath());
    }

    public String store(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error: El archivo está vacío.");
            }

            // Nombre único
            String originalFilename = file.getOriginalFilename();
            String extension = (originalFilename != null && originalFilename.contains("."))
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";

            String filename = java.util.UUID.randomUUID().toString() + extension;

            // Guardar físico
            Path destinationFile = this.rootLocation.resolve(filename).normalize().toAbsolutePath();
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            // 👇👇👇 ESTO ES LO QUE ARREGLA LA VISTA PREVIA 👇👇👇
            // Devolvemos la URL HTTP completa, no solo el nombre del archivo
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/images/")
                    .path(filename)
                    .toUriString();

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al guardar el archivo: " + e.getMessage());
        }
    }

    public void delete(String filenameUrl) {
        try {
            // A veces nos llega la URL completa, extraemos solo el nombre del archivo
            String filename = filenameUrl;
            if (filenameUrl.contains("/")) {
                filename = filenameUrl.substring(filenameUrl.lastIndexOf("/") + 1);
            }

            Path file = rootLocation.resolve(filename);
            Files.deleteIfExists(file);
        } catch (IOException e) {
            System.err.println("⚠️ No se pudo borrar la imagen antigua: " + filenameUrl);
        }
    }
}