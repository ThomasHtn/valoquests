package io.github.thomashtn.valoquests.match.entity;

import io.github.thomashtn.valoquests.match.model.MatchResult;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.CompetitiveTier;
import io.github.thomashtn.valoquests.shared.entity.AuditableEntity;
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
import java.math.RoundingMode;
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
     * Henrik's identifier of the red team.
     */
    private static final String RED_TEAM = "Red";

    /**
     * Scale of a percentage.
     */
    private static final int PERCENT = 100;

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
     * Whether the player earned the match MVP designation.
     */
    @Column(name = "was_mvp", nullable = false)
    private boolean mvp;

    /**
     * Returns the rounds won by the player's team.
     *
     * @return the team's score, {@code null} when the match reports none
     */
    public Integer allyScore() {
        return isRedTeam() ? match.getRedScore() : match.getBlueScore();
    }

    /**
     * Returns the rounds won by the opposing team.
     *
     * @return the opponents' score, {@code null} when the match reports none
     */
    public Integer enemyScore() {
        return isRedTeam() ? match.getBlueScore() : match.getRedScore();
    }

    /**
     * Returns every registered hit, wherever it landed.
     *
     * @return headshots, bodyshots and legshots together
     */
    public int totalShots() {
        return headshots + bodyshots + legshots;
    }

    /**
     * Returns the share of hits that landed on the head, as a percentage with two decimals.
     *
     * @return the percentage, {@code null} when Henrik reported no shot
     */
    public BigDecimal headshotPercentage() {
        int shots = totalShots();

        return shots == 0 ? null : BigDecimal.valueOf(headshots)
            .multiply(BigDecimal.valueOf(PERCENT))
            .divide(BigDecimal.valueOf(shots), 2, RoundingMode.HALF_UP);
    }

    /**
     * Returns kills per death, a deathless match counting as one death.
     *
     * @param scale decimals kept, rounded half up
     * @return the K/D ratio
     */
    public BigDecimal killDeathRatio(int scale) {
        return BigDecimal.valueOf(kills).divide(BigDecimal.valueOf(Math.max(1, deaths)), scale, RoundingMode.HALF_UP);
    }

    /**
     * Returns kills and assists per death with two decimals, a deathless match counting as one death.
     *
     * @return the KDA ratio
     */
    public BigDecimal kda() {
        return BigDecimal.valueOf((long) kills + assists)
            .divide(BigDecimal.valueOf(Math.max(1, deaths)), 2, RoundingMode.HALF_UP);
    }

    /**
     * Tells whether the player played on the red side, whose score Henrik reports apart.
     */
    private boolean isRedTeam() {
        return RED_TEAM.equalsIgnoreCase(teamId);
    }
}
