package io.github.thomashtn.valoquests.campaign.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Verifies the tier ladder's boundaries, which are what make two campaigns comparable.
 */
class CampaignTierTest {

    @ParameterizedTest(name = "a reference of {0} reads as {1}")
    @DisplayName("Places a reference on the ladder, boundaries included")
    @CsvSource({
        "0, AMATEUR",
        "2000, AMATEUR",
        "3499, AMATEUR",
        "3500, NORMAL",
        "8999, NORMAL",
        "9000, CONFIRMED",
        "15999, CONFIRMED",
        "16000, ELITE",
        "119000, ELITE"
    })
    void shouldPlaceAReferenceOnTheLadder(int reference, CampaignTier expected) {
        assertThat(CampaignTier.of(reference)).isEqualTo(expected);
    }
}
