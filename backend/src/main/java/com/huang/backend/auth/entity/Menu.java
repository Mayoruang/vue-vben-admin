package com.huang.backend.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "t_menu")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Menu {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pid")
    private Menu parent;
    
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private Set<Menu> children = new HashSet<>();
    
    @Column(nullable = false)
    private String name;
    
    private String path;
    
    private String component;
    
    private String redirect;
    
    @Column(name = "auth_code")
    private String authCode;
    
    @Column(nullable = false)
    private String type;
    
    private String icon;
    
    @Column(nullable = false)
    private Integer status = 1;
    
    @Column(columnDefinition = "jsonb")
    private String meta;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @ManyToMany
    @JoinTable(
        name = "t_role_menu",
        joinColumns = @JoinColumn(name = "menu_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
} 