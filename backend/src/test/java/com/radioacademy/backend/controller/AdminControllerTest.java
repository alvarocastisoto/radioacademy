package com.radioacademy.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.radioacademy.backend.dto.course.CourseDropdownDTO;
import com.radioacademy.backend.dto.student.UserListDTO;
import com.radioacademy.backend.service.admin.AdminService;
import com.radioacademy.backend.security.JwtAuthenticationFilter;
import com.radioacademy.backend.security.JwtService;
import com.radioacademy.backend.security.CustomUserDetailsService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminService adminService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAllUsers_ShouldReturnUserList() throws Exception {
        
        UUID userId = UUID.randomUUID();
        UserListDTO user = new UserListDTO(userId, "John Doe", "john@test.com", "12345678Z", "STUDENT", true);
        List<UserListDTO> users = Arrays.asList(user);

        when(adminService.getAllUsers()).thenReturn(users);

        
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("John Doe"))
                .andExpect(jsonPath("$[0].email").value("john@test.com"));
    }

    @Test
    void getCoursesForDropdown_ShouldReturnCourseList() throws Exception {
        
        UUID courseId = UUID.randomUUID();
        CourseDropdownDTO course = new CourseDropdownDTO(courseId, "Course 1");
        List<CourseDropdownDTO> courses = Arrays.asList(course);

        when(adminService.getCoursesForDropdown()).thenReturn(courses);

        
        mockMvc.perform(get("/api/admin/courses-dropdown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Course 1"));
    }

    @Test
    void enrollUser_ShouldReturnSuccess() throws Exception {
        
        UUID userId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        doNothing().when(adminService).enrollUser(userId, courseId);

        
        mockMvc.perform(post("/api/admin/enroll")
                .param("userId", userId.toString())
                .param("courseId", courseId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Usuario matriculado correctamente"));

        verify(adminService).enrollUser(userId, courseId);
    }

    @Test
    void enrollUser_ShouldReturnNotFound_WhenUserNotExists() throws Exception {
        
        UUID userId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        doThrow(new EntityNotFoundException("Usuario no encontrado"))
                .when(adminService).enrollUser(userId, courseId);

        
        mockMvc.perform(post("/api/admin/enroll")
                .param("userId", userId.toString())
                .param("courseId", courseId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Usuario no encontrado"));
    }

    @Test
    void enrollUser_ShouldReturnConflict_WhenAlreadyEnrolled() throws Exception {
        
        UUID userId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        doThrow(new IllegalArgumentException("Usuario ya matriculado"))
                .when(adminService).enrollUser(userId, courseId);

        
        mockMvc.perform(post("/api/admin/enroll")
                .param("userId", userId.toString())
                .param("courseId", courseId.toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Usuario ya matriculado"));
    }

    @Test
    void unenrollUser_ShouldReturnSuccess() throws Exception {
        
        UUID userId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        doNothing().when(adminService).unenrollUser(userId, courseId);

        
        mockMvc.perform(post("/api/admin/unenroll")
                .param("userId", userId.toString())
                .param("courseId", courseId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Matrícula cancelada correctamente"));

        verify(adminService).unenrollUser(userId, courseId);
    }

    @Test
    void unenrollUser_ShouldReturnNotFound_WhenUserNotExists() throws Exception {
        
        UUID userId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        doThrow(new EntityNotFoundException("Usuario no encontrado"))
                .when(adminService).unenrollUser(userId, courseId);

        
        mockMvc.perform(post("/api/admin/unenroll")
                .param("userId", userId.toString())
                .param("courseId", courseId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Usuario no encontrado"));
    }

    @Test
    void getUserCourses_ShouldReturnCourseList() throws Exception {
        
        UUID userId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        CourseDropdownDTO course = new CourseDropdownDTO(courseId, "User Course");
        List<CourseDropdownDTO> courses = Arrays.asList(course);

        when(adminService.getUserCourses(userId)).thenReturn(courses);

        
        mockMvc.perform(get("/api/admin/users/{userId}/courses", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("User Course"));
    }

    @Test
    void changeUserRole_ShouldReturnSuccess() throws Exception {
        
        UUID userId = UUID.randomUUID();
        doNothing().when(adminService).changeUserRole(userId, "ADMIN");

        
        mockMvc.perform(put("/api/admin/users/{userId}/role", userId)
                .param("newRole", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Rol actualizado"));

        verify(adminService).changeUserRole(userId, "ADMIN");
    }

    @Test
    void changeUserRole_ShouldReturnNotFound_WhenUserNotExists() throws Exception {
        
        UUID userId = UUID.randomUUID();
        doThrow(new EntityNotFoundException("Usuario no encontrado"))
                .when(adminService).changeUserRole(userId, "ADMIN");

        
        mockMvc.perform(put("/api/admin/users/{userId}/role", userId)
                .param("newRole", "ADMIN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Usuario no encontrado"));
    }

    @Test
    void changeUserRole_ShouldReturnBadRequest_WhenRoleInvalid() throws Exception {
        
        UUID userId = UUID.randomUUID();
        doThrow(new IllegalArgumentException("Rol inválido"))
                .when(adminService).changeUserRole(userId, "INVALID_ROLE");

        
        mockMvc.perform(put("/api/admin/users/{userId}/role", userId)
                .param("newRole", "INVALID_ROLE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Rol inválido"));
    }
}
