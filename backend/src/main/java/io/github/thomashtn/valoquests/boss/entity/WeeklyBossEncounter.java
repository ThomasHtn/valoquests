package io.github.thomashtn.valoquests.boss.entity;

import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.entity.Season;
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
 * <p>{@code effectiveHp} and {@code activePlayerCount} are fixed when the week opens and never change
 * afterward: a week's fight is resolved once, against the numbers that were true when it started.
 *
 * <p>{@code damageDealt} is frozen at closure rather than derived from
 * {@link io.github.thomashtn.valoquests.ranking.entity.WeeklyPlayerScore} rows. Deriving it would let an
 * admin recalculating a finalized week move a number later weeks calibrate themselves against.
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
     * Valorant act this fight belongs to, when one was known at the time.
     *
     * <p>What makes the campaign restart at every act instead of running forever: the campaign is
     * the fights sharing the act currently in progress, so a new act empties it without deleting
     * anything. {@code null} only when no match had been imported yet and no act could be resolved.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id")
    private Season season;

    /**
     * Hit points the boss must lose to be defeated, frozen when the week opened.
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
     * Number of players the roster held active when this fight was sized.
     *
     * <p>Recorded rather than recomputed: it is what {@link #damageDealt} has to be divided by for the
     * week to say anything about per-player output, and the roster can change after the fact.
     */
    @Column(name = "active_player_count", nullable = false)
    private int activePlayerCount;

    /**
     * Damage the week dealt to this boss, frozen at closure. Zero until the encounter is finalized.
     */
    @Column(name = "damage_dealt", nullable = false)
    private int damageDealt;

    /**
     * Timestamp at which the weekly result became immutable.
     */
    @Column(name = "finalized_at")
    private Instant finalizedAt;

    /**
     * Returns the hit points still standing when the fight ended.
     *
     * @return remaining hit points, never negative
     */
    public int remainingHp() {
        return Math.max(0, effectiveHp - damageDealt);
    }
}
