package com.radioacademy.backend.security;

import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.enums.Role;
import com.radioacademy.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void loadUserByUsername_ShouldReturnUserDetails_WhenUserExists() {
        User user = new User();
        user.setEmail("test@test.com");
        user.setPassword("password");
        user.setRole(Role.STUDENT);

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("test@test.com");

        assertNotNull(userDetails);
        assertEquals("test@test.com", userDetails.getUsername());
        assertEquals("password", userDetails.getPassword());

        // Verify Role Mapping
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("STUDENT")));
    }

    @Test
    void loadUserByUsername_ShouldThrow_WhenUserNotFound() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("unknown@test.com"));
    }

    @Test
    void loadUserByUsername_ShouldMapAdminRoleCorrectly() {
        User user = new User();
        user.setEmail("admin@test.com");
        user.setPassword("adminpass");
        user.setRole(Role.ADMIN);

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("admin@test.com");

        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN")));
    }
}
