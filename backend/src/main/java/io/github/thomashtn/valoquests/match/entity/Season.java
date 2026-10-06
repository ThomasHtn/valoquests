package io.github.thomashtn.valoquests.match.entity;

import io.github.thomashtn.valoquests.shared.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Stores a Valorant season referenced by imported matches.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "season")
public class Season extends AuditableEntity {

    /**
     * Internal database identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Stable season identifier returned by Henrik.
     */
    @Column(name = "external_id", nullable = false, unique = true, length = 64)
    private String externalId;

    /**
     * Human-readable season name.
     */
    @Column(nullable = false, length = 100)
    private String name;
}
