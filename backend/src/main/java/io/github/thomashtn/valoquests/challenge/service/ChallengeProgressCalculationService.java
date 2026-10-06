package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.calculator.ChallengeProgressCalculator;
import io.github.thomashtn.valoquests.challenge.calculator.ChallengeProgressCalculatorRegistry;
import io.github.thomashtn.valoquests.challenge.calculator.ChallengeProgressResult;
import io.github.thomashtn.valoquests.challenge.calculator.PlayerChallengeContext;
import io.github.thomashtn.valoquests.challenge.calculator.PlayerChallengeContextFactory;
import io.github.thomashtn.valoquests.challenge.entity.ChallengeSelection;
import io.github.thomashtn.valoquests.challenge.model.CalculatedProgress;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.parser.ChallengeDefinitionParser;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.scoring.model.ChallengeCadence;
import io.github.thomashtn.valoquests.shared.time.WeekCalendar;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Calculates a player's progress on selected challenges, from the matches already stored.
 *
 * <p>Always evaluates the definition resolved at draw time, never the catalogue's base one.
 */
@Service
public class ChallengeProgressCalculationService {

    /**
     * Application logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ChallengeProgressCalculationService.class);

    /**
     * Parser used to convert persisted JSON rules into typed definitions.
     */
    private final ChallengeDefinitionParser definitionParser;

    /**
     * Registry used to select the appropriate progress calculator.
     */
    private final ChallengeProgressCalculatorRegistry calculatorRegistry;

    /**
     * Factory loading each player's matches of one week.
     */
    private final PlayerChallengeContextFactory contextFactory;

    /**
     * Calendar resolving the instants a daily selection's day spans.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the challenge calculation service.
     *
     * @param definitionParser   persisted challenge-definition parser
     * @param calculatorRegistry progress calculator registry
     * @param contextFactory     player challenge context factory
     * @param weekCalendar       calendar resolving the instants a day spans
     */
    public ChallengeProgressCalculationService(
        ChallengeDefinitionParser definitionParser,
        ChallengeProgressCalculatorRegistry calculatorRegistry,
        PlayerChallengeContextFactory contextFactory,
        WeekCalendar weekCalendar
    ) {
        this.definitionParser = definitionParser;
        this.calculatorRegistry = calculatorRegistry;
        this.contextFactory = contextFactory;
        this.weekCalendar = weekCalendar;
    }

    /**
     * Calculates one player's progress on every selection of one week.
     *
     * <p>The week's matches are loaded once. A daily selection is evaluated over its own day only,
     * carved out of them.
     *
     * @param player     player being recalculated
     * @param weekStart  Monday identifying the week
     * @param selections the week's selections, weekly pack and daily draws
     * @return one calculated progress per selection, in selection order
     */
    public List<CalculatedProgress> calculateWeek(
        Player player,
        LocalDate weekStart,
        List<ChallengeSelection> selections
    ) {
        PlayerChallengeContext weekContext = contextFactory.create(player, weekStart);

        LOGGER.debug(
            "Calculating {} challenge(s) for player {} using {} match(es).",
            selections.size(),
            player.getDisplayName(),
            weekContext.playerMatches().size()
        );

        return selections.stream()
            .map(selection -> new CalculatedProgress(
                selection,
                calculateOverItsPeriod(player, selection, weekContext)
            ))
            .toList();
    }

    /**
     * Calculates the progress of one player for one selected challenge.
     *
     * @param selection weekly or daily selection, with its resolved conditions
     * @param context   player challenge calculation context over the selection's period
     * @return calculated challenge progress
     */
    public ChallengeProgressResult calculate(
        ChallengeSelection selection,
        PlayerChallengeContext context
    ) {
        Objects.requireNonNull(
            selection,
            "Selection must not be null."
        );

        Objects.requireNonNull(
            context,
            "Player challenge context must not be null."
        );

        ChallengeDefinition definition =
            definitionParser.parse(selection);

        ChallengeProgressCalculator calculator =
            calculatorRegistry.getCalculator(
                definition.progressMode()
            );

        return calculator.calculate(
            definition,
            context
        );
    }

    /**
     * Calculates and logs one selection's result for a player, over the selection's own period.
     *
     * @param player      player being recalculated
     * @param selection   evaluated selection
     * @param weekContext the player's matches of the whole week
     * @return calculated progress result
     */
    private ChallengeProgressResult calculateOverItsPeriod(
        Player player,
        ChallengeSelection selection,
        PlayerChallengeContext weekContext
    ) {
        PlayerChallengeContext periodContext = weekContext;

        if (selection.getCadence() == ChallengeCadence.DAILY) {
            LocalDate day = selection.getDay();
            periodContext = weekContext.restrictedTo(
                weekCalendar.startOfDay(day),
                weekCalendar.endOfDay(day)
            );
        }

        ChallengeProgressResult result = calculate(selection, periodContext);

        LOGGER.debug(
            "Calculated challenge {} for player {}: current={}, "
                + "target={}, completed={}.",
            selection.getChallenge().getCode(),
            player.getDisplayName(),
            result.currentValue(),
            result.targetValue(),
            result.completed()
        );

        return result;
    }
}
