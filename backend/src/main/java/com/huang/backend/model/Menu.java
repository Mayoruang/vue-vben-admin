package com.huang.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "menus")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_id")
    private Long parentId = 0L;

    @Column(nullable = false, length = 20)
    private String type; // 'menu', 'catalog', 'button', 'embedded', 'link'

    @Column(nullable = false, length = 100)
    private String name;

    @Column
    private String path;

    @Column
    private String component;

    @Column
    private String redirect;

    @Column(name = "auth_code")
    private String authCode;

    @Column
    private Integer status = 1; // 1-启用, 0-禁用

    @OneToOne(mappedBy = "menu", cascade = CascadeType.ALL, orphanRemoval = true)
    private MenuMeta meta;

    @ManyToMany(mappedBy = "menus")
    private Set<Role> roles = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Menu menu = (Menu) o;
        return Objects.equals(id, menu.id) &&
               Objects.equals(name, menu.name) &&
               Objects.equals(path, menu.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, path);
    }
} 