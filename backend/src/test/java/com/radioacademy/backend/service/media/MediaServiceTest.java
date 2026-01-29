package com.radioacademy.backend.service.media;

import com.radioacademy.backend.service.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock
    private StorageService storageService;

    @InjectMocks
    private MediaService mediaService;

    @Test
    void uploadMedia_ShouldThrow_WhenFileEmpty() {
        MockMultipartFile file = new MockMultipartFile("file", "", "image/png", new byte[0]);
        assertThrows(ResponseStatusException.class, () -> mediaService.uploadMedia(file));
    }

    @Test
    void uploadMedia_ShouldThrow_WhenFileTooLarge() {
        // Enforce size check mock or rely on logic inside?
        // The service checks file.getSize().
        MockMultipartFile file = spy(new MockMultipartFile("file", "large.png", "image/png", new byte[10]));
        when(file.getSize()).thenReturn(21L * 1024 * 1024); // 21 MB

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> mediaService.uploadMedia(file));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void uploadMedia_ShouldThrow_WhenInvalidType() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> mediaService.uploadMedia(file));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void uploadMedia_ShouldStore_WhenValid() {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", "content".getBytes());
        when(storageService.store(file, null)).thenReturn("uploads/images/test.png");

        String result = mediaService.uploadMedia(file);

        assertEquals("uploads/images/test.png", result);
        verify(storageService).store(file, null);
    }

    @Test
    void loadMediaResource_ShouldThrow_WhenPathRestricted() {
        // Direct access to PDFs is forbidden
        assertThrows(ResponseStatusException.class,
                () -> mediaService.loadMediaResource("uploads/pdfs/secret.pdf"));
    }

    @Test
    void loadMediaResource_ShouldThrow_WhenPathTraversal() {
        assertThrows(ResponseStatusException.class,
                () -> mediaService.loadMediaResource("../etc/passwd"));
    }

    @Test
    void loadMediaResource_ShouldReturnResource_WhenValidImage() {
        String path = "uploads/images/test.png";
        Resource resource = mock(Resource.class);
        when(resource.exists()).thenReturn(true);
        when(storageService.loadAsResource(path)).thenReturn(resource);

        Resource result = mediaService.loadMediaResource(path);

        assertNotNull(result);
    }

    @Test
    void determineMediaType_ShouldIdentifyPdf() {
        MediaType type = mediaService.determineMediaType("file.pdf");
        assertEquals(MediaType.APPLICATION_PDF, type);
    }

    @Test
    void determineMediaType_ShouldDefaultToOctetStream() {
        MediaType type = mediaService.determineMediaType("file.xyz");
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, type);
    }
}
