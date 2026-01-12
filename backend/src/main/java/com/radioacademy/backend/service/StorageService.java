package com.radioacademy.backend.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class StorageService {

    // Nombre de la carpeta donde se guardarán los archivos
    @Value("${media.location:uploads/images}")
    private String mediaLocation;

    private Path rootLocation;

    @PostConstruct
    public void init() throws IOException {
        rootLocation = Paths.get(mediaLocation);
        Files.createDirectories(rootLocation);
    }

    public String store(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Error: El archivo está vacío.");
            }

            // 1. Generamos un nombre único (UUID) para evitar sobrescribir archivos
            // Ejemplo: "avatar.jpg" -> "550e8400-e29b-41d4-a716-446655440000.jpg"
            String filename = UUID.randomUUID().toString();
            String extension = "";

            int i = file.getOriginalFilename().lastIndexOf('.');
            if (i > 0) {
                extension = file.getOriginalFilename().substring(i); // .jpg, .png
            }

            String storedFilename = filename + extension;
            // 2. Copiamos el archivo a la carpeta destino
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, this.rootLocation.resolve(storedFilename),
                        StandardCopyOption.REPLACE_EXISTING);
            }

            return storedFilename; // Devolvemos el nombre final
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar el archivo: " + e.getMessage());
        }
    }

    public void delete(String filename) {
        try {
            Path file = rootLocation.resolve(filename);
            // deleteIfExists es perfecto: si el archivo no está, no da error, simplemente
            // sigue.
            Files.deleteIfExists(file);
        } catch (IOException e) {
            // Solo logueamos el error, no queremos romper la actualización del perfil
            // por culpa de un archivo viejo que no se pudo borrar.
            System.err.println("⚠️ No se pudo borrar la imagen antigua: " + filename);
        }
    }
}