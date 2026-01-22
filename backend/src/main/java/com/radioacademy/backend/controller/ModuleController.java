package com.radioacademy.backend.controller;

import com.radioacademy.backend.dto.module.CreateModuleRequest;
import com.radioacademy.backend.dto.module.ModuleRequest;
import com.radioacademy.backend.entity.Course;
import com.radioacademy.backend.entity.Module;
import com.radioacademy.backend.repository.CourseRepository;
import com.radioacademy.backend.repository.ModuleRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/modules")
public class ModuleController {

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private CourseRepository courseRepository;

    // GET: Obtener todos los módulos (Devuelve DTOs)
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<ModuleRequest>> getModulesByCourse(@PathVariable UUID courseId) {

        List<Module> modules = moduleRepository.findByCourseIdOrderByOrderIndexAsc(courseId);

        // 🔄 CONVERSIÓN: Entity -> DTO
        List<ModuleRequest> response = modules.stream()
                .map(module -> new ModuleRequest(
                        module.getId(),
                        module.getTitle(),
                        module.getOrderIndex()))
                .toList();

        return ResponseEntity.ok(response);
    }

    // POST: Crear un módulo nuevo
    // Recibe: CreateModuleRequest (Input con courseId)
    // Devuelve: ModuleRequest (Output con ID generado)
    @PostMapping
    public ResponseEntity<ModuleRequest> createModule(@Valid @RequestBody CreateModuleRequest request) {

        // 1. Buscamos el curso padre
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El curso no existe"));

        // 2. Creamos la entidad
        Module newModule = new Module();
        newModule.setTitle(request.title());
        newModule.setOrderIndex(request.orderIndex());
        newModule.setCourse(course);

        // 3. Guardamos
        Module savedModule = moduleRepository.save(newModule);

        // 4. 🔄 CONVERSIÓN: Preparamos la respuesta DTO
        ModuleRequest responseDTO = new ModuleRequest(
                savedModule.getId(),
                savedModule.getTitle(),
                savedModule.getOrderIndex());

        return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
    }

    // DELETE: Eliminar un módulo
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteModule(@PathVariable UUID id) {
        if (!moduleRepository.existsById(id)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        moduleRepository.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}