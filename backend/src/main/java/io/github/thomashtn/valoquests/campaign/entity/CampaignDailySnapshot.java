package io.github.thomashtn.valoquests.campaign.entity;

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
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The base at the close of one day of a campaign.
 *
 * <p>Output of the replay, never its input: every row of a campaign is deleted and written again on
 * each run, so a value here can never compound into the next day's. Reading a stored stock back
 * would make a rerun depend on the run before it, which is exactly what the replay exists to avoid.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "campaign_daily_snapshot")
public class CampaignDailySnapshot extends AuditableEntity {

    /**
     * Internal database identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Campaign the day belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    /**
     * Calendar day.
     */
    @Column(nullable = false)
    private LocalDate day;

    /**
     * Damage the roster dealt that day.
     */
    @Column(nullable = false)
    private int damage;

    /**
     * Food produced that day.
     */
    @Column(name = "food_gained", nullable = false)
    private int foodGained;

    /**
     * Components produced that day.
     */
    @Column(name = "components_gained", nullable = false)
    private int componentsGained;

    /**
     * Inhabitants the day's damage added.
     */
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal growth;

    /**
     * Food the base consumed that evening.
     */
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal eaten;

    /**
     * Inhabitants lost to an empty larder.
     */
    @Column(name = "famine_loss", nullable = false, precision = 14, scale = 3)
    private BigDecimal famineLoss;

    /**
     * Inhabitants lost to a guardian left standing, Sundays only.
     */
    @Column(name = "guardian_loss", nullable = false, precision = 14, scale = 3)
    private BigDecimal guardianLoss;

    /**
     * Wounded who joined the base, Sundays only.
     */
    @Column(nullable = false)
    private int arrivals;

    /**
     * Food in reserve at the close of the day.
     */
    @Column(name = "food_stock", nullable = false, precision = 14, scale = 3)
    private BigDecimal foodStock;

    /**
     * Components in reserve at the close of the day.
     */
    @Column(name = "components_stock", nullable = false, precision = 14, scale = 3)
    private BigDecimal componentsStock;

    /**
     * Inhabitants at the close of the day.
     */
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal population;

    /**
     * Roster operators who played at least one valued match that day.
     */
    @Column(name = "presence_count", nullable = false)
    private int presenceCount;
}
