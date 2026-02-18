package com.radioacademy.backend.repository;

import com.radioacademy.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByDni(String dni);

    @Modifying 
    @Transactional 
    @Query("UPDATE User u SET u.name = :name, u.surname = :surname, u.phone = :phone, u.email = :email, u.password = :password WHERE u.id = :id")
    void updateProfileDirectly(UUID id, String name, String surname, String phone, String email, String password);

    @Modifying
    @Transactional 
    @Query("UPDATE User u SET u.name = :name, u.surname = :surname, u.phone = :phone, u.email = :email, u.password = :password, u.avatar = :avatar WHERE u.id = :id")
    void updateProfileDirectly(
            @Param("id") UUID id,
            @Param("name") String name,
            @Param("surname") String surname,
            @Param("phone") String phone,
            @Param("email") String email,
            @Param("password") String password,
            @Param("avatar") String avatar 
    );

}