package com.radioacademy.backend.service.content;

import com.radioacademy.backend.dto.lesson.LessonResponse;
import com.radioacademy.backend.entity.Lesson;
import com.radioacademy.backend.entity.Module;
import com.radioacademy.backend.repository.LessonRepository;
import com.radioacademy.backend.repository.ModuleRepository;
import com.radioacademy.backend.service.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LessonServiceTest {

    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private ModuleRepository moduleRepository;
    @Mock
    private StorageService storageService;

    @InjectMocks
    private LessonService lessonService;

    @Test
    void getLessonsByModule_ShouldReturnList() {
        UUID moduleId = UUID.randomUUID();
        Module module = new Module();
        module.setId(moduleId);

        Lesson lesson = new Lesson();
        lesson.setId(UUID.randomUUID());
        lesson.setTitle("Intro");
        lesson.setModule(module);

        when(lessonRepository.findByModuleIdOrderByOrderIndexAsc(moduleId)).thenReturn(List.of(lesson));

        List<LessonResponse> result = lessonService.getLessonsByModule(moduleId);

        assertEquals(1, result.size());
        assertEquals("Intro", result.get(0).title());
        assertEquals(moduleId, result.get(0).moduleId());
    }

    @Test
    void createLesson_ShouldThrow_WhenModuleNotFound() {
        when(moduleRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> lessonService.createLesson("Title", "url", UUID.randomUUID(), 1, null));
    }

    @Test
    void createLesson_ShouldSave_WhenFileIsValid() {
        UUID moduleId = UUID.randomUUID();
        Module module = new Module();
        module.setId(moduleId);

        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", MediaType.APPLICATION_PDF_VALUE,
                "content".getBytes());

        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));
        when(storageService.store(file)).thenReturn("uploads/pdfs/test.pdf");
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(i -> i.getArgument(0));

        LessonResponse response = lessonService.createLesson("Title", "url", moduleId, 1, file);

        assertNotNull(response);
        assertEquals("uploads/pdfs/test.pdf", response.pdfUrl());
    }

    @Test
    void createLesson_ShouldThrow_WhenFileNotPdf() {
        UUID moduleId = UUID.randomUUID();
        Module module = new Module();
        module.setId(moduleId);

        MockMultipartFile file = new MockMultipartFile("file", "test.png", MediaType.IMAGE_PNG_VALUE,
                "content".getBytes());

        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lessonService.createLesson("Title", "url", moduleId, 1, file));

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void updateLesson_ShouldDeleteOldFile_WhenNewFileProvided() {
        UUID lessonId = UUID.randomUUID();
        Lesson existing = new Lesson();
        existing.setId(lessonId);
        existing.setPdfUrl("uploads/pdfs/old.pdf");
        existing.setModule(new Module()); 

        MockMultipartFile newFile = new MockMultipartFile("file", "new.pdf", MediaType.APPLICATION_PDF_VALUE,
                "new content".getBytes());

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(existing));
        when(storageService.store(newFile)).thenReturn("uploads/pdfs/new.pdf");
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(i -> i.getArgument(0));

        lessonService.updateLesson(lessonId, "Updated Title", "url", 2, newFile);

        verify(storageService).delete("uploads/pdfs/old.pdf");
        assertEquals("uploads/pdfs/new.pdf", existing.getPdfUrl());
    }

    @Test
    void deleteLesson_ShouldCleanupFilesAndRemove() {
        UUID lessonId = UUID.randomUUID();
        Lesson existing = new Lesson();
        existing.setId(lessonId);
        existing.setPdfUrl("uploads/pdfs/todelete.pdf");

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(existing));

        lessonService.deleteLesson(lessonId);

        verify(storageService).delete("uploads/pdfs/todelete.pdf");
        verify(lessonRepository).delete(existing);
    }
}
