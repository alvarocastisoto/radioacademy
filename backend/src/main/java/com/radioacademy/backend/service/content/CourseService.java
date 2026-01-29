package com.radioacademy.backend.service.content;

import com.radioacademy.backend.dto.course.CourseDTO;
import com.radioacademy.backend.dto.course.CourseDashboardDTO;
import com.radioacademy.backend.dto.course.CourseDetailDTO;
import com.radioacademy.backend.dto.course.CreateCourseRequest;
import com.radioacademy.backend.entity.Course;
import com.radioacademy.backend.entity.Enrollment;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.repository.CourseRepository;
import com.radioacademy.backend.repository.EnrollmentRepository;
import com.radioacademy.backend.repository.UserRepository;
import com.radioacademy.backend.security.CustomUserDetails;
import com.radioacademy.backend.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StorageService storageService; // Inyectamos servicio de ficheros

    // 1. OBTENER CURSOS (CATÁLOGO PÚBLICO)
    @Transactional(readOnly = true)
    public List<CourseDTO> getAllCourses(Authentication authentication) {
        List<Course> courses = courseRepository.findAll();
        Set<UUID> purchasedCourseIds = new HashSet<>();

        // Lógica: Si está logueado, buscamos qué ha comprado
        if (authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser")) {

            userRepository.findByEmail(authentication.getName()).ifPresent(user -> {
                enrollmentRepository.findByUserId(user.getId())
                        .forEach(enrollment -> purchasedCourseIds.add(enrollment.getCourse().getId()));
            });
        }

        return courses.stream().map(course -> new CourseDTO(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getCoverImage(),
                course.getPrice(),
                course.getHours(),
                purchasedCourseIds.contains(course.getId()) // true/false
        )).toList();
    }

    // 2. CREAR CURSO
    @Transactional
    public CourseDetailDTO createCourse(CreateCourseRequest request, CustomUserDetails userDetails) {

        Course newCourse = new Course();
        newCourse.setTitle(request.title());
        newCourse.setDescription(request.description());
        newCourse.setPrice(request.price());
        newCourse.setHours(request.hours());
        newCourse.setActive(true);
        newCourse.setTeacher(userRepository.getReferenceById(userDetails.getId()));
        newCourse.setCoverImage(request.coverImage());

        Course savedCourse = courseRepository.save(newCourse);
        return mapToDetailDTO(savedCourse);
    }

    // 3. ACTUALIZAR CURSO
    @Transactional
    public CourseDetailDTO updateCourse(UUID id, CreateCourseRequest request) {
        Course existingCourse = courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado"));

        // Lógica de borrado de imagen antigua (Clean Code)
        if (request.coverImage() != null && !request.coverImage().equals(existingCourse.getCoverImage())) {
            deleteImageFile(existingCourse.getCoverImage());
            existingCourse.setCoverImage(request.coverImage());
        }

        existingCourse.setTitle(request.title());
        existingCourse.setDescription(request.description());
        existingCourse.setPrice(request.price());
        existingCourse.setHours(request.hours());

        Course updatedCourse = courseRepository.save(existingCourse);
        return mapToDetailDTO(updatedCourse);
    }

    // 4. BORRAR CURSO
    @Transactional
    public void deleteCourse(UUID id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado"));

        // Borrar imagen física
        deleteImageFile(course.getCoverImage());

        courseRepository.delete(course);
    }

    // 5. OBTENER MIS CURSOS (ALUMNO)
    @Transactional(readOnly = true)
    public List<CourseDashboardDTO> getMyCourses(CustomUserDetails userDetails) {
        List<Enrollment> enrollments = enrollmentRepository.findByUserId(userDetails.getId());

        return enrollments.stream().map(enrollment -> {
            Course course = enrollment.getCourse();
            // TODO: Conectar con ProgressRepository real cuando lo tengas aquí inyectado
            int progress = 0;

            return new CourseDashboardDTO(
                    course.getId(),
                    course.getTitle(),
                    course.getDescription(),
                    course.getCoverImage(),
                    progress);
        }).toList();
    }

    @Transactional(readOnly = true)
    public CourseDetailDTO getCourseById(UUID id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado."));

        return mapToDetailDTO(course);
    }
    // --- Helpers Privados (Limpian el código principal) ---

    private CourseDetailDTO mapToDetailDTO(Course course) {
        return new CourseDetailDTO(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getCoverImage(),
                course.getPrice(),
                course.getHours(),
                course.getActive());
    }

    private void deleteImageFile(String imagePathOrUrl) {
        if (imagePathOrUrl == null || imagePathOrUrl.isBlank())
            return;
        storageService.delete(imagePathOrUrl);
    }

}