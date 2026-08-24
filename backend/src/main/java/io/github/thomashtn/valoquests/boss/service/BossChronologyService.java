package io.github.thomashtn.valoquests.boss.service;

import io.github.thomashtn.valoquests.challenge.calculator.ChallengeProgressCalculator;
import io.github.thomashtn.valoquests.challenge.calculator.ChallengeProgressCalculatorRegistry;
import io.github.thomashtn.valoquests.challenge.calculator.PlayerChallengeContext;
import io.github.thomashtn.valoquests.challenge.calculator.PlayerChallengeContextFactory;
import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.parser.ChallengeDefinitionParser;
import io.github.thomashtn.valoquests.challenge.service.WeeklyChallengeSelectionService;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.scoring.ScoringRuleset;
import io.github.thomashtn.valoquests.scoring.service.MatchDamageCalculator;
import io.github.thomashtn.valoquests.scoring.service.WeeklyMatchDamageResolver;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Determines whether a week's boss was defeated and, when it was, which exact match dealt the finishing
 * blow.
 *
 * <p>Builds one global, deterministic timeline of every valued match played during the week by every
 * active player, orders it chronologically, and walks it cumulatively against the boss's effective hit
 * points. A match's contribution to this timeline is its own damage, plus, for the one player it belongs
 * to, the damage of any challenge that match happened to complete — resolved by
 * {@link ChallengeProgressCalculator#findFirstCompletionIndex} so no calculator implementation had to
 * change. The regularity bonus never enters this timeline: it rewards showing up rather than output, so
 * it moves the individual ranking without ever being the decisive blow against the shared boss.
 *
 * <p>The team bonus is retroactive, so the moment a player joins a challenge it also raises what every
 * earlier completer was worth. That uplift is credited to the joining player's own match, which keeps
 * the timeline strictly chronological while leaving the total identical to the sum of the weekly scores.
 */
@Service
public class BossChronologyService {

    /**
     * Active player repository.
     */
    private final PlayerRepository playerRepository;

    /**
     * Factory building each player's weekly match context.
     */
    private final PlayerChallengeContextFactory contextFactory;

    /**
     * Service resolving the challenge pack owned by a week.
     */
    private final WeeklyChallengeSelectionService weeklyChallengeSelectionService;

    /**
     * Parser turning persisted challenge rules into typed definitions.
     */
    private final ChallengeDefinitionParser definitionParser;

    /**
     * Registry resolving the calculator for one progress mode.
     */
    private final ChallengeProgressCalculatorRegistry calculatorRegistry;

    /**
     * Resolves whether one match is valued at all.
     */
    private final MatchDamageCalculator matchDamageCalculator;

    /**
     * Prices each match after the ruleset's daily diminishing returns.
     */
    private final WeeklyMatchDamageResolver damageResolver;

    /**
     * Creates the boss chronology service.
     *
     * @param playerRepository                player repository
     * @param contextFactory                  player challenge context factory
     * @param weeklyChallengeSelectionService  weekly challenge selection service
     * @param definitionParser                challenge definition parser
     * @param calculatorRegistry               challenge calculator registry
     * @param matchDamageCalculator            match damage calculator
     * @param damageResolver                   weekly match damage resolver
     */
    public BossChronologyService(
        PlayerRepository playerRepository,
        PlayerChallengeContextFactory contextFactory,
        WeeklyChallengeSelectionService weeklyChallengeSelectionService,
        ChallengeDefinitionParser definitionParser,
        ChallengeProgressCalculatorRegistry calculatorRegistry,
        MatchDamageCalculator matchDamageCalculator,
        WeeklyMatchDamageResolver damageResolver
    ) {
        this.playerRepository = playerRepository;
        this.contextFactory = contextFactory;
        this.weeklyChallengeSelectionService = weeklyChallengeSelectionService;
        this.definitionParser = definitionParser;
        this.calculatorRegistry = calculatorRegistry;
        this.matchDamageCalculator = matchDamageCalculator;
        this.damageResolver = damageResolver;
    }

    /**
     * Replays one week's chronology to determine the boss's fate.
     *
     * @param weekStart   week being closed
     * @param ruleset     ruleset the week's boss encounter was resolved against
     * @param effectiveHp effective hit points frozen for the week
     * @return chronology outcome
     */
    @Transactional(readOnly = true)
    public BossChronologyResult computeChronology(
        LocalDate weekStart,
        ScoringRuleset ruleset,
        int effectiveHp
    ) {
        List<Player> activePlayers =
            playerRepository.findAllByStatusOrderByIdAsc(Player.COMPETITIVE_STATUS);
        List<WeeklyChallenge> weeklyChallenges =
            weeklyChallengeSelectionService.findExistingWeekChallenges(weekStart);

        Map<Player, PlayerChallengeContext> contextsByPlayer = new LinkedHashMap<>();
        for (Player player : activePlayers) {
            contextsByPlayer.put(player, contextFactory.create(player, weekStart));
        }

        Map<Long, DamageEvent> eventsByPlayerMatchId = new LinkedHashMap<>();
        seedMatchDamageEvents(contextsByPlayer, ruleset, eventsByPlayerMatchId);

        for (WeeklyChallenge weeklyChallenge : weeklyChallenges) {
            applyChallengeTriggerEvents(
                weeklyChallenge,
                contextsByPlayer,
                ruleset,
                eventsByPlayerMatchId
            );
        }

        return walkChronology(eventsByPlayerMatchId, effectiveHp);
    }

    /**
     * Seeds one damage event per valued match, before any challenge trigger is applied.
     *
     * <p>Only eligible matches get an event. A remake carries no damage and must not be able to become
     * the match credited with the finishing blow.
     *
     * @param contextsByPlayer      weekly match context per active player
     * @param ruleset               ruleset used to price each match
     * @param eventsByPlayerMatchId accumulator indexed by player-match identifier
     */
    private void seedMatchDamageEvents(
        Map<Player, PlayerChallengeContext> contextsByPlayer,
        ScoringRuleset ruleset,
        Map<Long, DamageEvent> eventsByPlayerMatchId
    ) {
        for (Map.Entry<Player, PlayerChallengeContext> entry : contextsByPlayer.entrySet()) {
            Map<Long, Integer> damageByPlayerMatchId =
                damageResolver.resolve(entry.getValue().playerMatches(), ruleset);

            for (PlayerMatch playerMatch : entry.getValue().playerMatches()) {
                if (!matchDamageCalculator.isEligible(playerMatch)) {
                    continue;
                }

                eventsByPlayerMatchId.put(
                    playerMatch.getId(),
                    new DamageEvent(
                        entry.getKey(),
                        playerMatch,
                        damageByPlayerMatchId.getOrDefault(playerMatch.getId(), 0)
                    )
                );
            }
        }
    }

    /**
     * Resolves, for one weekly challenge, which match completed it for each player, and credits that
     * match with the challenge damage and every team-bonus movement the completion caused.
     *
     * <p>Because the team bonus is retroactive, the k-th completion is worth its own tier plus the
     * uplift it hands back to the k-1 players already there. Crediting that uplift to the joining
     * player's own match is what keeps the timeline chronological while leaving the running total equal
     * to what the weekly ranking reports.
     *
     * @param weeklyChallenge       evaluated weekly challenge
     * @param contextsByPlayer      weekly match context per active player
     * @param ruleset               ruleset used to price the challenge and its team bonus
     * @param eventsByPlayerMatchId accumulator indexed by player-match identifier
     */
    private void applyChallengeTriggerEvents(
        WeeklyChallenge weeklyChallenge,
        Map<Player, PlayerChallengeContext> contextsByPlayer,
        ScoringRuleset ruleset,
        Map<Long, DamageEvent> eventsByPlayerMatchId
    ) {
        Challenge challenge = weeklyChallenge.getChallenge();
        ChallengeDefinition definition = definitionParser.parse(challenge);
        ChallengeProgressCalculator calculator = calculatorRegistry.getCalculator(definition.progressMode());

        List<PlayerMatch> creditedMatches = new ArrayList<>();

        for (PlayerChallengeContext context : contextsByPlayer.values()) {
            OptionalInt completionIndex = calculator.findFirstCompletionIndex(definition, context);

            if (completionIndex.isEmpty()) {
                continue;
            }

            PlayerMatch creditedMatch = findCreditedMatch(
                context,
                completionIndex.getAsInt(),
                eventsByPlayerMatchId
            );

            if (creditedMatch != null) {
                creditedMatches.add(creditedMatch);
            }
        }

        creditedMatches.sort(PLAYER_MATCH_CHRONOLOGICAL_ORDER);

        int baseDamage = ruleset.challengeDamage(challenge.getDifficulty());
        int completionCount = 0;
        int previousTeamBonus = 0;

        for (PlayerMatch creditedMatch : creditedMatches) {
            completionCount++;

            int teamBonus = ruleset.challengeTeamBonus(challenge.getDifficulty(), completionCount);
            int retroactiveUplift = (completionCount - 1) * (teamBonus - previousTeamBonus);

            eventsByPlayerMatchId
                .get(creditedMatch.getId())
                .addDamage(baseDamage + teamBonus + retroactiveUplift);

            previousTeamBonus = teamBonus;
        }
    }

    /**
     * Finds the match a completion's damage should be credited to.
     *
     * <p>Normally the completing match itself. A challenge can however be completed by a match that
     * carries no damage of its own — a remake still counts towards a "matches played" target — and such
     * a match has no event. The damage then falls back to the nearest valued match around it, so it
     * stays in the player's own timeline instead of being dropped.
     *
     * @param context               player's weekly match context
     * @param completionIndex       index of the match that completed the challenge
     * @param eventsByPlayerMatchId events seeded for valued matches only
     * @return match to credit, or {@code null} when the player played no valued match at all
     */
    private PlayerMatch findCreditedMatch(
        PlayerChallengeContext context,
        int completionIndex,
        Map<Long, DamageEvent> eventsByPlayerMatchId
    ) {
        List<PlayerMatch> playerMatches = context.playerMatches();

        for (int index = completionIndex; index >= 0; index--) {
            if (eventsByPlayerMatchId.containsKey(playerMatches.get(index).getId())) {
                return playerMatches.get(index);
            }
        }

        for (int index = completionIndex + 1; index < playerMatches.size(); index++) {
            if (eventsByPlayerMatchId.containsKey(playerMatches.get(index).getId())) {
                return playerMatches.get(index);
            }
        }

        return null;
    }

    /**
     * Walks the assembled chronology in order and finds the moment it crosses the effective hit points.
     *
     * @param eventsByPlayerMatchId assembled damage events, indexed by player-match identifier
     * @param effectiveHp           effective hit points frozen for the week
     * @return chronology outcome
     */
    private BossChronologyResult walkChronology(
        Map<Long, DamageEvent> eventsByPlayerMatchId,
        int effectiveHp
    ) {
        List<DamageEvent> orderedEvents = eventsByPlayerMatchId.values().stream()
            .sorted(Comparator.comparing(
                (DamageEvent event) -> event.playerMatch,
                PLAYER_MATCH_CHRONOLOGICAL_ORDER
            ))
            .toList();

        if (orderedEvents.isEmpty()) {
            return BossChronologyResult.UNTOUCHED;
        }

        // The walk never stops at the finishing blow: the total is what a surviving boss carries over
        // and what the health bar reports, so every event still has to be counted past that point.
        long cumulativeDamage = 0;
        DamageEvent finishingEvent = null;

        for (DamageEvent event : orderedEvents) {
            cumulativeDamage += event.damage;

            if (finishingEvent == null && cumulativeDamage >= effectiveHp) {
                finishingEvent = event;
            }
        }

        int totalDamage = (int) Math.min(cumulativeDamage, Integer.MAX_VALUE);

        if (finishingEvent == null) {
            return BossChronologyResult.survived(totalDamage);
        }

        return new BossChronologyResult(
            true,
            finishingEvent.player,
            finishingEvent.playerMatch,
            totalDamage
        );
    }

    /**
     * Orders matches by start time, then by the tracked player's match row identifier — the same
     * tie-break already used elsewhere in this codebase (see
     * {@code PlayerMatchRepository.findForChallengePeriod}).
     */
    private static final Comparator<PlayerMatch> PLAYER_MATCH_CHRONOLOGICAL_ORDER = Comparator
        .comparing((PlayerMatch playerMatch) -> playerMatch.getMatch().getStartedAt())
        .thenComparing(PlayerMatch::getId);

    /**
     * Mutable accumulation of damage dealt by one player-match, across the base match damage and every
     * challenge it happened to trigger.
     */
    private static final class DamageEvent {

        /**
         * Player this match belongs to.
         */
        private final Player player;

        /**
         * Match this event accumulates damage for.
         */
        private final PlayerMatch playerMatch;

        /**
         * Cumulative damage attributed to this match.
         */
        private int damage;

        private DamageEvent(Player player, PlayerMatch playerMatch, int damage) {
            this.player = player;
            this.playerMatch = playerMatch;
            this.damage = damage;
        }

        private void addDamage(int additionalDamage) {
            damage += additionalDamage;
        }
    }
}
