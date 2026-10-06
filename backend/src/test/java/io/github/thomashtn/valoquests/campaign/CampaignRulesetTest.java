package io.github.thomashtn.valoquests.campaign;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Verifies that guardians and groups are sized per active player, so the game plays the same at two
 * players as at twenty.
 */
class CampaignRulesetTest {

    /**
     * Reference the catalogue's examples are written at.
     */
    private static final int REFERENCE = 5_300;

    @Test
    @DisplayName("Sizes week one's guardian at the documented value for a squad of seven")
    void shouldSizeTheFirstGuardian() {
        assertThat(CampaignRuleset.guardianHitPoints(REFERENCE, 0.60, 7)).isEqualTo(24_486);
    }

    @Test
    @DisplayName("Sizes week one's group at the documented value for a squad of seven")
    void shouldSizeTheFirstGroup() {
        assertThat(CampaignRuleset.woundedCount(REFERENCE, 1.00, 7, 100)).isEqualTo(1_855);
    }

    @ParameterizedTest(name = "{0} player(s) get {1} hit points per player")
    @DisplayName("Keeps the guardian's size per player identical whatever the roster")
    @CsvSource({"2, 3498", "7, 3498", "20, 3498"})
    void shouldKeepGuardianSizePerPlayerConstant(int players, int expectedPerPlayer) {
        assertThat(CampaignRuleset.guardianHitPoints(REFERENCE, 0.60, players) / players)
            .isEqualTo(expectedPerPlayer);
    }

    @Test
    @DisplayName("Grows the group by the week's reward progression")
    void shouldApplyTheRewardProgressionToTheGroup() {
        int firstWeek = CampaignRuleset.woundedCount(REFERENCE, 1.00, 7, 100);
        int tenthWeek = CampaignRuleset.woundedCount(REFERENCE, 1.00, 7, 136);

        assertThat(tenthWeek).isEqualTo((int) Math.round(firstWeek * 1.36));
    }
}
