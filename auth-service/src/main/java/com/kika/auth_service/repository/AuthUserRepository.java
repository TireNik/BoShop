package com.kika.auth_service.repository;

import com.kika.auth_service.entity.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface AuthUserRepository extends JpaRepository<AuthUser, Long>, JpaSpecificationExecutor<AuthUser> {
    boolean existsByEmail(String email);

    Optional<AuthUser> findByEmail(String email);
}