package com.radioacademy.backend.service.media;

import com.radioacademy.backend.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class MediaService {

    private static final long MAX_BYTES = 20L * 1024 * 1024; 
    private final StorageService storageService;

    
    public String uploadMedia(MultipartFile file, String folderName) {
        
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo está vacío");
        }

        
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Archivo demasiado grande (máx 20MB)");
        }

        
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.equals("application/pdf") || contentType.startsWith("image/"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tipo de archivo no permitido (solo PDF o imágenes)");
        }

        
        
        
        return storageService.store(file, folderName);
    }

    
    public String uploadMedia(MultipartFile file) {
        return uploadMedia(file, null);
    }

    
    public Resource loadMediaResource(String path) {
        if (path == null || path.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El path es obligatorio");
        }

        
        
        String normalized = Paths.get(path).normalize().toString().replace("\\", "/");

        if (!normalized.startsWith("uploads/")) {
            
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado a esta ruta");
        }

        
        if (normalized.startsWith("uploads/pdfs/")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "PDFs solo vía endpoint /api/student/lessons/{id}/pdf");
        }

        
        Resource resource = storageService.loadAsResource(normalized);

        if (resource == null || !resource.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo no encontrado");
        }

        return resource;
    }

    
    public MediaType determineMediaType(String filename) {
        if (filename.toLowerCase().endsWith(".pdf")) {
            return MediaType.APPLICATION_PDF;
        }
        
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}