package com.radioacademy.backend.service;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
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

    /**
     * Guarda el archivo. Si es imagen, permite subcarpeta (ej: "courses").
     * Si es PDF, ignora la subcarpeta y va a /pdfs por seguridad.
     */
    public String store(MultipartFile file, String subFolder) {
        try {
            if (file == null || file.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo está vacío.");
            }

            String contentType = file.getContentType();
            if (contentType == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de archivo desconocido.");
            }

            
            String baseCategory = resolveBaseCategory(contentType); 
            Path targetDir;

            if (baseCategory.equals("images") && subFolder != null && !subFolder.isBlank()) {
                
                if (subFolder.contains("..") || subFolder.contains("/") || subFolder.contains("\\")) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre de carpeta inválido.");
                }
                
                targetDir = uploadsRoot.resolve("images").resolve(subFolder);
            } else {
                
                targetDir = uploadsRoot.resolve(baseCategory);
            }

            
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            
            String extension = detectExtension(file.getOriginalFilename(), contentType);
            String filename = UUID.randomUUID() + extension;

            
            Path destinationFile = targetDir.resolve(filename).normalize().toAbsolutePath();
            if (!destinationFile.startsWith(uploadsRoot)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ruta inválida.");
            }

            
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            
            
            
            
            Path relativePath = uploadsRoot.getParent().relativize(destinationFile);
            return relativePath.toString().replace("\\", "/");

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al guardar el archivo: " + e.getMessage());
        }
    }

    
    public String store(MultipartFile file) {
        return store(file, null);
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

    public void delete(String filenameOrUrl) {
        try {
            if (filenameOrUrl == null || filenameOrUrl.isBlank())
                return;

            String s = filenameOrUrl.replace("\\", "/");
            int idx = s.indexOf("uploads/");
            if (idx < 0)
                return;

            String path = s.substring(idx);
            String normalized = Paths.get(path).normalize().toString().replace("\\", "/");

            if (!normalized.startsWith("uploads/"))
                return;

            String withoutPrefix = normalized.substring("uploads/".length());
            Path file = uploadsRoot.resolve(withoutPrefix).normalize().toAbsolutePath();

            if (!file.startsWith(uploadsRoot.toAbsolutePath()))
                return;

            Files.deleteIfExists(file);

        } catch (IOException ignored) {
            System.err.println("⚠️ No se pudo borrar: " + filenameOrUrl);
        }
    }

    private String resolveBaseCategory(String contentType) {
        if (contentType.equals("application/pdf"))
            return "pdfs";
        if (contentType.startsWith("image/"))
            return "images";
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo no permitido (solo PDF o imágenes).");
    }

    private String detectExtension(String originalFilename, String contentType) {
        if (originalFilename != null && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
            if (contentType.equals("application/pdf") && ext.equals(".pdf"))
                return ".pdf";
            if (contentType.startsWith("image/")
                    && (ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png") || ext.equals(".webp"))) {
                return ext.equals(".jpeg") ? ".jpg" : ext;
            }
        }
        if (contentType.equals("application/pdf"))
            return ".pdf";
        if (contentType.equals("image/png"))
            return ".png";
        if (contentType.equals("image/webp"))
            return ".webp";
        return ".jpg";
    }
}