package io.github.thomashtn.valoquests.campaign.entity;

import io.github.thomashtn.valoquests.campaign.model.ExtractionLimiter;
import io.github.thomashtn.valoquests.campaign.model.GuardianCategory;
import io.github.thomashtn.valoquests.campaign.model.GuardianProgress;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.shared.entity.AuditableEntity;
import io.github.thomashtn.valoquests.shared.time.WeekCalendar;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One of a campaign's ten weeks: its planet, its guardian, and how its Sunday went.
 *
 * <p>Fields up to the group are frozen at opening; everything from {@link #damageDealt} down is
 * rewritten by each replay, never incremented.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "campaign_week")
public class CampaignWeek extends AuditableEntity {

    /**
     * Internal database identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Campaign the week belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    /**
     * One-based position in the campaign, from one to ten.
     */
    @Column(name = "week_index", nullable = false)
    private int weekIndex;

    /**
     * Monday identifying the week.
     */
    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    /**
     * Planet the wounded are stranded on.
     */
    @Column(name = "planet_name", nullable = false, length = 60)
    private String planetName;

    /**
     * Weight class the guardian was drawn from.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GuardianCategory category;

    /**
     * Guardian weight of the week, in shares of reference × active players.
     */
    @Column(name = "guardian_weight", nullable = false, precision = 4, scale = 2)
    private BigDecimal guardianWeight;

    /**
     * Group weight of the week, in shares of reference × active players.
     */
    @Column(name = "group_weight", nullable = false, precision = 4, scale = 2)
    private BigDecimal groupWeight;

    /**
     * Guardian drawn for the week.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guardian_id", nullable = false)
    private Guardian guardian;

    /**
     * Hit points the guardian opens the week with, frozen at opening.
     */
    @Column(name = "guardian_hit_points", nullable = false)
    private int guardianHitPoints;

    /**
     * Wounded stranded on the planet, frozen at opening.
     */
    @Column(name = "wounded_count", nullable = false)
    private int woundedCount;

    /**
     * Damage the roster dealt over the week.
     */
    @Column(name = "damage_dealt", nullable = false)
    private int damageDealt;

    /**
     * Whether the guardian fell.
     */
    @Column(nullable = false)
    private boolean defeated;

    /**
     * Start instant of the match that took the guardian down, never the synchronization's.
     */
    @Column(name = "defeated_at")
    private Instant defeatedAt;

    /**
     * Player who landed the finishing blow.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "defeated_by_player_id")
    private Player defeatedByPlayer;

    /**
     * Match that landed the finishing blow.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finishing_player_match_id")
    private PlayerMatch finishingPlayerMatch;

    /**
     * Wounded the week's challenges brought back, capped by the group.
     */
    @Column(name = "challenge_rescued", nullable = false)
    private int challengeRescued;

    /**
     * Wounded the ship extracted on Sunday.
     */
    @Column(name = "extraction_rescued", nullable = false)
    private int extractionRescued;

    /**
     * Food spent settling those extracted.
     */
    @Column(name = "food_spent", nullable = false)
    private int foodSpent;

    /**
     * Components spent reaching those extracted.
     */
    @Column(name = "components_spent", nullable = false)
    private int componentsSpent;

    /**
     * What capped the extraction.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private ExtractionLimiter limiter = ExtractionLimiter.NONE;

    /**
     * Inhabitants a guardian left standing killed on Sunday evening.
     */
    @Column(name = "base_loss", nullable = false, precision = 14, scale = 3)
    private BigDecimal baseLoss = BigDecimal.ZERO;

    /**
     * Whether the week's Sunday has been settled by a replay.
     */
    @Column(nullable = false)
    private boolean settled;

    /**
     * Returns the Sunday the week settles on.
     *
     * @return the week's last day
     */
    public LocalDate settlementDay() {
        return WeekCalendar.lastDayOf(weekStart);
    }

    /**
     * Determines whether a calendar day falls inside this week, Monday to Sunday.
     *
     * @param day calendar day, must not be {@code null}
     * @return {@code true} when the day belongs to the week
     */
    public boolean contains(LocalDate day) {
        return !day.isBefore(weekStart) && !day.isAfter(settlementDay());
    }

    /**
     * Returns how far the squad got on the guardian, as the Sunday settlement counts it.
     *
     * @return progress between zero and one
     */
    public double progress() {
        return GuardianProgress.of(defeated, damageDealt, guardianHitPoints);
    }

    /**
     * Returns the total number of wounded brought home that week.
     *
     * @return challenge rescues plus extraction rescues
     */
    public int rescued() {
        return challengeRescued + extractionRescued;
    }
}
