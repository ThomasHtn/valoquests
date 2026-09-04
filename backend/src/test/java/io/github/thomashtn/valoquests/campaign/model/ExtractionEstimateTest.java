package io.github.thomashtn.valoquests.campaign.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies the extraction rule the settlement and the forecast share.
 */
class ExtractionEstimateTest {

    @Test
    @DisplayName("Reaches the whole group when both stocks cover it and the guardian is down")
    void shouldBringEverybodyHome() {
        ExtractionEstimate estimate = ExtractionEstimate.of(50, 10, 2_000, 1_400, 1_000, 1);

        assertThat(estimate.challengeRescued()).isEqualTo(10);
        assertThat(estimate.remainingGroup()).isEqualTo(40);
        assertThat(estimate.byComponents()).isEqualTo(100);
        assertThat(estimate.byFood()).isEqualTo(162);
        assertThat(estimate.extracted()).isEqualTo(40);
        assertThat(estimate.rescued()).isEqualTo(50);
        assertThat(estimate.limiter()).isEqualTo(ExtractionLimiter.NONE);
    }

    @Test
    @DisplayName("Names the stock that ran out first and never spends the seven protected evenings")
    void shouldNameTheBindingStock() {
        // 1 000 inhabitants protect 56 food: 100 spendable food settles 8, components carry 100.
        ExtractionEstimate byFood = ExtractionEstimate.of(50, 0, 156, 1_400, 1_000, 1);
        assertThat(byFood.byFood()).isEqualTo(8);
        assertThat(byFood.extracted()).isEqualTo(8);
        assertThat(byFood.limiter()).isEqualTo(ExtractionLimiter.FOOD);

        ExtractionEstimate byComponents = ExtractionEstimate.of(50, 0, 2_000, 70, 1_000, 1);
        assertThat(byComponents.byComponents()).isEqualTo(5);
        assertThat(byComponents.limiter()).isEqualTo(ExtractionLimiter.COMPONENTS);
    }

    @Test
    @DisplayName("Scales what is reachable by the breakthrough and caps the challenges at the group")
    void shouldScaleByTheBreakthrough() {
        ExtractionEstimate estimate = ExtractionEstimate.of(30, 45, 2_000, 1_400, 1_000, 0.5);

        assertThat(estimate.challengeRescued()).isEqualTo(30);
        assertThat(estimate.remainingGroup()).isZero();
        assertThat(estimate.extracted()).isZero();
        assertThat(estimate.limiter()).isEqualTo(ExtractionLimiter.NONE);

        ExtractionEstimate half = ExtractionEstimate.of(50, 10, 2_000, 1_400, 1_000, 0.5);
        assertThat(half.extracted()).isEqualTo(20);
        assertThat(half.limiter()).isEqualTo(ExtractionLimiter.GROUP);
    }
}
