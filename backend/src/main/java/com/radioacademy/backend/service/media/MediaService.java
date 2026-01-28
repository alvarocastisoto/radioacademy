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

    private static final long MAX_BYTES = 20L * 1024 * 1024; // 20 MB
    private final StorageService storageService;

    // ✅ LÓGICA DE SUBIDA (Actualizada con folderName)
    public String uploadMedia(MultipartFile file, String folderName) {
        // 1. Validar existencia
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo está vacío");
        }

        // 2. Validar tamaño
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Archivo demasiado grande (máx 20MB)");
        }

        // 3. Validar tipo (MIME Type)
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.equals("application/pdf") || contentType.startsWith("image/"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tipo de archivo no permitido (solo PDF o imágenes)");
        }

        // 4. Delegar almacenamiento físico pasando la carpeta
        // Si folderName es null, StorageService lo manejará correctamente (poniéndolo
        // en raíz de images)
        return storageService.store(file, folderName);
    }

    // Sobrecarga para mantener compatibilidad si alguien llama sin carpeta
    public String uploadMedia(MultipartFile file) {
        return uploadMedia(file, null);
    }

    // ✅ LÓGICA DE DESCARGA (Se mantiene igual, la seguridad ya es robusta)
    public Resource loadMediaResource(String path) {
        if (path == null || path.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El path es obligatorio");
        }

        // 1. Anti Path Traversal (Seguridad crítica)
        // Normalizamos y aseguramos que empieza por "uploads/"
        String normalized = Paths.get(path).normalize().toString().replace("\\", "/");

        if (!normalized.startsWith("uploads/")) {
            // Si intentan acceder a /etc/passwd o salir de uploads, cortamos
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado a esta ruta");
        }

        // Bloqueo específico de PDFs (solo accesibles vía endpoint seguro)
        if (normalized.startsWith("uploads/pdfs/")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "PDFs solo vía endpoint /api/student/lessons/{id}/pdf");
        }

        // 2. Cargar recurso físico
        Resource resource = storageService.loadAsResource(normalized);

        if (resource == null || !resource.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo no encontrado");
        }

        return resource;
    }

    // ✅ Helper para determinar el Content-Type de respuesta
    public MediaType determineMediaType(String filename) {
        if (filename.toLowerCase().endsWith(".pdf")) {
            return MediaType.APPLICATION_PDF;
        }
        // Para imágenes u otros, stream genérico o detección automática
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}