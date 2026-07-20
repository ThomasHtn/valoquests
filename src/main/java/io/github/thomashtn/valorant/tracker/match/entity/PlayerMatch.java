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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Represents the player match component.
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
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private ValorantMatch match;

    @Column(name = "team_id", length = 100)
    private String teamId;

    @Column(name = "agent_id", length = 64)
    private String agentId;

    @Column(name = "agent_name", nullable = false, length = 64)
    private String agentName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MatchResult result;

    @Column(nullable = false)
    private int kills;

    @Column(nullable = false)
    private int deaths;

    @Column(nullable = false)
    private int assists;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private int headshots;

    @Column(name = "bodyshots", nullable = false)
    private int bodyshots;

    @Column(name = "legshots", nullable = false)
    private int legshots;

    @Column(name = "damage_dealt", nullable = false)
    private int damageDealt;

    @Column(name = "rounds_played", nullable = false)
    private int roundsPlayed;

    @Column(precision = 8, scale = 2)
    private BigDecimal acs;

    @Column(precision = 8, scale = 2)
    private BigDecimal adr;

    @Enumerated(EnumType.STRING)
    @Column(name = "competitive_tier", length = 32)
    private CompetitiveTier competitiveTier;

    @Column(name = "rank_rating")
    private Integer rankRating;

    @Column(name = "was_mvp", nullable = false)
    private boolean mvp;
}
