package io.github.thomashtn.valorant.tracker.player.entity;

import io.github.thomashtn.valorant.tracker.player.model.*;
import io.github.thomashtn.valorant.tracker.shared.entity.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

/**
 * Represents the player component.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "player")
public class Player extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "riot_puuid", unique = true, length = 100)
    private String riotPuuid;

    @Column(name = "game_name", nullable = false, length = 32)
    private String gameName;

    @Column(name = "tag_line", nullable = false, length = 16)
    private String tagLine;

    @Column(name = "display_name", nullable = false, length = 64)
    private String displayName;

    @Column(length = 255)
    private String portrait;

    @Enumerated(EnumType.STRING)
    @Column(name = "competitive_tier", nullable = false, length = 32)
    private CompetitiveTier competitiveTier = CompetitiveTier.UNRANKED;

    @Column(name = "rank_rating")
    private Integer rankRating;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PlayerStatus status = PlayerStatus.ACTIVE;

    @Column(name = "last_successful_synchronization_at")
    private Instant lastSuccessfulSynchronizationAt;
}
