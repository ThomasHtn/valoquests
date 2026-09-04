package io.github.thomashtn.valoquests.campaign.service;

import io.github.thomashtn.valoquests.campaign.CampaignRuleset;
import io.github.thomashtn.valoquests.campaign.model.CampaignTier;
import io.github.thomashtn.valoquests.campaign.model.PlayerCalibration;
import io.github.thomashtn.valoquests.campaign.model.SquadCalibration;
import io.github.thomashtn.valoquests.challenge.model.ChallengeScaling;
import io.github.thomashtn.valoquests.challenge.model.SkillAnchor;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.scoring.ScoringRuleset;
import io.github.thomashtn.valoquests.scoring.model.DailyOutput;
import io.github.thomashtn.valoquests.scoring.service.DailyOutputReader;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Measures a squad against itself, once, so a campaign can be sized for it.
 *
 * <p>The reference is the average of each player's weekly average, not a median: a guardian is
 * worth "reference × players", so it is a sum that is being aimed at and a strong player must weigh
 * on it. Every week of the window counts, including the ones a player did not touch — a player who
 * plays little is a weak player, and the reference has to say so. Measured on the real roster on
 * 04/09/2026, the median with empty weeks fell to 396 a week per player, one competitive game,
 * because half the squad plays every other week; the average gave about 1 050.
 *
 * <p>Priced by {@link DailyOutputReader}, the same reader the campaign itself runs on. Applying the
 * streak and the diminishing returns on one side and not the other moved the guardian's bar by about
 * 30 %, which took a regular squad from eight guardians down to under seven for no reason anyone
 * could see on screen.
 */
@Service
@Transactional(readOnly = true)
public class SquadCalibrationService {

    /**
     * Application logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SquadCalibrationService.class);

    /**
     * Days in a week, the unit the reference is expressed in.
     */
    private static final int DAYS_PER_WEEK = 7;

    /**
     * Lowest volume factor a campaign may scale challenge targets by.
     */
    private static final BigDecimal MINIMUM_VOLUME_FACTOR = new BigDecimal("0.40");

    /**
     * Highest volume factor a campaign may scale challenge targets by.
     */
    private static final BigDecimal MAXIMUM_VOLUME_FACTOR = new BigDecimal("3.00");

    /**
     * Decimals the volume factor keeps.
     */
    private static final int VOLUME_FACTOR_SCALE = 4;

    /**
     * Repository used to read history depth and per-match statistics.
     */
    private final PlayerMatchRepository playerMatchRepository;

    /**
     * Reader pricing the window with the campaign's own multipliers.
     */
    private final DailyOutputReader dailyOutputReader;

    /**
     * Barème holding the reference floor.
     */
    private final ScoringRuleset ruleset;

    /**
     * Calendar resolving a day's instant bounds.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the squad calibration service.
     *
     * @param playerMatchRepository player match repository
     * @param dailyOutputReader     daily output reader
     * @param ruleset               scoring ruleset
     * @param weekCalendar          week calendar
     */
    public SquadCalibrationService(
        PlayerMatchRepository playerMatchRepository,
        DailyOutputReader dailyOutputReader,
        ScoringRuleset ruleset,
        WeekCalendar weekCalendar
    ) {
        this.playerMatchRepository = playerMatchRepository;
        this.dailyOutputReader = dailyOutputReader;
        this.ruleset = ruleset;
        this.weekCalendar = weekCalendar;
    }

    /**
     * Calibrates one roster as of a day.
     *
     * @param roster       players the campaign will freeze, never empty
     * @param referenceDay last day of the window, normally the day the campaign is opened
     * @return the squad's reference, tier, scaling and per-player breakdown
     * @throws IllegalArgumentException when the roster is empty
     */
    public SquadCalibration calibrate(List<Player> roster, LocalDate referenceDay) {
        if (roster.isEmpty()) {
            throw new IllegalArgumentException("A campaign cannot be calibrated on an empty roster.");
        }

        Map<Long, LocalDate> earliestDays = earliestMatchDays(roster);
        LocalDate beginnerThreshold = referenceDay.minusMonths(CampaignRuleset.BEGINNER_HISTORY_MONTHS);
        int windowMonths = resolveWindowMonths(roster, earliestDays, beginnerThreshold, referenceDay);
        LocalDate firstDay = referenceDay.minusMonths(windowMonths);

        Map<Long, Integer> averages = weeklyAverages(roster, firstDay, referenceDay);
        List<Long> beginners = roster.stream()
            .map(Player::getId)
            .filter(id -> isBeginner(earliestDays.get(id), beginnerThreshold))
            .toList();
        int squadMedian = medianOfExperienced(averages, beginners);
        beginners.forEach(id -> averages.put(id, squadMedian));

        int reference = Math.max(ruleset.referenceFloor(), (int) Math.round(
            averages.values().stream().mapToInt(Integer::intValue).average().orElse(0)
        ));

        LOGGER.info(
            "Squad calibrated over {} month(s) from {}: reference {} for {} player(s).",
            windowMonths,
            firstDay,
            reference,
            roster.size()
        );

        return new SquadCalibration(
            reference,
            CampaignTier.of(reference),
            new ChallengeScaling(volumeFactor(reference), anchors(roster, firstDay, referenceDay)),
            windowMonths,
            firstDay,
            breakdown(roster, averages, earliestDays, firstDay, beginnerThreshold, windowMonths)
        );
    }

    /**
     * Reads the day of each player's oldest known match.
     *
     * @param roster players to read
     * @return the oldest day per player, absent for a player without a single match
     */
    private Map<Long, LocalDate> earliestMatchDays(List<Player> roster) {
        Map<Long, LocalDate> days = new LinkedHashMap<>(roster.size());

        for (Player player : roster) {
            playerMatchRepository.findEarliestMatchStartedAt(player.getId())
                .map(weekCalendar::dayOf)
                .ifPresent(day -> days.put(player.getId(), day));
        }

        return days;
    }

    /**
     * Chooses the longest window every experienced player's history covers.
     *
     * <p>Shrunk a month at a time and for everyone at once: averaging one player over nine months
     * and another over three would compare two different questions. A beginner never shrinks it —
     * they take the squad's median instead, so a player who joined last week cannot reduce a
     * nine-month reading to one month.
     *
     * @param roster            players to cover
     * @param earliestDays      oldest known day per player
     * @param beginnerThreshold day under which a player is a beginner
     * @param referenceDay      last day of the window
     * @return the number of months to read, at least one
     */
    private int resolveWindowMonths(
        List<Player> roster,
        Map<Long, LocalDate> earliestDays,
        LocalDate beginnerThreshold,
        LocalDate referenceDay
    ) {
        for (int months = CampaignRuleset.CALIBRATION_WINDOW_MONTHS; months > 1; months--) {
            LocalDate firstDay = referenceDay.minusMonths(months);
            boolean everyoneCovered = roster.stream()
                .map(player -> earliestDays.get(player.getId()))
                .filter(earliest -> !isBeginner(earliest, beginnerThreshold))
                .allMatch(earliest -> !earliest.isAfter(firstDay));

            if (everyoneCovered) {
                return months;
            }
        }

        return 1;
    }

    /**
     * Averages each player's weekly damage over the window, empty weeks counted as zero.
     *
     * @param roster       players to measure
     * @param firstDay     first day of the window, inclusive
     * @param referenceDay last day of the window, inclusive
     * @return the weekly average per player
     */
    private Map<Long, Integer> weeklyAverages(List<Player> roster, LocalDate firstDay, LocalDate referenceDay) {
        long days = ChronoUnit.DAYS.between(firstDay, referenceDay) + 1;
        Map<Long, Integer> averages = new LinkedHashMap<>(roster.size());

        for (Player player : roster) {
            DailyOutput output = dailyOutputReader.readPlayer(player.getId(), firstDay, referenceDay);
            long total = 0;

            for (LocalDate day = firstDay; !day.isAfter(referenceDay); day = day.plusDays(1)) {
                total += output.of(player.getId(), day).damage();
            }

            averages.put(player.getId(), (int) Math.round((double) total * DAYS_PER_WEEK / days));
        }

        return averages;
    }

    /**
     * Returns the median weekly average of the players who are not beginners.
     *
     * @param averages  weekly average per player
     * @param beginners players excluded from the median
     * @return the squad median, zero when every player is a beginner
     */
    private int medianOfExperienced(Map<Long, Integer> averages, List<Long> beginners) {
        List<Integer> experienced = new ArrayList<>(averages.entrySet().stream()
            .filter(entry -> !beginners.contains(entry.getKey()))
            .map(Map.Entry::getValue)
            .toList());

        if (experienced.isEmpty()) {
            return 0;
        }

        experienced.sort(Integer::compareTo);
        int middle = experienced.size() / 2;

        if (experienced.size() % 2 == 1) {
            return experienced.get(middle);
        }

        return (experienced.get(middle - 1) + experienced.get(middle)) / 2;
    }

    /**
     * Measures the squad's talent anchors over the window.
     *
     * @param roster       players to measure
     * @param firstDay     first day of the window, inclusive
     * @param referenceDay last day of the window, inclusive
     * @return the anchors, anchors nobody has a sample of omitted
     */
    private Map<SkillAnchor, BigDecimal> anchors(List<Player> roster, LocalDate firstDay, LocalDate referenceDay) {
        Map<Long, List<PlayerMatch>> matchesByPlayer = new LinkedHashMap<>(roster.size());

        for (Player player : roster) {
            matchesByPlayer.put(player.getId(), playerMatchRepository.findForChallengePeriod(
                player.getId(),
                weekCalendar.startOfDay(firstDay),
                weekCalendar.endOfDay(referenceDay)
            ));
        }

        return SkillAnchorReader.read(matchesByPlayer);
    }

    /**
     * Returns the factor the catalogue's base volume targets are scaled by.
     *
     * <p>Bounded so an unusually quiet or unusually heavy squad still gets targets that read as
     * challenges rather than as jokes or as walls.
     *
     * @param reference squad reference
     * @return the bounded factor
     */
    private BigDecimal volumeFactor(int reference) {
        BigDecimal raw = BigDecimal.valueOf(reference)
            .divide(BigDecimal.valueOf(CampaignRuleset.CALIBRATION_ANCHOR_REFERENCE), VOLUME_FACTOR_SCALE,
                RoundingMode.HALF_UP);

        return raw.min(MAXIMUM_VOLUME_FACTOR).max(MINIMUM_VOLUME_FACTOR);
    }

    /**
     * Assembles the per-player breakdown the backoffice preview shows.
     *
     * @param roster            players measured
     * @param averages          weekly average per player, beginners already replaced
     * @param earliestDays      oldest known day per player
     * @param firstDay          first day of the window
     * @param beginnerThreshold day under which a player is a beginner
     * @param windowMonths      months the window ended up covering
     * @return one line per player, roster order
     */
    private List<PlayerCalibration> breakdown(
        List<Player> roster,
        Map<Long, Integer> averages,
        Map<Long, LocalDate> earliestDays,
        LocalDate firstDay,
        LocalDate beginnerThreshold,
        int windowMonths
    ) {
        int weeks = (int) Math.round(windowMonths * 365.0 / 12 / DAYS_PER_WEEK);

        return roster.stream()
            .map(player -> {
                LocalDate earliest = earliestDays.get(player.getId());

                return new PlayerCalibration(
                    player.getId(),
                    player.getGameName() + "#" + player.getTagLine(),
                    averages.getOrDefault(player.getId(), 0),
                    weeks,
                    earliest,
                    earliest != null && !earliest.isAfter(firstDay),
                    isBeginner(earliest, beginnerThreshold)
                );
            })
            .toList();
    }

    /**
     * Determines whether a player is too new to be measured.
     *
     * @param earliestDay       oldest known day, {@code null} without any match
     * @param beginnerThreshold day under which a player is a beginner
     * @return {@code true} when the player takes the squad's median instead of their own average
     */
    private boolean isBeginner(LocalDate earliestDay, LocalDate beginnerThreshold) {
        return earliestDay == null || earliestDay.isAfter(beginnerThreshold);
    }
}
