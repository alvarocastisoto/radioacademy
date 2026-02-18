package com.radioacademy.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class StorageServiceTest {

    private StorageService storageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        storageService = new StorageService();
        
        
        
        
        
        
        
        
        

        Path simulatedUploadsRoot = tempDir.resolve("uploads");
        Files.createDirectories(simulatedUploadsRoot); 

        ReflectionTestUtils.setField(storageService, "uploadsRoot", simulatedUploadsRoot);
        storageService.init();
    }

    @Test
    void store_ShouldSaveImage_WhenValid() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", MediaType.IMAGE_JPEG_VALUE, "test content".getBytes());

        String path = storageService.store(file);

        assertNotNull(path);
        assertTrue(path.startsWith("uploads/images/"));
        assertTrue(path.endsWith(".jpg"));

        
        
        
        

        
        Path imagesDir = tempDir.resolve("uploads/images");
        assertTrue(Files.exists(imagesDir));

        
        String filename = path.substring(path.lastIndexOf("/") + 1);
        Path savedFile = imagesDir.resolve(filename);
        assertTrue(Files.exists(savedFile));
    }

    @Test
    void store_ShouldSavePdf_WhenValid() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", MediaType.APPLICATION_PDF_VALUE, "pdf content".getBytes());

        String path = storageService.store(file);

        assertNotNull(path);
        assertTrue(path.startsWith("uploads/pdfs/"));
        assertTrue(path.endsWith(".pdf"));
    }

    @Test
    void store_ShouldThrow_WhenEmpty() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.png", MediaType.IMAGE_PNG_VALUE, new byte[0]);

        assertThrows(ResponseStatusException.class, () -> storageService.store(file));
    }

    @Test
    void store_ShouldThrow_WhenInvalidType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "text".getBytes());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> storageService.store(file));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void loadAsResource_ShouldReturnResource_WhenExists() throws IOException {
        
        Path imageDir = tempDir.resolve("uploads/images");
        Files.createDirectories(imageDir);
        Path file = imageDir.resolve("test.jpg");
        Files.writeString(file, "content");

        
        
        Resource resource = storageService.loadAsResource("uploads/images/test.jpg");

        
        assertTrue(resource.exists());
        assertTrue(resource.isReadable());
    }

    @Test
    void loadAsResource_ShouldThrow_WhenNotFound() {
        assertThrows(ResponseStatusException.class,
                () -> storageService.loadAsResource("uploads/images/non-existent.jpg"));
    }

    @Test
    void loadAsResource_ShouldThrow_WhenPathTraversal() {
        
        assertThrows(ResponseStatusException.class,
                () -> storageService.loadAsResource("uploads/../../windows/system32/calc.exe"));
    }

    @Test
    void delete_ShouldRemoveFile() throws IOException {
        
        Path imageDir = tempDir.resolve("uploads/images");
        Files.createDirectories(imageDir);
        Path file = imageDir.resolve("delete-me.jpg");
        Files.writeString(file, "content");

        assertTrue(Files.exists(file));

        
        storageService.delete("uploads/images/delete-me.jpg");

        
        assertFalse(Files.exists(file));
    }
}
