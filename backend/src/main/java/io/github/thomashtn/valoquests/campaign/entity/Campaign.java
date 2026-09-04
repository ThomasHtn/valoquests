package io.github.thomashtn.valoquests.campaign.entity;

import io.github.thomashtn.valoquests.campaign.model.CampaignStatus;
import io.github.thomashtn.valoquests.campaign.model.CampaignTier;
import io.github.thomashtn.valoquests.shared.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One ten-week rescue campaign, opened from the backoffice and calibrated once.
 *
 * <p>The calibration block — reference, tier, volume factor, skill anchors — is written at opening
 * and never touched again. It sizes the guardians, the groups and every challenge target, so a
 * reference that moved mid-campaign would resize a guardian the squad has already spent a week on.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "campaign")
public class Campaign extends AuditableEntity {

    /**
     * Internal database identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Sequential campaign number, starting at one.
     */
    @Column(name = "number", nullable = false, unique = true)
    private int number;

    /**
     * Where the campaign stands in its lifecycle.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CampaignStatus status = CampaignStatus.OPENED;

    /**
     * Instant an operator opened the campaign, which is when the roster froze.
     */
    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    /**
     * Monday the campaign's first week starts on, always strictly after {@link #openedAt}.
     */
    @Column(name = "first_week_start", nullable = false)
    private LocalDate firstWeekStart;

    /**
     * Monday the campaign's tenth and last week starts on.
     */
    @Column(name = "last_week_start", nullable = false)
    private LocalDate lastWeekStart;

    /**
     * Instant the campaign was closed, or {@code null} while it is not.
     */
    @Column(name = "closed_at")
    private Instant closedAt;

    /**
     * Day an operator cut the campaign short, or {@code null} for one that ran its course.
     *
     * <p>{@link #closedAt} cannot tell the two apart: it is set either way. This gives the replay a
     * day to stop on, so a campaign stopped in week four is never credited weeks five to ten.
     */
    @Column(name = "stopped_on")
    private LocalDate stoppedOn;

    /**
     * Number of players frozen into the roster at opening.
     */
    @Column(name = "roster_size", nullable = false)
    private int rosterSize;

    /**
     * Squad's weekly reference per player, the unit every other figure is a multiple of.
     */
    @Column(nullable = false)
    private int reference;

    /**
     * Bracket the reference falls in, printed next to the score and used in no formula.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private CampaignTier tier;

    /**
     * Factor the challenge volume targets are scaled by, bounded at draw time.
     */
    @Column(name = "volume_factor", nullable = false, precision = 6, scale = 4)
    private BigDecimal volumeFactor;

    /**
     * Squad's skill anchors, serialized, the challenge talent bars are resolved against.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "skill_anchors_json", nullable = false, columnDefinition = "jsonb")
    private String skillAnchorsJson;

    /**
     * Months of history the calibration ended up reading.
     */
    @Column(name = "calibration_window_months", nullable = false)
    private int calibrationWindowMonths;

    /**
     * First day of that window.
     */
    @Column(name = "calibration_first_day", nullable = false)
    private LocalDate calibrationFirstDay;

    /**
     * Returns the last day this campaign's base is ever computed on.
     *
     * <p>The tenth Sunday, or the day an operator stopped it. Not the Monday after: the tenth week
     * settles on its own Sunday, so there is nothing left to credit on day seventy-one.
     *
     * @return the campaign's final day
     */
    public LocalDate finalDay() {
        return stoppedOn != null ? stoppedOn : lastWeekStart.plusDays(6);
    }

    /**
     * Determines whether a calendar day falls inside this campaign.
     *
     * @param day day to place, must not be {@code null}
     * @return {@code true} when the day is between the first day and the final day, inclusive
     */
    public boolean covers(LocalDate day) {
        return !day.isBefore(firstWeekStart) && !day.isAfter(finalDay());
    }

    /**
     * Places one week inside this campaign, counting from one.
     *
     * @param weekStart Monday identifying the week, must not be {@code null}
     * @return the week's one-based position, outside {@code 1..10} for a week outside the campaign
     */
    public int weekIndexOf(LocalDate weekStart) {
        return (int) ChronoUnit.WEEKS.between(firstWeekStart, weekStart) + 1;
    }
}
