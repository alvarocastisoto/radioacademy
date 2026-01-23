package com.radioacademy.backend.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Locale;
import java.util.UUID;

@Service
public class StorageService {

    private final Path uploadsRoot = Paths.get(System.getProperty("user.dir"), "uploads")
            .toAbsolutePath()
            .normalize();

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(uploadsRoot.resolve("images"));
        Files.createDirectories(uploadsRoot.resolve("pdfs"));
    }

    public String store(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo está vacío.");
            }

            String contentType = file.getContentType();
            if (contentType == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de archivo desconocido.");
            }

            String subdir = resolveSubdir(contentType);
            Path targetDir = uploadsRoot.resolve(subdir);
            Files.createDirectories(targetDir);

            String extension = detectExtension(file.getOriginalFilename(), contentType);
            String filename = UUID.randomUUID() + extension;

            Path destinationFile = targetDir.resolve(filename).normalize().toAbsolutePath();
            if (!destinationFile.startsWith(uploadsRoot)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ruta inválida.");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            return "uploads/" + subdir + "/" + filename;

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al guardar el archivo: " + e.getMessage());
        }
    }

    public Resource loadAsResource(String relativePath) {
        try {
            if (relativePath == null || relativePath.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path vacío.");
            }

            String normalized = Paths.get(relativePath).normalize().toString().replace("\\", "/");
            if (!normalized.startsWith("uploads/")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path inválido.");
            }

            String withoutPrefix = normalized.substring("uploads/".length());
            Path file = uploadsRoot.resolve(withoutPrefix).normalize().toAbsolutePath();

            if (!file.startsWith(uploadsRoot)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path inválido.");
            }

            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo no encontrado.");
            }

            return resource;

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error leyendo el archivo.");
        }
    }

    /**
     * Borra un archivo a partir de su path relativo o URL (legacy).
     */
    public void delete(String filenameOrUrl) {
        try {
            if (filenameOrUrl == null || filenameOrUrl.isBlank())
                return;

            String s = filenameOrUrl.replace("\\", "/");

            // 1) Extrae desde la primera aparición de "uploads/"
            int idx = s.indexOf("uploads/");
            if (idx < 0)
                return;

            String path = s.substring(idx); // "uploads/images/.."

            // 2) Normaliza y valida
            String normalized = Paths.get(path).normalize().toString().replace("\\", "/");
            if (!normalized.startsWith("uploads/"))
                return;

            String withoutPrefix = normalized.substring("uploads/".length());
            Path file = uploadsRoot.resolve(withoutPrefix).normalize().toAbsolutePath();

            // 3) Anti path traversal
            if (!file.startsWith(uploadsRoot.toAbsolutePath()))
                return;

            Files.deleteIfExists(file);

        } catch (IOException ignored) {
            System.err.println("⚠️ No se pudo borrar: " + filenameOrUrl);
        }
    }

    private String resolveSubdir(String contentType) {
        if (contentType.equals("application/pdf"))
            return "pdfs";
        if (contentType.startsWith("image/"))
            return "images";
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo no permitido (solo PDF o imágenes).");
    }

    private String detectExtension(String originalFilename, String contentType) {
        // Si viene extensión, úsala (con normalización)
        if (originalFilename != null && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
            // lista blanca básica
            if (contentType.equals("application/pdf") && ext.equals(".pdf"))
                return ".pdf";
            if (contentType.startsWith("image/")
                    && (ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png") || ext.equals(".webp"))) {
                return ext.equals(".jpeg") ? ".jpg" : ext;
            }
        }

        // Si no viene, decide por contentType
        if (contentType.equals("application/pdf"))
            return ".pdf";
        if (contentType.equals("image/png"))
            return ".png";
        if (contentType.equals("image/webp"))
            return ".webp";
        return ".jpg";
    }

}
