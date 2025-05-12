package com.huang.backend.repository;

import com.huang.backend.model.PermissionCode;
import com.huang.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionCodeRepository extends JpaRepository<PermissionCode, Long> {
    Optional<PermissionCode> findByCode(String code);
    
    @Query("SELECT pc.code FROM PermissionCode pc JOIN pc.roles r JOIN r.users u WHERE u = ?1")
    List<String> findCodesByUser(User user);

    @Query("SELECT DISTINCT pc.code FROM PermissionCode pc JOIN pc.users u WHERE u = ?1")
    List<String> findDirectCodesByUser(User user);
} 