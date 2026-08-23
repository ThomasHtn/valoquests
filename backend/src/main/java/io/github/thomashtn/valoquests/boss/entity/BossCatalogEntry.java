package io.github.thomashtn.valoquests.boss.entity;

import io.github.thomashtn.valoquests.scoring.model.BossCategory;
import io.github.thomashtn.valoquests.shared.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Defines one reusable boss from the weekly-boss catalogue.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "boss_catalog_entry")
public class BossCatalogEntry extends AuditableEntity {

    /**
     * Internal database identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Stable application code used by migrations and deterministic selection.
     */
    @Column(nullable = false, unique = true, length = 80)
    private String code;

    /**
     * Human-readable boss name.
     */
    @Column(nullable = false, length = 120)
    private String name;

    /**
     * User-facing boss description.
     */
    @Column(nullable = false, length = 500)
    private String description;

    /**
     * Visual asset reference, left {@code null} until real artwork replaces the provisional catalogue.
     */
    @Column(name = "image_url", length = 300)
    private String imageUrl;

    /**
     * Weight class governing the boss's base hit points.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BossCategory category;

    /**
     * Whether the boss is eligible for future weekly selection.
     */
    @Column(nullable = false)
    private boolean enabled = true;
}
