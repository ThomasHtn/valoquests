package io.github.thomashtn.valoquests.challenge.model;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.thomashtn.valoquests.campaign.CampaignRuleset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pins the two numbers the whole game is sized on, since nothing else asserts them.
 */
class CampaignDifficultyTest {

    /**
     * Ruleset sizing a guardian from a reference.
     */
    private final CampaignRuleset ruleset = new CampaignRuleset();

    @Test
    @DisplayName("Writes the amateur reference at the catalogue's own anchor")
    void shouldAnchorAmateurOnTheCatalogue() {
        // The catalogue's targets are written at 5 300, so amateur serves its numbers untouched.
        assertThat(CampaignDifficulty.AMATEUR.reference()).isEqualTo(5_300);
    }

    @Test
    @DisplayName("Makes a pro guardian exactly twice an amateur one")
    void shouldDoubleTheGuardianAtPro() {
        int amateur = ruleset.guardianHitPoints(CampaignDifficulty.AMATEUR.reference(), 0.60, 5);
        int pro = ruleset.guardianHitPoints(CampaignDifficulty.PRO.reference(), 0.60, 5);

        assertThat(pro).isEqualTo(amateur * 2);
    }
}
