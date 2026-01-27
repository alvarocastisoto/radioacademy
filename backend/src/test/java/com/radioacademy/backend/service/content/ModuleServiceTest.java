package com.radioacademy.backend.service.content;

import com.radioacademy.backend.dto.module.CreateModuleRequest;
import com.radioacademy.backend.dto.module.ModuleRequest;
import com.radioacademy.backend.entity.Course;
import com.radioacademy.backend.entity.Module;
import com.radioacademy.backend.repository.CourseRepository;
import com.radioacademy.backend.repository.ModuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModuleServiceTest {

    @Mock
    private ModuleRepository moduleRepository;
    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private ModuleService moduleService;

    @Test
    void getModulesByCourse_ShouldReturnOrderedList() {
        UUID courseId = UUID.randomUUID();
        Module module = new Module();
        module.setId(UUID.randomUUID());
        module.setTitle("Module 1");
        module.setOrderIndex(1);

        when(moduleRepository.findByCourseIdOrderByOrderIndexAsc(courseId)).thenReturn(List.of(module));

        List<ModuleRequest> result = moduleService.getModulesByCourse(courseId);

        assertEquals(1, result.size());
        assertEquals("Module 1", result.get(0).title());
    }

    @Test
    void createModule_ShouldThrow_WhenCourseNotFound() {
        CreateModuleRequest request = new CreateModuleRequest("Title", 1, UUID.randomUUID());
        when(courseRepository.findById(any())).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> moduleService.createModule(request));

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void createModule_ShouldSave_WhenValid() {
        UUID courseId = UUID.randomUUID();
        CreateModuleRequest request = new CreateModuleRequest("New Module", 1, courseId);

        Course course = new Course();
        course.setId(courseId);

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(moduleRepository.save(any(Module.class))).thenAnswer(i -> {
            Module m = i.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });

        ModuleRequest response = moduleService.createModule(request);

        assertNotNull(response);
        assertEquals("New Module", response.title());
        assertEquals(1, response.orderIndex());
    }

    @Test
    void deleteModule_ShouldDelete_WhenExists() {
        UUID moduleId = UUID.randomUUID();
        when(moduleRepository.existsById(moduleId)).thenReturn(true);

        moduleService.deleteModule(moduleId);

        verify(moduleRepository).deleteById(moduleId);
    }

    @Test
    void deleteModule_ShouldThrow_WhenNotFound() {
        UUID moduleId = UUID.randomUUID();
        when(moduleRepository.existsById(moduleId)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> moduleService.deleteModule(moduleId));

        assertEquals(404, ex.getStatusCode().value());
    }
}
