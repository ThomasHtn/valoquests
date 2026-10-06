package io.github.thomashtn.valoquests.player.mapper;

import io.github.thomashtn.valoquests.henrik.dto.mmr.HenrikMmrResponse;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.CompetitiveTier;
import org.springframework.stereotype.Component;

/**
 * Applies Henrik MMR data to a tracked player entity.
 */
@Component
public class HenrikMmrMapper {

    /**
     * Updates the current competitive tier and Rank Rating of a player.
     * Missing MMR data is treated as an unranked state rather than an error.
     *
     * @param response Henrik MMR response
     * @param player player to update
     */
    public void updatePlayer(
        HenrikMmrResponse response,
        Player player
    ) {
        if (response == null || response.data() == null
            || response.data().current() == null) {
            player.setCompetitiveTier(CompetitiveTier.UNRANKED);
            player.setRankRating(null);
            return;
        }

        HenrikMmrResponse.HenrikCurrentMmr current =
            response.data().current();

        player.setCompetitiveTier(
            CompetitiveTier.fromHenrikName(current.tier() == null ? null : current.tier().name())
        );
        player.setRankRating(current.rankRating());
    }
}
