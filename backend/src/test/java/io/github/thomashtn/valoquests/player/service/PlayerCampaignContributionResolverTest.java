package io.github.thomashtn.valoquests.player.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.boss.repository.WeeklyBossEncounterRepository;
import io.github.thomashtn.valoquests.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link PlayerCampaignContributionResolver}.
 */
@ExtendWith(MockitoExtension.class)
class PlayerCampaignContributionResolverTest {

    /**
     * Monday the campaign's first boss was drawn for.
     */
    private static final LocalDate FIRST_BOSS_WEEK = LocalDate.of(2026, 6, 1);

    /**
     * First instant belonging to {@link #FIRST_BOSS_WEEK}.
     */
    private static final Instant CAMPAIGN_START = Instant.parse("2026-06-01T00:00:00Z");

    /**
     * Mocked weekly boss encounter repository.
     */
    @Mock
    private WeeklyBossEncounterRepository bossEncounterRepository;

    /**
     * Mocked player match repository.
     */
    @Mock
    private PlayerMatchRepository playerMatchRepository;

    /**
     * Resolver under test.
     */
    private PlayerCampaignContributionResolver resolver;

    /**
     * Creates the resolver under test before each test.
     */
    @BeforeEach
    void setUp() {
        resolver = new PlayerCampaignContributionResolver(
            bossEncounterRepository,
            playerMatchRepository,
            new WeekCalendar(Clock.systemUTC(), ZoneOffset.UTC)
        );
    }

    /**
     * Verifies that a boss finisher contributes, without needing to look at matches.
     */
    @Test
    void shouldReportAContributionForABossFinisher() {
        when(bossEncounterRepository.existsByDefeatedByPlayerId(3L)).thenReturn(true);

        assertThat(resolver.hasContributed(3L)).isTrue();

        verifyNoInteractions(playerMatchRepository);
    }

    /**
     * Verifies that a match played since the campaign started counts as a contribution.
     */
    @Test
    void shouldReportAContributionForAMatchPlayedSinceTheCampaignStarted() {
        when(bossEncounterRepository.existsByDefeatedByPlayerId(3L)).thenReturn(false);
        when(bossEncounterRepository.findEarliestWeekStart())
            .thenReturn(Optional.of(FIRST_BOSS_WEEK));
        when(playerMatchRepository
            .existsByPlayerIdAndMatchStartedAtGreaterThanEqual(3L, CAMPAIGN_START))
            .thenReturn(true);

        assertThat(resolver.hasContributed(3L)).isTrue();
    }

    /**
     * Verifies that a player whose whole history predates the campaign is free to delete.
     */
    @Test
    void shouldReportNoContributionWhenEveryMatchPredatesTheCampaign() {
        when(bossEncounterRepository.existsByDefeatedByPlayerId(3L)).thenReturn(false);
        when(bossEncounterRepository.findEarliestWeekStart())
            .thenReturn(Optional.of(FIRST_BOSS_WEEK));
        when(playerMatchRepository
            .existsByPlayerIdAndMatchStartedAtGreaterThanEqual(3L, CAMPAIGN_START))
            .thenReturn(false);

        assertThat(resolver.hasContributed(3L)).isFalse();
    }

    /**
     * Verifies that no boss ever drawn means no possible contribution.
     *
     * <p>The matches are not even queried: with no encounter there is no campaign to have taken
     * part in.
     */
    @Test
    void shouldReportNoContributionWhenNoBossWasEverDrawn() {
        when(bossEncounterRepository.existsByDefeatedByPlayerId(3L)).thenReturn(false);
        when(bossEncounterRepository.findEarliestWeekStart()).thenReturn(Optional.empty());

        assertThat(resolver.hasContributed(3L)).isFalse();

        verifyNoInteractions(playerMatchRepository);
    }
}
