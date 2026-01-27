package com.radioacademy.backend.service.user;

import com.radioacademy.backend.dto.student.UserProfileDTO;
import com.radioacademy.backend.dto.student.UserProfileResponseDTO;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.enums.Role;
import com.radioacademy.backend.repository.UserRepository;
import com.radioacademy.backend.service.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private StorageService storageService;

    @InjectMocks
    private UserService userService;

    @Test
    void getMyProfile_ShouldReturnData_WhenUserExists() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@test.com");
        user.setRole(Role.STUDENT);

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        UserProfileResponseDTO profile = userService.getMyProfile("test@test.com");

        assertNotNull(profile);
        assertEquals("test@test.com", profile.email());
        assertEquals("STUDENT", profile.role());
    }

    @Test
    void updateProfile_ShouldUpdateBasicInfo() {
        // Arrange
        User user = new User();
        user.setEmail("test@test.com");

        UserProfileDTO request = new UserProfileDTO(null, "NewName", "NewSurname", "test@test.com", "666666666", null,
                null, null);

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        // Act
        userService.updateProfile("test@test.com", request);

        // Assert
        assertEquals("NewName", user.getName());
        assertEquals("NewSurname", user.getSurname());
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_ShouldThrow_WhenEmailTaken() {
        User user = new User();
        user.setEmail("old@test.com");

        UserProfileDTO request = new UserProfileDTO(null, "Name", "Surname", "taken@test.com", "123", null, null, null);

        when(userRepository.findByEmail("old@test.com")).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("taken@test.com")).thenReturn(true);

        assertThrows(ResponseStatusException.class,
                () -> userService.updateProfile("old@test.com", request));
    }

    @Test
    void updateProfile_ShouldChangePassword_WhenValid() {
        User user = new User();
        user.setEmail("test@test.com");
        user.setPassword("oldEncoded");

        String strongPass = "NewStrongPass1!";
        UserProfileDTO request = new UserProfileDTO(null, "Name", "Surname", "test@test.com", "123", "oldRaw",
                strongPass, null);

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldRaw", "oldEncoded")).thenReturn(true);
        when(passwordEncoder.encode(strongPass)).thenReturn("newEncoded");

        userService.updateProfile("test@test.com", request);

        assertEquals("newEncoded", user.getPassword());
    }

    @Test
    void updateProfile_ShouldThrow_WhenPasswordWeak() {
        User user = new User();
        user.setEmail("test@test.com");

        UserProfileDTO request = new UserProfileDTO(null, "Name", "Surname", "test@test.com", "123", "old", "weak",
                null);

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        assertThrows(ResponseStatusException.class,
                () -> userService.updateProfile("test@test.com", request));
    }

    @Test
    void updateProfile_ShouldRotateAvatar() {
        User user = new User();
        user.setEmail("test@test.com");
        user.setAvatar("old_avatar.jpg");

        UserProfileDTO request = new UserProfileDTO(null, "Name", "Surname", "test@test.com", "123", null, null,
                "new_avatar.jpg");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

        userService.updateProfile("test@test.com", request);

        assertEquals("new_avatar.jpg", user.getAvatar());
        verify(storageService).delete("old_avatar.jpg");
    }
}
