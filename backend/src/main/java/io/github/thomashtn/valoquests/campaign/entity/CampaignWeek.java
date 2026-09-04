package io.github.thomashtn.valoquests.campaign.entity;

import io.github.thomashtn.valoquests.campaign.model.ExtractionLimiter;
import io.github.thomashtn.valoquests.campaign.model.GuardianCategory;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.player.entity.Player;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One of a campaign's ten weeks: its planet, its guardian, and how its Sunday went.
 *
 * <p>All ten rows are created at opening, so the map exists before the first match is played. The
 * top half — planet, category, weights, guardian, hit points, group — is decided then and never
 * moves. Everything from {@link #damageDealt} down is output: the replay rewrites it from the
 * matches and challenges every time it runs, so nothing here is ever incremented.
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
     * Operator who landed the finishing blow.
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
        return weekStart.plusDays(6);
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
