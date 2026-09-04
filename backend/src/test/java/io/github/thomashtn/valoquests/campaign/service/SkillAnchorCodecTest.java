package io.github.thomashtn.valoquests.campaign.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.thomashtn.valoquests.challenge.model.ChallengeScaling;
import io.github.thomashtn.valoquests.challenge.model.SkillAnchor;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Verifies that a campaign's anchors survive the round trip through its JSON column.
 */
class SkillAnchorCodecTest {

    /**
     * Codec under test, on a real mapper.
     */
    private final SkillAnchorCodec codec = new SkillAnchorCodec(JsonMapper.builder().build());

    @Test
    @DisplayName("Reads back exactly what it wrote")
    void shouldRoundTripAnchors() {
        Map<SkillAnchor, BigDecimal> anchors = Map.of(
            SkillAnchor.LONG_KILLS, BigDecimal.valueOf(18),
            SkillAnchor.LONG_KD, new BigDecimal("1.15")
        );

        ChallengeScaling scaling = codec.toScaling(new BigDecimal("1.20"), codec.toJson(anchors));

        assertThat(scaling.volumeFactor()).isEqualByComparingTo("1.20");
        assertThat(scaling.anchor(SkillAnchor.LONG_KILLS)).contains(BigDecimal.valueOf(18));
        assertThat(scaling.anchor(SkillAnchor.LONG_KD)).contains(new BigDecimal("1.15"));
    }

    @Test
    @DisplayName("Accepts a squad that measures no anchor at all")
    void shouldAcceptAnEmptyMeasurement() {
        ChallengeScaling scaling = codec.toScaling(BigDecimal.ONE, codec.toJson(Map.of()));

        assertThat(scaling.anchors()).isEmpty();
        assertThat(scaling.anchor(SkillAnchor.LONG_ADR)).isEmpty();
    }

    @Test
    @DisplayName("Refuses to guess when the stored anchors cannot be read")
    void shouldRefuseUnreadableAnchors() {
        assertThatThrownBy(() -> codec.toScaling(BigDecimal.ONE, "not json"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("cannot be read");
    }
}
