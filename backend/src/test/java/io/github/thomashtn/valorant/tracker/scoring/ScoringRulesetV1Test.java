package io.github.thomashtn.valorant.tracker.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valorant.tracker.match.model.GameMode;
import io.github.thomashtn.valorant.tracker.scoring.model.BossCategory;
import io.github.thomashtn.valorant.tracker.scoring.model.MatchOutcome;
import org.junit.jupiter.api.Test;

/**
 * Verifies every damage barème of {@link ScoringRulesetV1} against the design notes.
 */
class ScoringRulesetV1Test {

    /** Ruleset under test. */
    private final ScoringRulesetV1 ruleset = new ScoringRulesetV1();

    @Test
    void shouldExposeVersionOne() {
        assertThat(ruleset.version()).isEqualTo(1);
    }

    @Test
    void shouldResolveMatchDamageForEveryValuedMode() {
        assertThat(ruleset.matchDamage(GameMode.COMPETITIVE, MatchOutcome.LOSS)).isEqualTo(350);
        assertThat(ruleset.matchDamage(GameMode.COMPETITIVE, MatchOutcome.DRAW)).isEqualTo(425);
        assertThat(ruleset.matchDamage(GameMode.COMPETITIVE, MatchOutcome.WIN)).isEqualTo(500);

        assertThat(ruleset.matchDamage(GameMode.UNRATED, MatchOutcome.LOSS)).isEqualTo(280);
        assertThat(ruleset.matchDamage(GameMode.UNRATED, MatchOutcome.DRAW)).isEqualTo(340);
        assertThat(ruleset.matchDamage(GameMode.UNRATED, MatchOutcome.WIN)).isEqualTo(400);

        assertThat(ruleset.matchDamage(GameMode.SPIKE_RUSH, MatchOutcome.LOSS)).isEqualTo(130);
        assertThat(ruleset.matchDamage(GameMode.SPIKE_RUSH, MatchOutcome.WIN)).isEqualTo(180);

        assertThat(ruleset.matchDamage(GameMode.SKIRMISH, MatchOutcome.LOSS)).isEqualTo(120);
        assertThat(ruleset.matchDamage(GameMode.SKIRMISH, MatchOutcome.DRAW)).isEqualTo(145);
        assertThat(ruleset.matchDamage(GameMode.SKIRMISH, MatchOutcome.WIN)).isEqualTo(170);

        assertThat(ruleset.matchDamage(GameMode.TEAM_DEATHMATCH, MatchOutcome.LOSS)).isEqualTo(110);
        assertThat(ruleset.matchDamage(GameMode.TEAM_DEATHMATCH, MatchOutcome.DRAW)).isEqualTo(135);
        assertThat(ruleset.matchDamage(GameMode.TEAM_DEATHMATCH, MatchOutcome.WIN)).isEqualTo(160);

        assertThat(ruleset.matchDamage(GameMode.DEATHMATCH, MatchOutcome.LOSS)).isEqualTo(100);
        assertThat(ruleset.matchDamage(GameMode.DEATHMATCH, MatchOutcome.WIN)).isEqualTo(150);
    }

    @Test
    void shouldResolveZeroDamageForModesOutsideTheClosedList() {
        assertThat(ruleset.matchDamage(GameMode.SWIFTPLAY, MatchOutcome.WIN)).isZero();
        assertThat(ruleset.matchDamage(GameMode.CUSTOM, MatchOutcome.WIN)).isZero();
        assertThat(ruleset.matchDamage(GameMode.OTHER, MatchOutcome.WIN)).isZero();
        assertThat(ruleset.matchDamage(null, MatchOutcome.WIN)).isZero();
        assertThat(ruleset.matchDamage(GameMode.COMPETITIVE, null)).isZero();
    }

    @Test
    void shouldFoldADrawIntoTheDefeatTierForModesThatCannotDraw() {
        // Spike Rush and Deathmatch have no draw tier in the design notes; a draw is treated as a
        // defeat rather than rejected, so an unexpected upstream value degrades quietly.
        assertThat(ruleset.matchDamage(GameMode.SPIKE_RUSH, MatchOutcome.DRAW))
            .isEqualTo(ruleset.matchDamage(GameMode.SPIKE_RUSH, MatchOutcome.LOSS));
        assertThat(ruleset.matchDamage(GameMode.DEATHMATCH, MatchOutcome.DRAW))
            .isEqualTo(ruleset.matchDamage(GameMode.DEATHMATCH, MatchOutcome.LOSS));
    }

    @Test
    void shouldResolveChallengeDamageByDifficulty() {
        assertThat(ruleset.challengeDamage(ChallengeDifficulty.EASY)).isEqualTo(1_500);
        assertThat(ruleset.challengeDamage(ChallengeDifficulty.NORMAL)).isEqualTo(2_500);
        assertThat(ruleset.challengeDamage(ChallengeDifficulty.MEDIUM)).isEqualTo(4_000);
        assertThat(ruleset.challengeDamage(ChallengeDifficulty.HARD)).isEqualTo(6_000);
        assertThat(ruleset.challengeDamage(ChallengeDifficulty.VERY_HARD)).isEqualTo(9_000);
    }

    @Test
    void shouldSumToTwentyThreeThousandForACompleteWeeklyPack() {
        int total = ruleset.challengeDamage(ChallengeDifficulty.EASY)
            + ruleset.challengeDamage(ChallengeDifficulty.NORMAL)
            + ruleset.challengeDamage(ChallengeDifficulty.MEDIUM)
            + ruleset.challengeDamage(ChallengeDifficulty.HARD)
            + ruleset.challengeDamage(ChallengeDifficulty.VERY_HARD);

        assertThat(total).isEqualTo(23_000);
    }

    @Test
    void shouldResolveRegularityBonusWithoutCumulatingTiers() {
        assertThat(ruleset.regularityBonus(0)).isZero();
        assertThat(ruleset.regularityBonus(1)).isZero();
        assertThat(ruleset.regularityBonus(2)).isEqualTo(300);
        assertThat(ruleset.regularityBonus(3)).isEqualTo(700);
        assertThat(ruleset.regularityBonus(4)).isEqualTo(1_200);
        assertThat(ruleset.regularityBonus(5)).isEqualTo(1_800);
        assertThat(ruleset.regularityBonus(6)).isEqualTo(2_400);
        assertThat(ruleset.regularityBonus(7)).isEqualTo(3_000);
    }

    @Test
    void shouldClampRegularityBonusBeyondSevenDays() {
        assertThat(ruleset.regularityBonus(8)).isEqualTo(ruleset.regularityBonus(7));
    }

    @Test
    void shouldResolveTeamBonusWithoutCumulatingTiers() {
        assertThat(ruleset.teamBonus(0)).isZero();
        assertThat(ruleset.teamBonus(1)).isZero();
        assertThat(ruleset.teamBonus(2)).isEqualTo(150);
        assertThat(ruleset.teamBonus(3)).isEqualTo(300);
        assertThat(ruleset.teamBonus(4)).isEqualTo(500);
        assertThat(ruleset.teamBonus(5)).isEqualTo(750);
        assertThat(ruleset.teamBonus(6)).isEqualTo(1_100);
    }

    @Test
    void shouldCapTeamBonusBeyondTheFixedRosterSize() {
        assertThat(ruleset.teamBonus(7)).isEqualTo(ruleset.teamBonus(6));
    }

    @Test
    void shouldResolveBossBaseHpByCategory() {
        assertThat(ruleset.bossBaseHp(BossCategory.MINOR)).isEqualTo(80_000);
        assertThat(ruleset.bossBaseHp(BossCategory.STANDARD)).isEqualTo(95_000);
        assertThat(ruleset.bossBaseHp(BossCategory.ELITE)).isEqualTo(115_000);
    }
}
