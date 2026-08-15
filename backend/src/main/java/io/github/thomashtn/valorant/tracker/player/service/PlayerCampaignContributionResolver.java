package io.github.thomashtn.valorant.tracker.player.service;

import io.github.thomashtn.valorant.tracker.boss.repository.WeeklyBossEncounterRepository;
import io.github.thomashtn.valorant.tracker.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valorant.tracker.week.WeekCalendar;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Decides whether a player took part in the campaign, and therefore whether removing it would
 * rewrite history.
 *
 * <p>Boss damage is not stored: it is replayed from the matches of a week by
 * {@code BossChronologyService}. Asking the exact question — "did this player's matches move a boss
 * health bar" — would mean replaying every finalized week to answer a delete request, so the
 * question asked here is the cheap superset: does the player have a match from the moment the
 * campaign started.
 *
 * <p>The imprecision is one-directional and deliberate. A match falling in a week that has no boss
 * encounter counts as a contribution although it dealt no damage, so a player may be archived where
 * a deletion would have been safe. That is the error worth making: archiving is reversible, and a
 * deletion that removes a player a finalized week still names is not.
 */
@Service
public class PlayerCampaignContributionResolver {

    /**
     * Repository used to locate the campaign's first week and boss finishers.
     */
    private final WeeklyBossEncounterRepository bossEncounterRepository;

    /**
     * Repository used to look for matches played since the campaign started.
     */
    private final PlayerMatchRepository playerMatchRepository;

    /**
     * Calendar resolving a week's instant bounds.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the campaign contribution resolver.
     *
     * @param bossEncounterRepository weekly boss encounter repository
     * @param playerMatchRepository   player match repository
     * @param weekCalendar            week calendar
     */
    public PlayerCampaignContributionResolver(
        WeeklyBossEncounterRepository bossEncounterRepository,
        PlayerMatchRepository playerMatchRepository,
        WeekCalendar weekCalendar
    ) {
        this.bossEncounterRepository = bossEncounterRepository;
        this.playerMatchRepository = playerMatchRepository;
        this.weekCalendar = weekCalendar;
    }

    /**
     * Determines whether a player contributed to the campaign.
     *
     * @param playerId tracked player identifier
     * @return {@code true} when finalized campaign data may depend on the player
     */
    @Transactional(readOnly = true)
    public boolean hasContributed(long playerId) {
        if (bossEncounterRepository.existsByDefeatedByPlayerId(playerId)) {
            return true;
        }

        Optional<LocalDate> campaignStart = bossEncounterRepository.findEarliestWeekStart();

        return campaignStart
            .map(weekCalendar::startOf)
            .map(start -> playerMatchRepository
                .existsByPlayerIdAndMatchStartedAtGreaterThanEqual(playerId, start))
            .orElse(false);
    }
}
