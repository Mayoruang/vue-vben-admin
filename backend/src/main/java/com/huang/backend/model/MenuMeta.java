package com.huang.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "menu_meta")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuMeta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    @Column
    private String title;

    @Column
    private String icon;

    @Column(name = "order_num")
    private Integer orderNum;

    @Column(name = "keep_alive")
    private Boolean keepAlive = false;

    @Column
    private Boolean hidden = false;

    @Column(name = "affix_tab")
    private Boolean affixTab = false;

    @Column
    private String badge;

    @Column(name = "badge_type")
    private String badgeType;

    @Column(name = "badge_variants")
    private String badgeVariants;

    @Column
    private String link;

    @Column(name = "iframe_src")
    private String iframeSrc;

    @Column
    private String authority;

    @Column(name = "menu_visible_with_forbidden")
    private Boolean menuVisibleWithForbidden = false;

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
        MenuMeta menuMeta = (MenuMeta) o;
        return Objects.equals(id, menuMeta.id) &&
               Objects.equals(title, menuMeta.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title);
    }
} 