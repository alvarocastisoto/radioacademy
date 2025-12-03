package com.radioacademy.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.radioacademy.backend.entity.User;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByDni(String dni);
}
