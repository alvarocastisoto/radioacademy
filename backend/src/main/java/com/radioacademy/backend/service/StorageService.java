package com.radioacademy.backend.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

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
                throw new RuntimeException("Error: El archivo está vacío.");
            }

            // 1. Generar nombre único
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            } else {
                extension = ".jpg"; // Fallback por defecto
            }
            
            String filename = UUID.randomUUID().toString() + extension;

            // 2. Guardar archivo físico
            Path destinationFile = this.rootLocation.resolve(filename).normalize().toAbsolutePath();
            
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            // 3. RETORNAR URL COMPLETA (Crucial para que Angular muestre la foto)
            // Genera: http://localhost:8080/uploads/images/tu-archivo.jpg
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/images/")
                    .path(filename)
                    .toUriString();

        } catch (IOException e) {
            throw new RuntimeException("Error al guardar el archivo: " + e.getMessage());
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