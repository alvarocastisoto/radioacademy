package com.radioacademy.backend.controller;

import com.radioacademy.backend.dto.module.CreateModuleRequest;
import com.radioacademy.backend.dto.module.ModuleRequest;
import com.radioacademy.backend.service.content.ModuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/modules")
@RequiredArgsConstructor 
public class ModuleController {

    private final ModuleService moduleService;

    
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<ModuleRequest>> getModulesByCourse(@PathVariable UUID courseId) {
        return ResponseEntity.ok(moduleService.getModulesByCourse(courseId));
    }

    
    @PostMapping
    public ResponseEntity<ModuleRequest> createModule(@Valid @RequestBody CreateModuleRequest request) {
        return new ResponseEntity<>(moduleService.createModule(request), HttpStatus.CREATED);
    }

    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteModule(@PathVariable UUID id) {
        moduleService.deleteModule(id);
        return ResponseEntity.noContent().build();
    }
}