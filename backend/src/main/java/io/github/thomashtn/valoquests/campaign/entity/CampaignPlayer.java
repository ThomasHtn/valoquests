package io.github.thomashtn.valoquests.campaign.entity;

import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.shared.entity.AuditableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One operator frozen into a campaign's roster at opening.
 *
 * <p>The roster is the campaign's denominator: guardians, groups and the base are all sized per
 * active player. Reading it live from the player table would let a deactivation halfway through
 * shrink a guardian the squad already spent four weeks on, so it is copied here instead. A player
 * deactivated or archived mid-campaign keeps their row and keeps counting until it closes.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "campaign_player")
public class CampaignPlayer extends AuditableEntity {

    /**
     * Internal database identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Campaign the roster belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    /**
     * Operator taking part.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;
}
