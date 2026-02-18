package com.radioacademy.backend.service.content;

import com.radioacademy.backend.dto.lesson.LessonResponse;
import com.radioacademy.backend.entity.Lesson;
import com.radioacademy.backend.entity.Module;
import com.radioacademy.backend.repository.LessonRepository;
import com.radioacademy.backend.repository.ModuleRepository;
import com.radioacademy.backend.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;
    private final ModuleRepository moduleRepository;
    private final StorageService storageService; 

    
    public List<LessonResponse> getLessonsByModule(UUID moduleId) {
        return lessonRepository.findByModuleIdOrderByOrderIndexAsc(moduleId)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    
    public LessonResponse getLessonById(UUID id) {
        return lessonRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lección no encontrada"));
    }

    
    @Transactional
    public LessonResponse createLesson(String title, String videoUrl, UUID moduleId, Integer orderIndex,
            MultipartFile file) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Módulo no encontrado"));

        Lesson newLesson = new Lesson();
        newLesson.setTitle(title);
        newLesson.setVideoUrl(videoUrl);
        newLesson.setOrderIndex(orderIndex);
        newLesson.setModule(module);

        if (file != null && !file.isEmpty()) {
            validatePdf(file);
            String storedPath = storageService.store(file);
            newLesson.setPdfUrl(storedPath);
        }

        Lesson saved = lessonRepository.save(newLesson);
        return mapToDTO(saved);
    }

    
    @Transactional
    public LessonResponse updateLesson(UUID id, String title, String videoUrl, Integer orderIndex, MultipartFile file) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lección no encontrada"));

        lesson.setTitle(title);
        lesson.setVideoUrl(videoUrl);
        lesson.setOrderIndex(orderIndex);

        if (file != null && !file.isEmpty()) {
            validatePdf(file);

            
            if (lesson.getPdfUrl() != null && !lesson.getPdfUrl().isBlank()) {
                storageService.delete(lesson.getPdfUrl());
            }

            String storedPath = storageService.store(file);
            lesson.setPdfUrl(storedPath);
        }

        Lesson saved = lessonRepository.save(lesson);
        return mapToDTO(saved);
    }

    
    @Transactional
    public void deleteLesson(UUID id) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lección no encontrada"));

        if (lesson.getPdfUrl() != null && !lesson.getPdfUrl().isBlank()) {
            storageService.delete(lesson.getPdfUrl());
        }

        lessonRepository.delete(lesson);
    }

    

    private LessonResponse mapToDTO(Lesson lesson) {
        return new LessonResponse(
                lesson.getId(),
                lesson.getTitle(),
                lesson.getVideoUrl(),
                lesson.getPdfUrl(),
                lesson.getOrderIndex(),
                lesson.getModule().getId());
    }

    private void validatePdf(MultipartFile file) {
        String ct = file.getContentType();
        if (ct == null || !ct.equalsIgnoreCase("application/pdf")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se permite PDF (application/pdf).");
        }
    }
}