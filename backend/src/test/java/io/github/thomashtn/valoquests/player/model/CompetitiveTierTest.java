package io.github.thomashtn.valoquests.player.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Verifies how a Henrik tier name becomes a competitive tier.
 */
class CompetitiveTierTest {

    @ParameterizedTest
    @CsvSource({"Gold 2,GOLD_2", " immortal 1 ,IMMORTAL_1", "Radiant,RADIANT", "silver-3,SILVER_3"})
    @DisplayName("Reads a Henrik tier name whatever its case, spacing or separator")
    void shouldReadHenrikTierNames(String name, CompetitiveTier expected) {
        assertThat(CompetitiveTier.fromHenrikName(name)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "Mythic 4"})
    @DisplayName("Falls back to unranked when the name is missing or unknown")
    void shouldFallBackToUnranked(String name) {
        assertThat(CompetitiveTier.fromHenrikName(name)).isEqualTo(CompetitiveTier.UNRANKED);
    }

    @Test
    @DisplayName("Orders tiers from the lowest to the highest rank")
    void shouldOrderTiersByRank() {
        assertThat(CompetitiveTier.IRON_1).isLessThan(CompetitiveTier.GOLD_1);
        assertThat(CompetitiveTier.IMMORTAL_3).isLessThan(CompetitiveTier.RADIANT);
    }
}
