package com.radioacademy.backend.repository;

import com.radioacademy.backend.entity.PasswordResetToken;
import com.radioacademy.backend.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findByUser_Id(UUID user_Id);

    void deleteByUserId(UUID userId); // Para borrar tokens viejos
}