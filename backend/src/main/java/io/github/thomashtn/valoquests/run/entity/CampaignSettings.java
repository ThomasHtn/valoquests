package io.github.thomashtn.valoquests.run.entity;

import io.github.thomashtn.valoquests.shared.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Single-row settings governing the campaign's lifecycle, outside of any one run.
 *
 * <p>A dedicated row rather than a flag on {@link Run}: the setting outlives any one run, including
 * the gap where none is open at all — a flag on the run itself would have nowhere to live exactly
 * then, which is the one moment it matters most.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "campaign_settings")
public class CampaignSettings extends AuditableEntity {

    /**
     * Fixed identifier of the single settings row, enforced by a database check constraint.
     */
    public static final short SINGLETON_ID = 1;

    /**
     * Row identifier, always {@link #SINGLETON_ID}.
     */
    @Id
    private Short id = SINGLETON_ID;

    /**
     * Whether the weekly rollover may open a new run once the current one closes.
     *
     * <p>On by default, preserving the always-on behaviour every run before this setting relied on.
     * Turned off, a closed run gives way to "no campaign" rather than an automatic successor, until
     * an operator starts the next one.
     */
    @Column(name = "auto_renew_enabled", nullable = false)
    private boolean autoRenewEnabled = true;
}
