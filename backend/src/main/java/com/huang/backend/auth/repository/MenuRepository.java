package com.huang.backend.auth.repository;

import com.huang.backend.auth.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Integer> {
    List<Menu> findByParentIsNull();
    
    @Query("SELECT DISTINCT m FROM Menu m JOIN m.roles r JOIN r.users u WHERE u.username = :username AND m.status = 1")
    List<Menu> findUserMenus(String username);
} 