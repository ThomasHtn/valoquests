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
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.scoring.ScoringRuleset;
import io.github.thomashtn.valoquests.scoring.service.MatchDamageCalculator;
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
 * to, the damage of any challenge that match happened to complete and the team bonus tier that
 * completion reached at that moment — resolved by {@link ChallengeProgressCalculator#findSustainedCompletionIndex}
 * so no calculator implementation had to change. The regularity bonus never enters this timeline: it is
 * applied at closure, after this calculation, so it can never be the decisive action.
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
     * Resolves whether one match is valued and how much damage it deals.
     */
    private final MatchDamageCalculator matchDamageCalculator;

    /**
     * Creates the boss chronology service.
     *
     * @param playerRepository                player repository
     * @param contextFactory                  player challenge context factory
     * @param weeklyChallengeSelectionService  weekly challenge selection service
     * @param definitionParser                challenge definition parser
     * @param calculatorRegistry               challenge calculator registry
     * @param matchDamageCalculator            match damage calculator
     */
    public BossChronologyService(
        PlayerRepository playerRepository,
        PlayerChallengeContextFactory contextFactory,
        WeeklyChallengeSelectionService weeklyChallengeSelectionService,
        ChallengeDefinitionParser definitionParser,
        ChallengeProgressCalculatorRegistry calculatorRegistry,
        MatchDamageCalculator matchDamageCalculator
    ) {
        this.playerRepository = playerRepository;
        this.contextFactory = contextFactory;
        this.weeklyChallengeSelectionService = weeklyChallengeSelectionService;
        this.definitionParser = definitionParser;
        this.calculatorRegistry = calculatorRegistry;
        this.matchDamageCalculator = matchDamageCalculator;
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
            playerRepository.findAllByStatusOrderByIdAsc(PlayerStatus.ACTIVE);
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
            for (PlayerMatch playerMatch : entry.getValue().playerMatches()) {
                int matchDamage = matchDamageCalculator.damageOf(playerMatch, ruleset);
                eventsByPlayerMatchId.put(
                    playerMatch.getId(),
                    new DamageEvent(entry.getKey(), playerMatch, matchDamage)
                );
            }
        }
    }

    /**
     * Resolves, for one weekly challenge, which match triggered it for each player, and adds the
     * challenge damage and arrival-order team bonus to that match's event.
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

        List<PlayerMatch> triggeringMatches = new ArrayList<>();

        for (PlayerChallengeContext context : contextsByPlayer.values()) {
            OptionalInt sustainedIndex = calculator.findSustainedCompletionIndex(definition, context);

            sustainedIndex.ifPresent(
                index -> triggeringMatches.add(context.playerMatches().get(index))
            );
        }

        triggeringMatches.sort(PLAYER_MATCH_CHRONOLOGICAL_ORDER);

        int arrivalRank = 0;
        for (PlayerMatch triggeringMatch : triggeringMatches) {
            arrivalRank++;

            int challengeDamage = ruleset.challengeDamage(challenge.getDifficulty())
                + ruleset.teamBonus(arrivalRank);

            eventsByPlayerMatchId.get(triggeringMatch.getId()).addDamage(challengeDamage);
        }
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

        long cumulativeDamage = 0;
        for (DamageEvent event : orderedEvents) {
            cumulativeDamage += event.damage;

            if (cumulativeDamage >= effectiveHp) {
                return new BossChronologyResult(true, event.player, event.playerMatch);
            }
        }

        return BossChronologyResult.SURVIVED;
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
