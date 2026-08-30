package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.dto.ChallengeCatalogueResponse;
import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import io.github.thomashtn.valoquests.challenge.parser.ChallengeDefinitionParser;
import io.github.thomashtn.valoquests.challenge.repository.ChallengeRepository;
import io.github.thomashtn.valoquests.colony.ColonyRuleset;
import io.github.thomashtn.valoquests.scoring.ScoringRuleset;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides the enabled challenge catalogue exposed by the catalogue endpoint.
 */
@Service
@Transactional(readOnly = true)
public class DefaultChallengeCatalogueQueryService implements ChallengeCatalogueQueryService {

    /**
     * Repository used to retrieve the challenge catalogue.
     */
    private final ChallengeRepository challengeRepository;

    /**
     * Parser used to expose typed challenge-definition values.
     */
    private final ChallengeDefinitionParser definitionParser;

    /**
     * Barèmes saying what a challenge of each difficulty is worth.
     */
    private final ScoringRuleset ruleset;

    /**
     * Calibration saying what a challenge of each difficulty hands the colony.
     *
     * <p>Read here rather than derived by the client, for the same reason
     * {@link DefaultChallengeQueryService} reads it: the colony prices a challenge from the very
     * damage this class already advertises, and a client doing that division itself would carry a
     * second copy of the rule.
     */
    private final ColonyRuleset colonyRuleset;

    /**
     * Creates the challenge-catalogue query service.
     *
     * @param challengeRepository challenge catalogue repository
     * @param definitionParser    challenge-definition parser
     * @param ruleset              scoring ruleset
     * @param colonyRuleset        colony ruleset pricing a challenge in materials
     */
    public DefaultChallengeCatalogueQueryService(
        ChallengeRepository challengeRepository,
        ChallengeDefinitionParser definitionParser,
        ScoringRuleset ruleset,
        ColonyRuleset colonyRuleset
    ) {
        this.challengeRepository = challengeRepository;
        this.definitionParser = definitionParser;
        this.ruleset = ruleset;
        this.colonyRuleset = colonyRuleset;
    }

    /**
     * Returns every challenge eligible for weekly selection, independent of any one week's draw.
     *
     * <p>Damage and materials are resolved the same way {@link DefaultChallengeQueryService}
     * resolves them for a drawn week — from the difficulty alone through the rulesets, never
     * stored on the challenge itself — so a catalogue entry and that same challenge once drawn
     * always agree.
     *
     * @return the enabled challenge catalogue, ordered by identifier
     */
    @Override
    public ChallengeCatalogueResponse findCatalogue() {
        List<ChallengeCatalogueResponse.ChallengeCatalogueEntry> entries = challengeRepository
            .findAllByEnabledTrueOrderByIdAsc()
            .stream()
            .map(this::toCatalogueEntry)
            .toList();

        return new ChallengeCatalogueResponse(entries);
    }

    /**
     * Converts one catalogue challenge into an API response.
     *
     * @param challenge catalogue challenge to convert
     * @return catalogue entry
     */
    private ChallengeCatalogueResponse.ChallengeCatalogueEntry toCatalogueEntry(Challenge challenge) {
        ChallengeDefinition definition = definitionParser.parse(challenge);
        ChallengeDifficulty difficulty = challenge.getDifficulty();

        return new ChallengeCatalogueResponse.ChallengeCatalogueEntry(
            challenge.getId(),
            challenge.getName(),
            challenge.getDescription(),
            difficulty,
            resolveMetricLabel(definition),
            resolveTargetValue(definition),
            ruleset.challengeDamage(difficulty),
            colonyRuleset.materialsForChallenge(difficulty)
        );
    }

    /**
     * Builds the metric label exposed by the endpoint.
     *
     * @param definition parsed challenge definition
     * @return distinct metric names joined in definition order
     */
    private String resolveMetricLabel(ChallengeDefinition definition) {
        // Mirrors DefaultChallengeQueryService#resolveMetricLabel: a progression challenge and an
        // absolute one can share a metric while asking opposite things, and the suffix is what
        // lets the client tell those two chips apart.
        String suffix = definition.progressMode() == ProgressMode.BASELINE ? "_PROGRESS" : "";

        return definition.conditions().stream()
            .map(condition -> condition.metric().name() + suffix)
            .distinct()
            .collect(Collectors.joining(" + "));
    }

    /**
     * Resolves the target displayed for a catalogue entry.
     *
     * @param definition parsed challenge definition
     * @return target value for a simple challenge, or {@code null} for a composite one — the
     * catalogue has no week's progress rows to fall back on the way the current-week endpoint does
     * for a drawn challenge
     */
    private BigDecimal resolveTargetValue(ChallengeDefinition definition) {
        return definition.conditions().size() == 1
            ? definition.singleCondition().target()
            : null;
    }
}
