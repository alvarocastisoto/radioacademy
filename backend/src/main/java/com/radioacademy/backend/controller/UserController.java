package com.radioacademy.backend.controller;

import com.radioacademy.backend.dto.student.UserProfileDTO;
import com.radioacademy.backend.dto.student.UserProfileResponseDTO;
import com.radioacademy.backend.entity.User;
import com.radioacademy.backend.security.CustomUserDetails;
import com.radioacademy.backend.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    

    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    
    
    
    
    
    

    
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponseDTO> getMyProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(userService.getMyProfile(userDetails));
    }

    
    @PutMapping("/profile")
    public ResponseEntity<Map<String, String>> updateProfile(
            @Valid @RequestBody UserProfileDTO profileData,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        
        return ResponseEntity.ok(userService.updateProfile(userDetails, profileData));
    }
}