package io.github.thomashtn.valorant.tracker.match.entity;

import io.github.thomashtn.valorant.tracker.match.model.MatchResult;
import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.player.model.CompetitiveTier;
import io.github.thomashtn.valorant.tracker.shared.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Stores the statistics of one tracked player for one Valorant match.
 *
 * <p>The match-level metadata is stored by {@link ValorantMatch}. This entity only contains data
 * that depends on the tracked player, such as the selected agent, combat statistics and result.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "player_match",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_player_match_player_match",
        columnNames = {"player_id", "match_id"}
    )
)
public class PlayerMatch extends AuditableEntity {

    /**
     * Internal database identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tracked player represented by these match statistics.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    /**
     * Match containing the shared metadata.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private ValorantMatch match;

    /**
     * Team identifier returned by Henrik for this participant.
     */
    @Column(name = "team_id", length = 100)
    private String teamId;

    /**
     * Stable identifier of the selected agent when available.
     */
    @Column(name = "agent_id", length = 64)
    private String agentId;

    /**
     * Human-readable name of the selected agent.
     */
    @Column(name = "agent_name", nullable = false, length = 64)
    private String agentName;

    /**
     * Result of the match from the tracked player's perspective.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MatchResult result;

    /**
     * Number of eliminations performed by the player.
     */
    @Column(nullable = false)
    private int kills;

    /**
     * Number of times the player was eliminated.
     */
    @Column(nullable = false)
    private int deaths;

    /**
     * Number of assists performed by the player.
     */
    @Column(nullable = false)
    private int assists;

    /**
     * Total combat score returned by Henrik.
     */
    @Column(nullable = false)
    private int score;

    /**
     * Number of registered headshot hits.
     */
    @Column(nullable = false)
    private int headshots;

    /**
     * Number of registered body-shot hits.
     */
    @Column(name = "bodyshots", nullable = false)
    private int bodyshots;

    /**
     * Number of registered leg-shot hits.
     */
    @Column(name = "legshots", nullable = false)
    private int legshots;

    /**
     * Total damage dealt during the match.
     */
    @Column(name = "damage_dealt", nullable = false)
    private int damageDealt;

    /**
     * Number of rounds used to normalize per-round statistics.
     */
    @Column(name = "rounds_played", nullable = false)
    private int roundsPlayed;

    /**
     * Average combat score calculated for the match.
     */
    @Column(precision = 8, scale = 2)
    private BigDecimal acs;

    /**
     * Average damage per round calculated for the match.
     */
    @Column(precision = 8, scale = 2)
    private BigDecimal adr;

    /**
     * Competitive tier recorded at match time when available.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "competitive_tier", length = 32)
    private CompetitiveTier competitiveTier;

    /**
     * Rank rating recorded at match time when available.
     */
    @Column(name = "rank_rating")
    private Integer rankRating;

    /**
     * Whether the player earned the match MVP designation.
     */
    @Column(name = "was_mvp", nullable = false)
    private boolean mvp;
}
