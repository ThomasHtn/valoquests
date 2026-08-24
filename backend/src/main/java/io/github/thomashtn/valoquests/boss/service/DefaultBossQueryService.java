package io.github.thomashtn.valoquests.boss.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.boss.dto.BossHistoryWeekResponse;
import io.github.thomashtn.valoquests.boss.dto.BossResponse;
import io.github.thomashtn.valoquests.boss.dto.CurrentBossResponse;
import io.github.thomashtn.valoquests.boss.entity.BossCatalogEntry;
import io.github.thomashtn.valoquests.boss.entity.WeeklyBossEncounter;
import io.github.thomashtn.valoquests.boss.repository.WeeklyBossEncounterRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.ranking.repository.WeeklyPlayerScoreRepository;
import io.github.thomashtn.valoquests.shared.dto.PageResponse;
import io.github.thomashtn.valoquests.shared.exception.InvalidRequestException;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides optimized read-only access to the current and historical boss confrontations.
 */
@Service
@Transactional(readOnly = true)
public class DefaultBossQueryService implements BossQueryService {

    /**
     * Maximum number of historical weeks accepted by one request.
     */
    private static final int MAXIMUM_PAGE_SIZE = 100;

    /**
     * Service used to draw or retrieve the current week's boss encounter.
     *
     * <p>Selection is idempotent and only ever creates something for the current week, never for a
     * past one, mirroring how the equivalent challenge and ranking queries already rely on lazy
     * selection elsewhere in this codebase.
     */
    private final WeeklyBossSelectionService weeklyBossSelectionService;

    /**
     * Repository used to read historical boss encounters.
     */
    private final WeeklyBossEncounterRepository encounterRepository;

    /**
     * Repository used to read weekly player scores, to sum the week's total damage dealt.
     */
    private final WeeklyPlayerScoreRepository scoreRepository;

    /**
     * Calendar resolving the current week.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the boss query service.
     *
     * @param weeklyBossSelectionService boss selection service
     * @param encounterRepository        weekly boss encounter repository
     * @param scoreRepository            weekly player score repository
     * @param weekCalendar               calendar resolving the current week
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public DefaultBossQueryService(
        WeeklyBossSelectionService weeklyBossSelectionService,
        WeeklyBossEncounterRepository encounterRepository,
        WeeklyPlayerScoreRepository scoreRepository,
        WeekCalendar weekCalendar
    ) {
        this.weeklyBossSelectionService = weeklyBossSelectionService;
        this.encounterRepository = encounterRepository;
        this.scoreRepository = scoreRepository;
        this.weekCalendar = weekCalendar;
    }

    @Override
    @Transactional
    public CurrentBossResponse findCurrent() {
        LocalDate weekStart = weekCalendar.currentWeekStart();
        WeeklyBossEncounter encounter = weeklyBossSelectionService.selectCurrentWeekBoss();

        return new CurrentBossResponse(
            weekStart,
            weekStart.plusDays(6),
            toBossResponse(encounter.getBossCatalogEntry()),
            encounter.getEffectiveHp(),
            totalDamageDealt(weekStart)
        );
    }

    @Override
    public PageResponse<BossHistoryWeekResponse> findHistory(int page, int size) {
        validatePagination(page, size);

        Page<WeeklyBossEncounter> encounterPage = encounterRepository
            .findAllByFinalizedAtIsNotNullOrderByWeekStartDesc(PageRequest.of(page, size));

        return new PageResponse<>(
            encounterPage.getContent().stream().map(this::toHistoryWeek).toList(),
            encounterPage.getNumber(),
            encounterPage.getSize(),
            encounterPage.getTotalElements(),
            encounterPage.getTotalPages()
        );
    }

    /**
     * Sums the damage dealt to the boss so far during a week still in progress.
     *
     * <p>A player's total damage minus their regularity bonus, which is the one component that stays
     * out of the fight: it rewards showing up rather than output. The team bonus is retroactive and
     * identical for everyone who completed a challenge, so this sum is the same number
     * {@code BossChronologyService} arrives at when it walks the week in order at closure. It has to
     * be, or the health bar would show progress the fight never recognised.
     *
     * <p>A showcased non-competitive player's damage is never counted against the boss.
     *
     * @param weekStart week being queried
     * @return cumulative damage dealt
     */
    private int totalDamageDealt(LocalDate weekStart) {
        return scoreRepository.findAllByWeekStartOrderByPositionAsc(weekStart)
            .stream()
            .filter(score -> score.getPlayer().isCompetitive())
            .mapToInt(score -> score.getTotalDamage() - score.getRegularityBonus())
            .sum();
    }

    /**
     * Maps one finalized encounter to its immutable history representation.
     *
     * <p>Damage is read from the encounter, where closure froze it, rather than recomputed from the
     * week's scores: a past week's fight is settled, and an admin recalculating that week must not be
     * able to move a number a later week already inherited from.
     *
     * @param encounter finalized boss encounter
     * @return history entry
     */
    private BossHistoryWeekResponse toHistoryWeek(WeeklyBossEncounter encounter) {
        Player defeatedByPlayer = encounter.getDefeatedByPlayer();

        return new BossHistoryWeekResponse(
            encounter.getWeekStart(),
            encounter.getWeekStart().plusDays(6),
            encounter.getFinalizedAt(),
            toBossResponse(encounter.getBossCatalogEntry()),
            encounter.getEffectiveHp(),
            encounter.getDamageDealt(),
            encounter.isDefeated(),
            defeatedByPlayer == null ? null : defeatedByPlayer.getId(),
            defeatedByPlayer == null ? null : defeatedByPlayer.getDisplayName()
        );
    }

    /**
     * Maps one catalogue entry to its API representation.
     *
     * @param catalogEntry drawn boss
     * @return boss identity response
     */
    private BossResponse toBossResponse(BossCatalogEntry catalogEntry) {
        return new BossResponse(
            catalogEntry.getCode(),
            catalogEntry.getName(),
            catalogEntry.getDescription(),
            catalogEntry.getImageUrl(),
            catalogEntry.getCategory()
        );
    }

    /**
     * Validates public pagination parameters.
     *
     * @param page zero-based page index
     * @param size number of finalized weeks returned per page
     */
    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new InvalidRequestException("page must be greater than or equal to 0");
        }
        if (size < 1 || size > MAXIMUM_PAGE_SIZE) {
            throw new InvalidRequestException("size must be between 1 and " + MAXIMUM_PAGE_SIZE);
        }
    }
}
