package com.huang.backend.repository;

import com.huang.backend.model.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {
    List<Menu> findByParentIdOrderByMeta_OrderNumAsc(Long parentId);
    
    @Query("SELECT DISTINCT m FROM Menu m JOIN m.roles r JOIN r.users u WHERE u.username = ?1 AND m.status = 1 ORDER BY m.meta.orderNum ASC")
    List<Menu> findAllMenusByUsername(String username);
} 