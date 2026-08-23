package io.github.thomashtn.valoquests.boss.entity;

import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.shared.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Associates one catalogue boss with a calendar week and tracks the confrontation's outcome.
 *
 * <p>{@code baseHp}, {@code difficultyModifierPercent} and {@code effectiveHp} are fixed at selection
 * time and never change afterward, even if the catalogue entry or the collective modifier evolve later:
 * a week's fight is resolved once, against the numbers that were true when it started.
 *
 * <p>Total damage dealt is intentionally not stored here: it is derived on demand from
 * {@link io.github.thomashtn.valoquests.ranking.entity.WeeklyPlayerScore} rows for the same week, so
 * there is a single source of truth for weekly damage.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "weekly_boss_encounter",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_weekly_boss_encounter_week",
        columnNames = {"week_start"}
    )
)
public class WeeklyBossEncounter extends AuditableEntity {

    /**
     * Internal database identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Monday identifying the confrontation's week.
     */
    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    /**
     * Catalogue boss drawn for the week.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boss_catalog_entry_id", nullable = false)
    private BossCatalogEntry bossCatalogEntry;

    /**
     * Version of the {@link io.github.thomashtn.valoquests.scoring.ScoringRuleset} resolved at
     * selection time. Recalculating this week must always resolve through this version.
     */
    @Column(name = "ruleset_version", nullable = false)
    private int rulesetVersion;

    /**
     * Base hit points copied from the catalogue entry at selection time.
     */
    @Column(name = "base_hp", nullable = false)
    private int baseHp;

    /**
     * Collective difficulty modifier applied this week, as a percentage (100 = neutral).
     */
    @Column(name = "difficulty_modifier_percent", nullable = false)
    private int difficultyModifierPercent;

    /**
     * Effective hit points for the week: {@code baseHp * difficultyModifierPercent / 100}, rounded and
     * frozen at selection time.
     */
    @Column(name = "effective_hp", nullable = false)
    private int effectiveHp;

    /**
     * Whether the boss was defeated. Only meaningful once {@link #finalizedAt} is set.
     */
    @Column(nullable = false)
    private boolean defeated;

    /**
     * Player who dealt the finishing blow, when the boss was defeated.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "defeated_by_player_id")
    private Player defeatedByPlayer;

    /**
     * Match that dealt the finishing blow, when the boss was defeated.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finishing_player_match_id")
    private PlayerMatch finishingPlayerMatch;

    /**
     * Timestamp at which the weekly result became immutable.
     */
    @Column(name = "finalized_at")
    private Instant finalizedAt;
}
