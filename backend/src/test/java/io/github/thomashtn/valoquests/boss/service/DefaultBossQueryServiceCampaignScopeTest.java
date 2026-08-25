package io.github.thomashtn.valoquests.boss.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.boss.dto.BossHistoryWeekResponse;
import io.github.thomashtn.valoquests.boss.entity.BossCatalogEntry;
import io.github.thomashtn.valoquests.boss.entity.WeeklyBossEncounter;
import io.github.thomashtn.valoquests.boss.repository.WeeklyBossEncounterRepository;
import io.github.thomashtn.valoquests.colony.DefaultColonyRuleset;
import io.github.thomashtn.valoquests.ranking.repository.WeeklyPlayerScoreRepository;
import io.github.thomashtn.valoquests.run.entity.Run;
import io.github.thomashtn.valoquests.run.service.RunService;
import io.github.thomashtn.valoquests.scoring.DefaultScoringRuleset;
import io.github.thomashtn.valoquests.scoring.model.BossCategory;
import io.github.thomashtn.valoquests.shared.dto.PageResponse;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * Tests that the campaign history is the run in progress, not the whole boss history.
 */
class DefaultBossQueryServiceCampaignScopeTest {

    /** Identifier of the run the campaign runs in. */
    private static final long CAMPAIGN_RUN_ID = 7L;

    /** Week the only finalized fixture fight was fought in. */
    private static final LocalDate FOUGHT_WEEK = LocalDate.of(2026, 7, 13);

    /** Boss selection dependency, never reached by a history read. */
    private WeeklyBossSelectionService selectionService;

    /** Encounter repository dependency. */
    private WeeklyBossEncounterRepository encounterRepository;

    /** Campaign run dependency. */
    private RunService runService;

    /** Service under test. */
    private DefaultBossQueryService service;

    /** Creates mocked dependencies before each test. */
    @BeforeEach
    void setUp() {
        selectionService = mock(WeeklyBossSelectionService.class);
        encounterRepository = mock(WeeklyBossEncounterRepository.class);
        runService = mock(RunService.class);

        Clock clock = Clock.fixed(Instant.parse("2026-07-21T10:00:00Z"), ZoneOffset.UTC);

        service = new DefaultBossQueryService(
            selectionService,
            encounterRepository,
            mock(WeeklyPlayerScoreRepository.class),
            new WeekCalendar(clock, ZoneOffset.UTC),
            runService,
            new DefaultColonyRuleset(new DefaultScoringRuleset())
        );
    }

    /**
     * Verifies that only the fights of the run in progress make up the campaign.
     *
     * <p>This is what makes a new run open on an empty map: the previous run's fights keep their rows
     * and simply stop being returned.
     */
    @Test
    void shouldReturnOnlyTheCurrentRunFights() {
        when(runService.currentRunId()).thenReturn(Optional.of(CAMPAIGN_RUN_ID));
        when(encounterRepository
            .findAllByRunIdAndFinalizedAtIsNotNullOrderByWeekStartDesc(
                eq(CAMPAIGN_RUN_ID),
                any(Pageable.class)
            ))
            .thenReturn(new PageImpl<>(List.of(finalizedEncounter())));

        PageResponse<BossHistoryWeekResponse> result = service.findHistory(0, 10);

        assertThat(result.content()).singleElement()
            .extracting(BossHistoryWeekResponse::weekStart)
            .isEqualTo(FOUGHT_WEEK);
    }

    /**
     * Verifies that no fight is returned while no run has been opened.
     *
     * <p>The state of a deployment that has not seen its first rollover. Unlike the act this replaces,
     * a missing run is never a resolution failure, so falling back on the whole history would show a
     * campaign that has not started.
     */
    @Test
    void shouldReturnNothingWhenNoRunHasBeenOpened() {
        when(runService.currentRunId()).thenReturn(Optional.empty());

        PageResponse<BossHistoryWeekResponse> result = service.findHistory(0, 10);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        verifyNoInteractions(encounterRepository);
    }

    /**
     * Builds one finalized fight of the run in progress.
     *
     * @return finalized encounter
     */
    private WeeklyBossEncounter finalizedEncounter() {
        BossCatalogEntry catalogEntry = new BossCatalogEntry();
        catalogEntry.setId(1L);
        catalogEntry.setCode("BOSS_A");
        catalogEntry.setName("Boss A");
        catalogEntry.setCategory(BossCategory.STANDARD);
        catalogEntry.setEnabled(true);

        Run run = new Run();
        run.setId(CAMPAIGN_RUN_ID);
        run.setNumber(1);
        run.setFirstWeekStart(LocalDate.of(2026, 6, 1));
        run.setLastWeekStart(LocalDate.of(2026, 8, 3));
        run.setRosterSize(7);

        WeeklyBossEncounter encounter = new WeeklyBossEncounter();
        encounter.setWeekStart(FOUGHT_WEEK);
        encounter.setBossCatalogEntry(catalogEntry);
        encounter.setRun(run);
        encounter.setEffectiveHp(70_000);
        encounter.setDamageDealt(50_000);
        encounter.setActivePlayerCount(7);
        encounter.setFinalizedAt(Instant.parse("2026-07-20T00:05:00Z"));

        return encounter;
    }
}
