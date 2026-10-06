package io.github.thomashtn.valoquests.campaign.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link GuardianProgress}.
 */
class GuardianProgressTest {

    @Test
    @DisplayName("Counts the exact share of hit points taken from a standing guardian")
    void shouldCountTheExactShareTaken() {
        assertThat(GuardianProgress.of(false, 333, 1_000)).isEqualTo(0.333);
    }

    @Test
    @DisplayName("Counts a fallen guardian as fully broken through, whatever the damage")
    void shouldCountAFallenGuardianAsFull() {
        assertThat(GuardianProgress.of(true, 10, 1_000)).isEqualTo(1);
    }

    @Test
    @DisplayName("Caps the overkill on a standing guardian and counts one without hit points as broken")
    void shouldCapTheProgressAtOne() {
        assertThat(GuardianProgress.of(false, 4_000, 1_000)).isEqualTo(1);
        assertThat(GuardianProgress.of(false, 0, 0)).isEqualTo(1);
    }
}
