package io.github.thomashtn.valoquests.campaign.entity;

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
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * What one frozen-roster operator produced on one day of a campaign.
 *
 * <p>Written by the replay alongside the base's own day. Stored rather than re-priced on demand:
 * the weekly titles, the squad table and the profile all read the same figures, and each of them
 * would otherwise walk sixty days of matches per request to get them.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "campaign_player_day")
public class CampaignPlayerDay extends AuditableEntity {

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
     * Operator the day belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    /**
     * Calendar day.
     */
    @Column(nullable = false)
    private LocalDate day;

    /**
     * Damage dealt to the week's guardian, food and components summed.
     */
    @Column(nullable = false)
    private int damage;

    /**
     * Food produced.
     */
    @Column(nullable = false)
    private int food;

    /**
     * Components produced.
     */
    @Column(nullable = false)
    private int components;

    /**
     * Valued matches played.
     */
    @Column(name = "match_count", nullable = false)
    private int matchCount;

    /**
     * Valued matches the day's diminishing returns priced below full value.
     */
    @Column(name = "reduced_match_count", nullable = false)
    private int reducedMatchCount;

    /**
     * Consecutive played days ending on this day, this day included.
     */
    @Column(name = "streak_days", nullable = false)
    private int streakDays;

    /**
     * Bonus every match of the day earned from that streak.
     */
    @Column(name = "streak_bonus_percent", nullable = false)
    private int streakBonusPercent;
}
