package com.radioacademy.backend.service.admin;

import com.radioacademy.backend.dto.course.CourseDropdownDTO;
import com.radioacademy.backend.dto.student.UserListDTO;
import com.radioacademy.backend.entity.Course;
import com.radioacademy.backend.entity.Enrollment;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.enums.Role;
import com.radioacademy.backend.repository.CourseRepository;
import com.radioacademy.backend.repository.EnrollmentRepository;
import com.radioacademy.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private EnrollmentRepository enrollmentRepository;

    // 1. Obtener todos los usuarios mapeados
    public List<UserListDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(u -> new UserListDTO(
                        u.getId(),
                        u.getName() + " " + u.getSurname(),
                        u.getEmail(),
                        u.getDni(),
                        u.getRole().toString(),
                        u.isEnabled()))
                .toList();
    }

    // 2. Dropdown de cursos
    public List<CourseDropdownDTO> getCoursesForDropdown() {
        return courseRepository.findAll().stream()
                .map(c -> new CourseDropdownDTO(c.getId(), c.getTitle()))
                .toList();
    }

    // 3. Matricular (Lógica pura)
    @Transactional
    public void enrollUser(UUID userId, UUID courseId) {
        // Validaciones
        if (enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new IllegalArgumentException("Este usuario YA está matriculado.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Curso no encontrado"));

        // Crear matrícula
        Enrollment enrollment = new Enrollment();
        enrollment.setUser(user);
        enrollment.setCourse(course);
        enrollment.setAmountPaid(java.math.BigDecimal.ZERO);
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollment.setPaymentId("MANUAL_ADMIN");

        enrollmentRepository.save(enrollment);
    }

    // 4. Desmatricular
    @Transactional
    public void unenrollUser(UUID userId, UUID courseId) {
        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new EntityNotFoundException("No existe matrícula para este usuario y curso."));

        enrollmentRepository.delete(enrollment);
    }

    // 5. Cursos de un usuario
    public List<CourseDropdownDTO> getUserCourses(UUID userId) {
        return enrollmentRepository.findByUserId(userId).stream()
                .map(e -> new CourseDropdownDTO(e.getCourse().getId(), e.getCourse().getTitle()))
                .toList();
    }

    // 6. Cambiar Rol
    @Transactional
    public void changeUserRole(UUID userId, String newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        try {
            user.setRole(Role.valueOf(newRole));
            userRepository.save(user);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Rol no válido: " + newRole);
        }
    }
}