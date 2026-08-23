package io.github.thomashtn.valoquests.player.entity;

import io.github.thomashtn.valoquests.player.model.CompetitiveTier;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.shared.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents one Valorant account tracked by the application.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "player")
public class Player extends AuditableEntity {

    /**
     * Internal database identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Stable Riot account identifier resolved through Henrik.
     */
    @Column(name = "riot_puuid", unique = true, length = 100)
    private String riotPuuid;

    /**
     * Riot game name, excluding the tag line.
     */
    @Column(name = "game_name", nullable = false, length = 32)
    private String gameName;

    /**
     * Riot tag line, excluding the leading hash character.
     */
    @Column(name = "tag_line", nullable = false, length = 16)
    private String tagLine;

    /**
     * Name displayed by the frontend.
     */
    @Column(name = "display_name", nullable = false, length = 64)
    private String displayName;

    /**
     * Relative path or URL of the player portrait.
     */
    @Column(length = 255)
    private String portrait;

    /**
     * Current competitive tier returned by Henrik.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "competitive_tier", nullable = false, length = 32)
    private CompetitiveTier competitiveTier = CompetitiveTier.UNRANKED;

    /**
     * Current rank rating inside the competitive tier.
     */
    @Column(name = "rank_rating")
    private Integer rankRating;

    /**
     * Lifecycle status controlling synchronization eligibility.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PlayerStatus status = PlayerStatus.ACTIVE;

    /**
     * Completion time of the latest successful synchronization.
     */
    @Column(name = "last_successful_synchronization_at")
    private Instant lastSuccessfulSynchronizationAt;

    /**
     * Whether this player takes part in weekly challenge resolution, boss combat and ranking
     * positions.
     *
     * <p>Derived from {@link #status}: an inactive player is still synchronized normally and
     * still gets a weekly score for display, it just never contributes to boss damage and never
     * consumes a ranking slot. It can still complete challenges individually, though.
     *
     * @return {@code true} when this player's status is {@link PlayerStatus#ACTIVE}
     */
    public boolean isCompetitive() {
        return status == PlayerStatus.ACTIVE;
    }
}
