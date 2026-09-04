package io.github.thomashtn.valoquests.campaign.entity;

import io.github.thomashtn.valoquests.campaign.model.GuardianCategory;
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
 * One entry of the guardian catalogue.
 *
 * <p>A campaign draws ten of these at opening, two minor, six standard and two elite, and points at
 * the rows it drew. Renaming an entry later therefore renames it everywhere, including on campaigns
 * already closed, which is what a catalogue is for.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "guardian")
public class Guardian extends AuditableEntity {

    /**
     * Internal database identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Stable functional code, unique across the catalogue.
     */
    @Column(nullable = false, length = 80, unique = true)
    private String code;

    /**
     * Display name.
     */
    @Column(nullable = false, length = 120)
    private String name;

    /**
     * One-line description shown next to the week's planet.
     */
    @Column(nullable = false, length = 500)
    private String description;

    /**
     * Weight class the entry may be drawn for.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GuardianCategory category;

    /**
     * Whether the entry may still be drawn.
     */
    @Column(nullable = false)
    private boolean enabled = true;
}
