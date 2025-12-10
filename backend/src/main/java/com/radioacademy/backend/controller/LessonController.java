package com.radioacademy.backend.controller;

import com.radioacademy.backend.dto.CreateLessonRequest;
import com.radioacademy.backend.entity.Lesson;
import com.radioacademy.backend.entity.Module;
import com.radioacademy.backend.repository.LessonRepository;
import com.radioacademy.backend.repository.ModuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/lessons")
public class LessonController {

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    // GET: Obtener lecciones de un módulo
    @GetMapping("/module/{moduleId}")
    public ResponseEntity<List<Lesson>> getLessonsByModule(@PathVariable UUID moduleId) {
        List<Lesson> lessons = lessonRepository.findByModuleIdOrderByOrderIndexAsc(moduleId);
        return ResponseEntity.ok(lessons);
    }

    // POST: Crear lección
    @PostMapping
    public ResponseEntity<Lesson> createLesson(@RequestBody CreateLessonRequest request) {
        Optional<Module> moduleOptional = moduleRepository.findById(request.moduleId());

        if (moduleOptional.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Lesson newLesson = new Lesson();
        newLesson.setTitle(request.title());
        newLesson.setVideoUrl(request.videoUrl());
        newLesson.setPdfUrl(request.pdfUrl());
        newLesson.setOrderIndex(request.orderIndex());
        newLesson.setModule(moduleOptional.get());

        Lesson savedLesson = lessonRepository.save(newLesson);
        return new ResponseEntity<>(savedLesson, HttpStatus.CREATED);
    }
}