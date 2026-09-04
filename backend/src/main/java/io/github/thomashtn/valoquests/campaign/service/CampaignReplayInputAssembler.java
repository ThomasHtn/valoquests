package io.github.thomashtn.valoquests.campaign.service;

import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.entity.CampaignWeek;
import io.github.thomashtn.valoquests.campaign.model.CampaignDayInput;
import io.github.thomashtn.valoquests.campaign.model.CampaignPlayerDayInput;
import io.github.thomashtn.valoquests.campaign.model.CampaignReplayInputs;
import io.github.thomashtn.valoquests.campaign.model.CampaignWeekInput;
import io.github.thomashtn.valoquests.campaign.model.GuardianFight;
import io.github.thomashtn.valoquests.campaign.model.WeekChallengeYield;
import io.github.thomashtn.valoquests.campaign.repository.CampaignPlayerRepository;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.scoring.model.DailyOutput;
import io.github.thomashtn.valoquests.scoring.model.PlayerDayOutput;
import io.github.thomashtn.valoquests.scoring.model.ValuedMatch;
import io.github.thomashtn.valoquests.scoring.service.DailyOutputReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads everything one campaign needs to be replayed, in one pass.
 *
 * <p>Reads the frozen roster, never the live one. A player deactivated or archived halfway through
 * a campaign keeps feeding the base they started: their statuses are not filtered out here, only
 * their membership of the campaign's roster is checked, so the history of a week already played can
 * never be rewritten by a backoffice click.
 */
@Service
@Transactional(readOnly = true)
public class CampaignReplayInputAssembler {

    /**
     * Every status, so a roster member's matches are read whatever became of them since.
     */
    private static final Set<PlayerStatus> EVERY_STATUS = EnumSet.allOf(PlayerStatus.class);

    /**
     * Reader pricing the campaign's days.
     */
    private final DailyOutputReader dailyOutputReader;

    /**
     * Reader pricing what the campaign's challenges brought back.
     */
    private final CampaignChallengeReader challengeReader;

    /**
     * Repository holding the frozen rosters.
     */
    private final CampaignPlayerRepository campaignPlayerRepository;

    /**
     * Creates the replay input assembler.
     *
     * @param dailyOutputReader        daily output reader
     * @param challengeReader          campaign challenge reader
     * @param campaignPlayerRepository campaign roster repository
     */
    public CampaignReplayInputAssembler(
        DailyOutputReader dailyOutputReader,
        CampaignChallengeReader challengeReader,
        CampaignPlayerRepository campaignPlayerRepository
    ) {
        this.dailyOutputReader = dailyOutputReader;
        this.challengeReader = challengeReader;
        this.campaignPlayerRepository = campaignPlayerRepository;
    }

    /**
     * Gathers one campaign's inputs from its first day to a last day.
     *
     * @param campaign campaign to read
     * @param weeks    the campaign's ten weeks, week one first
     * @param lastDay  last day to read, never past the campaign's own final day
     * @return everything the engine and the writer need
     */
    public CampaignReplayInputs assemble(Campaign campaign, List<CampaignWeek> weeks, LocalDate lastDay) {
        Set<Long> roster = campaignPlayerRepository
            .findAllByCampaignIdOrderByPlayerIdAsc(campaign.getId())
            .stream()
            .map(member -> member.getPlayer().getId())
            .collect(Collectors.toUnmodifiableSet());

        DailyOutput output = dailyOutputReader.read(EVERY_STATUS, campaign.getFirstWeekStart(), lastDay);
        Map<Integer, WeekChallengeYield> yields = challengeReader.read(campaign, roster);
        Map<Integer, GuardianFight> fights = fights(weeks, roster, output, lastDay);

        List<CampaignDayInput> days = new ArrayList<>();
        List<CampaignPlayerDayInput> playerDays = new ArrayList<>();

        for (LocalDate day = campaign.getFirstWeekStart(); !day.isAfter(lastDay); day = day.plusDays(1)) {
            days.add(dayOf(day, roster, output, playerDays));
        }

        return new CampaignReplayInputs(days, weekInputs(weeks, fights, yields, lastDay), fights, yields, playerDays);
    }

    /**
     * Folds one day's roster output into the base's day, collecting the operator rows on the way.
     *
     * @param day        calendar day
     * @param roster     campaign roster
     * @param output     the campaign's priced output
     * @param playerDays operator rows gathered so far, appended to
     * @return the day as the engine consumes it
     */
    private CampaignDayInput dayOf(
        LocalDate day,
        Set<Long> roster,
        DailyOutput output,
        List<CampaignPlayerDayInput> playerDays
    ) {
        int damage = 0;
        int food = 0;
        int components = 0;
        int presence = 0;

        for (Map.Entry<Long, PlayerDayOutput> entry : output.on(day).entrySet()) {
            if (!roster.contains(entry.getKey())) {
                continue;
            }

            PlayerDayOutput dayOutput = entry.getValue();
            damage += dayOutput.damage();
            food += dayOutput.food();
            components += dayOutput.components();
            presence++;
            playerDays.add(new CampaignPlayerDayInput(entry.getKey(), day, dayOutput));
        }

        return new CampaignDayInput(day, damage, food, components, presence);
    }

    /**
     * Replays each started week's guardian fight from the matches of that week.
     *
     * @param weeks   the campaign's weeks
     * @param roster  campaign roster
     * @param output  the campaign's priced output
     * @param lastDay last day read
     * @return the fight of each week that has started, by one-based week index
     */
    private Map<Integer, GuardianFight> fights(
        List<CampaignWeek> weeks,
        Set<Long> roster,
        DailyOutput output,
        LocalDate lastDay
    ) {
        Map<Integer, List<ValuedMatch>> byWeek = new HashMap<>();

        for (ValuedMatch match : output.valuedMatches()) {
            if (!roster.contains(match.playerId())) {
                continue;
            }

            weeks.stream()
                .filter(week -> covers(week, match.day()))
                .findFirst()
                .ifPresent(week -> byWeek
                    .computeIfAbsent(week.getWeekIndex(), ignored -> new ArrayList<>())
                    .add(match));
        }

        Map<Integer, GuardianFight> fights = new HashMap<>();

        for (CampaignWeek week : weeks) {
            if (week.getWeekStart().isAfter(lastDay)) {
                continue;
            }

            fights.put(week.getWeekIndex(), fightOf(
                week.getGuardianHitPoints(),
                byWeek.getOrDefault(week.getWeekIndex(), List.of())
            ));
        }

        return fights;
    }

    /**
     * Walks one week's matches in order until the guardian's hit points run out.
     *
     * @param hitPoints hit points the guardian opened the week with
     * @param matches   the week's valued matches, chronological
     * @return how the fight stands
     */
    private GuardianFight fightOf(int hitPoints, List<ValuedMatch> matches) {
        int dealt = 0;
        ValuedMatch finishing = null;

        for (ValuedMatch match : matches) {
            dealt += match.damage();

            if (finishing == null && dealt >= hitPoints) {
                finishing = match;
            }
        }

        if (finishing == null) {
            return new GuardianFight(dealt, false, null, null, null);
        }

        return new GuardianFight(dealt, true, finishing.startedAt(), finishing.playerId(),
            finishing.playerMatchId());
    }

    /**
     * Turns the weeks whose Sunday has been reached into the engine's settlement inputs.
     *
     * @param weeks   the campaign's weeks
     * @param fights  each started week's fight
     * @param yields  each week's challenge yield
     * @param lastDay last day read
     * @return the weeks to settle, week one first
     */
    private List<CampaignWeekInput> weekInputs(
        List<CampaignWeek> weeks,
        Map<Integer, GuardianFight> fights,
        Map<Integer, WeekChallengeYield> yields,
        LocalDate lastDay
    ) {
        return weeks.stream()
            .filter(week -> !week.settlementDay().isAfter(lastDay))
            .map(week -> {
                GuardianFight fight = fights.getOrDefault(week.getWeekIndex(), GuardianFight.UNTOUCHED);

                return new CampaignWeekInput(
                    week.getWeekIndex(),
                    week.settlementDay(),
                    week.getGuardianHitPoints(),
                    week.getWoundedCount(),
                    fight.damageDealt(),
                    fight.defeated(),
                    yields.getOrDefault(week.getWeekIndex(), WeekChallengeYield.NONE).survivors()
                );
            })
            .toList();
    }

    /**
     * Determines whether a day falls inside one week.
     *
     * @param week week to place the day in
     * @param day  calendar day
     * @return {@code true} when the day belongs to the week
     */
    private boolean covers(CampaignWeek week, LocalDate day) {
        return !day.isBefore(week.getWeekStart()) && !day.isAfter(week.settlementDay());
    }
}
