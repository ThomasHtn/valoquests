package io.github.thomashtn.valoquests.challenge.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SkillAnchor}.
 */
class SkillAnchorTest {

    /**
     * Verifies that every long-format filter maps to the same anchors.
     */
    @Test
    void shouldShareLongFormatAnchorsAcrossLongFormatFilters() {
        assertThat(SkillAnchor.of(ChallengeMetric.KILLS, ChallengeGameMode.COMPETITIVE))
            .contains(SkillAnchor.LONG_KILLS);
        assertThat(SkillAnchor.of(ChallengeMetric.KILLS, ChallengeGameMode.UNRATED))
            .contains(SkillAnchor.LONG_KILLS);
        assertThat(SkillAnchor.of(ChallengeMetric.ADR, ChallengeGameMode.COMPETITIVE_OR_UNRATED))
            .contains(SkillAnchor.LONG_ADR);
        assertThat(SkillAnchor.of(ChallengeMetric.SCORE, ChallengeGameMode.COMPETITIVE))
            .contains(SkillAnchor.LONG_SCORE);
    }

    /**
     * Verifies the short-format anchors and the absence of every other combination.
     */
    @Test
    void shouldOnlyAnchorWhatTheSquadMeasures() {
        assertThat(SkillAnchor.of(ChallengeMetric.KILLS, ChallengeGameMode.DEATHMATCH))
            .contains(SkillAnchor.DEATHMATCH_KILLS);
        assertThat(SkillAnchor.of(ChallengeMetric.HEADSHOTS, ChallengeGameMode.DEATHMATCH))
            .contains(SkillAnchor.DEATHMATCH_HEADSHOTS);
        assertThat(SkillAnchor.of(ChallengeMetric.KILLS, ChallengeGameMode.TEAM_DEATHMATCH))
            .contains(SkillAnchor.TEAM_DEATHMATCH_KILLS);

        assertThat(SkillAnchor.of(ChallengeMetric.ADR, ChallengeGameMode.DEATHMATCH)).isEmpty();
        assertThat(SkillAnchor.of(ChallengeMetric.HEADSHOTS, ChallengeGameMode.TEAM_DEATHMATCH)).isEmpty();
        assertThat(SkillAnchor.of(ChallengeMetric.KILLS, ChallengeGameMode.ANY)).isEmpty();
        assertThat(SkillAnchor.of(ChallengeMetric.MATCHES_PLAYED, ChallengeGameMode.COMPETITIVE)).isEmpty();
    }
}
