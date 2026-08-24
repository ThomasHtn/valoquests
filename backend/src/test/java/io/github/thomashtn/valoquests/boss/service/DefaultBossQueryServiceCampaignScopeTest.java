package io.github.thomashtn.valoquests.boss.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.boss.dto.BossHistoryWeekResponse;
import io.github.thomashtn.valoquests.boss.entity.BossCatalogEntry;
import io.github.thomashtn.valoquests.boss.entity.WeeklyBossEncounter;
import io.github.thomashtn.valoquests.boss.repository.WeeklyBossEncounterRepository;
import io.github.thomashtn.valoquests.match.entity.Season;
import io.github.thomashtn.valoquests.ranking.repository.WeeklyPlayerScoreRepository;
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
 * Tests that the campaign history is the act in progress, not the whole boss history.
 */
class DefaultBossQueryServiceCampaignScopeTest {

    /** Identifier of the act the campaign runs in. */
    private static final long CAMPAIGN_ACT_ID = 7L;

    /** Week the only finalized fixture fight was fought in. */
    private static final LocalDate FOUGHT_WEEK = LocalDate.of(2026, 7, 13);

    /** Boss selection dependency, never reached by a history read. */
    private WeeklyBossSelectionService selectionService;

    /** Encounter repository dependency. */
    private WeeklyBossEncounterRepository encounterRepository;

    /** Campaign act dependency. */
    private CampaignSeasonResolver campaignSeasonResolver;

    /** Service under test. */
    private DefaultBossQueryService service;

    /** Creates mocked dependencies before each test. */
    @BeforeEach
    void setUp() {
        selectionService = mock(WeeklyBossSelectionService.class);
        encounterRepository = mock(WeeklyBossEncounterRepository.class);
        campaignSeasonResolver = mock(CampaignSeasonResolver.class);

        Clock clock = Clock.fixed(Instant.parse("2026-07-21T10:00:00Z"), ZoneOffset.UTC);

        service = new DefaultBossQueryService(
            selectionService,
            encounterRepository,
            mock(WeeklyPlayerScoreRepository.class),
            new WeekCalendar(clock, ZoneOffset.UTC),
            campaignSeasonResolver
        );
    }

    /**
     * Verifies that only the fights of the act in progress make up the campaign.
     *
     * <p>This is what makes a new act open on an empty map: the previous act's fights keep their
     * rows and simply stop being returned.
     */
    @Test
    void shouldReturnOnlyTheCurrentActFights() {
        when(campaignSeasonResolver.currentSeasonId()).thenReturn(Optional.of(CAMPAIGN_ACT_ID));
        when(encounterRepository
            .findAllBySeasonIdAndFinalizedAtIsNotNullOrderByWeekStartDesc(
                org.mockito.ArgumentMatchers.eq(CAMPAIGN_ACT_ID),
                any(Pageable.class)
            ))
            .thenReturn(new PageImpl<>(List.of(finalizedEncounter())));

        PageResponse<BossHistoryWeekResponse> result = service.findHistory(0, 10);

        assertThat(result.content()).singleElement()
            .extracting(BossHistoryWeekResponse::weekStart)
            .isEqualTo(FOUGHT_WEEK);

        verify(encounterRepository, never())
            .findAllByFinalizedAtIsNotNullOrderByWeekStartDesc(any(Pageable.class));
    }

    /**
     * Verifies that the whole history is returned while no act can be resolved.
     *
     * <p>The state of a database whose matches have not been imported yet: scoping to nothing would
     * hide a campaign that does exist.
     */
    @Test
    void shouldReturnEveryFightWhenNoActIsKnown() {
        when(campaignSeasonResolver.currentSeasonId()).thenReturn(Optional.empty());
        when(encounterRepository
            .findAllByFinalizedAtIsNotNullOrderByWeekStartDesc(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(finalizedEncounter())));

        PageResponse<BossHistoryWeekResponse> result = service.findHistory(0, 10);

        assertThat(result.content()).hasSize(1);
    }

    /**
     * Builds one finalized fight of the act in progress.
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

        Season season = new Season();
        season.setId(CAMPAIGN_ACT_ID);
        season.setExternalId("v26a4");
        season.setName("v26a4");

        WeeklyBossEncounter encounter = new WeeklyBossEncounter();
        encounter.setWeekStart(FOUGHT_WEEK);
        encounter.setBossCatalogEntry(catalogEntry);
        encounter.setSeason(season);
        encounter.setEffectiveHp(70_000);
        encounter.setDamageDealt(50_000);
        encounter.setActivePlayerCount(7);
        encounter.setFinalizedAt(Instant.parse("2026-07-20T00:05:00Z"));

        return encounter;
    }
}
