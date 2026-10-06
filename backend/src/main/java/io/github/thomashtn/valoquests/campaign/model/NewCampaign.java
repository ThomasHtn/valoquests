package io.github.thomashtn.valoquests.campaign.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.entity.CampaignPlayer;
import io.github.thomashtn.valoquests.campaign.entity.CampaignWeek;
import java.util.List;

/**
 * A campaign built but not yet persisted: the row, its frozen roster and its ten weeks.
 *
 * @param campaign the campaign row
 * @param roster   the players frozen into it
 * @param weeks    its ten weeks, week one first
 */
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP",
    justification = """
        The campaign is a JPA entity handed straight to the repository that saves it:
        copying it would detach it from the persistence context and break the very save
        this holder exists to carry out.
        """
)
public record NewCampaign(Campaign campaign, List<CampaignPlayer> roster, List<CampaignWeek> weeks) {

    /**
     * Creates an immutable holder.
     */
    public NewCampaign {
        roster = List.copyOf(roster);
        weeks = List.copyOf(weeks);
    }
}
