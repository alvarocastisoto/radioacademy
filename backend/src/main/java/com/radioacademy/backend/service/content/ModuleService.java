package com.radioacademy.backend.service.content;

import com.radioacademy.backend.dto.module.CreateModuleRequest;
import com.radioacademy.backend.dto.module.ModuleRequest;
import com.radioacademy.backend.entity.Course;
import com.radioacademy.backend.entity.Module;
import com.radioacademy.backend.repository.CourseRepository;
import com.radioacademy.backend.repository.ModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ModuleService {

    private final ModuleRepository moduleRepository;
    private final CourseRepository courseRepository;

    // 1. OBTENER MÓDULOS POR CURSO
    public List<ModuleRequest> getModulesByCourse(UUID courseId) {
        return moduleRepository.findByCourseIdOrderByOrderIndexAsc(courseId)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    // 2. CREAR MÓDULO
    @Transactional
    public ModuleRequest createModule(CreateModuleRequest request) {
        // Buscamos el curso padre
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El curso no existe"));

        Module newModule = new Module();
        newModule.setTitle(request.title());
        newModule.setOrderIndex(request.orderIndex());
        newModule.setCourse(course);

        Module savedModule = moduleRepository.save(newModule);

        return mapToDTO(savedModule);
    }

    // 3. ELIMINAR MÓDULO
    @Transactional
    public void deleteModule(UUID id) {
        if (!moduleRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El módulo no existe");
        }
        moduleRepository.deleteById(id);
    }

    // --- Helper Privado ---
    private ModuleRequest mapToDTO(Module module) {
        return new ModuleRequest(
                module.getId(),
                module.getTitle(),
                module.getOrderIndex());
    }
}