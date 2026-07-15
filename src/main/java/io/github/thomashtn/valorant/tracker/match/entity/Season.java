package io.github.thomashtn.valorant.tracker.match.entity;

import io.github.thomashtn.valorant.tracker.shared.entity.AuditableEntity;
import jakarta.persistence.*;
import java.time.*;
import lombok.*;

/**
 * Represents the season component.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "season")
public class Season extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true, length = 64)
    private String externalId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(nullable = false)
    private boolean active;
}
