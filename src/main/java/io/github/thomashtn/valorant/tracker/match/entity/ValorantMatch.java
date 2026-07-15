package io.github.thomashtn.valorant.tracker.match.entity;

import io.github.thomashtn.valorant.tracker.match.model.GameMode;
import io.github.thomashtn.valorant.tracker.shared.entity.AuditableEntity;
import jakarta.persistence.*;
import java.time.*;
import lombok.*;

/**
 * Represents the valorant match component.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "valorant_match")
public class ValorantMatch extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_match_id", nullable = false, unique = true, length = 100)
    private String externalMatchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "map_id", length = 64)
    private String mapId;

    @Column(name = "map_name", nullable = false, length = 100)
    private String mapName;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_mode", nullable = false, length = 32)
    private GameMode gameMode;

    @Column(name = "red_score")
    private Integer redScore;

    @Column(name = "blue_score")
    private Integer blueScore;
}
