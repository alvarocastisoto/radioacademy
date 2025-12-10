package com.radioacademy.backend.controller;

import com.radioacademy.backend.dto.CreateModuleRequest;
import com.radioacademy.backend.entity.Course;
import com.radioacademy.backend.entity.Module;
import com.radioacademy.backend.repository.CourseRepository;
import com.radioacademy.backend.repository.ModuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/modules")
public class ModuleController {

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private CourseRepository courseRepository;

    // GET: Obtener todos los módulos de un curso específico
    // Ejemplo URL: /api/modules/course/a0eebc99...
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<Module>> getModulesByCourse(@PathVariable UUID courseId) {
        List<Module> modules = moduleRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        return ResponseEntity.ok(modules);
    }

    // POST: Crear un módulo nuevo
    @PostMapping
    public ResponseEntity<Module> createModule(@RequestBody CreateModuleRequest request) {
        // 1. Buscamos el curso padre
        Optional<Course> courseOptional = courseRepository.findById(request.courseId());

        if (courseOptional.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        // 2. Creamos el módulo
        Module newModule = new Module();
        newModule.setTitle(request.title());
        newModule.setOrderIndex(request.orderIndex());
        newModule.setCourse(courseOptional.get()); // Asignamos la relación

        Module savedModule = moduleRepository.save(newModule);
        return new ResponseEntity<>(savedModule, HttpStatus.CREATED);
    }
}