package io.github.thomashtn.valoquests.match.entity;

import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.GameModeSource;
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
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Stores match-level metadata shared by every tracked participant.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "valorant_match")
public class ValorantMatch extends AuditableEntity {

    /**
     * Internal database identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Stable match identifier returned by Henrik.
     */
    @Column(
        name = "external_match_id",
        nullable = false,
        unique = true,
        length = 100
    )
    private String externalMatchId;

    /**
     * Season containing the match.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    /**
     * UTC timestamp at which the match started.
     */
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    /**
     * Match duration in seconds.
     */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    /**
     * Stable map identifier when provided by Henrik.
     */
    @Column(name = "map_id", length = 64)
    private String mapId;

    /**
     * Human-readable map name.
     */
    @Column(name = "map_name", nullable = false, length = 100)
    private String mapName;

    /**
     * Normalized game mode used by statistics and challenge filters.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "game_mode", nullable = false, length = 32)
    private GameMode gameMode;

    /**
     * Source that resolved {@link #gameMode}, governing whether a later synchronization may enrich it.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "game_mode_source", nullable = false, length = 20)
    private GameModeSource gameModeSource;

    /**
     * Raw Henrik queue identifier retained for diagnostics.
     */
    @Column(name = "queue_id", length = 64)
    private String queueId;

    /**
     * Final red-team score when available.
     */
    @Column(name = "red_score")
    private Integer redScore;

    /**
     * Final blue-team score when available.
     */
    @Column(name = "blue_score")
    private Integer blueScore;
}
