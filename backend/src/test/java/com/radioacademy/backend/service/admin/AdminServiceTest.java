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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    void getAllUsers_ShouldReturnList() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName("John");
        user.setSurname("Doe");
        user.setRole(Role.STUDENT);

        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserListDTO> result = adminService.getAllUsers();

        assertEquals(1, result.size());
        assertEquals("John Doe", result.get(0).fullName());
    }

    @Test
    void getCoursesForDropdown_ShouldReturnCourses() {
        Course course = new Course();
        course.setId(UUID.randomUUID());
        course.setTitle("Java Basics");

        when(courseRepository.findAll()).thenReturn(List.of(course));

        List<CourseDropdownDTO> result = adminService.getCoursesForDropdown();

        assertEquals(1, result.size());
        assertEquals("Java Basics", result.get(0).title());
    }

    @Test
    void enrollUser_ShouldEnroll_WhenNotEnrolled() {
        UUID userId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        when(enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(new Course()));

        adminService.enrollUser(userId, courseId);

        verify(enrollmentRepository).save(any(Enrollment.class));
    }

    @Test
    void enrollUser_ShouldThrow_WhenAlreadyEnrolled() {
        UUID userId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        when(enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> adminService.enrollUser(userId, courseId));
    }

    @Test
    void unenrollUser_ShouldDelete_WhenEnrolled() {
        UUID userId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        Enrollment enrollment = new Enrollment();

        when(enrollmentRepository.findByUserIdAndCourseId(userId, courseId)).thenReturn(Optional.of(enrollment));

        adminService.unenrollUser(userId, courseId);

        verify(enrollmentRepository).delete(enrollment);
    }

    @Test
    void unenrollUser_ShouldThrow_WhenNotEnrolled() {
        UUID userId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        when(enrollmentRepository.findByUserIdAndCourseId(userId, courseId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> adminService.unenrollUser(userId, courseId));
    }

    @Test
    void changeUserRole_ShouldUpdateRole() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setRole(Role.STUDENT);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        adminService.changeUserRole(userId, "ADMIN");

        assertEquals(Role.ADMIN, user.getRole());
        verify(userRepository).save(user);
    }

    @Test
    void changeUserRole_ShouldThrow_WhenInvalidRole() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));

        assertThrows(IllegalArgumentException.class,
                () -> adminService.changeUserRole(userId, "SUPER_GOD_MODE"));
    }
}
