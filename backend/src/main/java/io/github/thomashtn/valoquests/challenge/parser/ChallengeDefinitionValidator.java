package io.github.thomashtn.valoquests.challenge.parser;

import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.exception.InvalidChallengeDefinitionException;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCondition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeScope;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;

/**
 * Structural rules a parsed challenge definition must satisfy, per progress mode.
 *
 * <p>Kept apart from the JSON parsing so the rules read as a list: each progress mode has one
 * method naming what it requires of its single condition.</p>
 */
final class ChallengeDefinitionValidator {

    private ChallengeDefinitionValidator() {
    }

    /**
     * Validates every condition, the condition count and the progress-mode specific rules.
     *
     * @param challenge  challenge being validated, for error messages
     * @param definition parsed definition
     */
    static void validate(Challenge challenge, ChallengeDefinition definition) {
        for (ChallengeCondition condition : definition.conditions()) {
            validateCondition(challenge, condition);
        }

        validateConditionCount(challenge, definition);
        validateProgressMode(challenge, definition);
    }

    /**
     * Builds the exception raised for an invalid definition.
     *
     * @param challenge challenge being validated
     * @param message   what is wrong
     * @return the exception, with the challenge code in its message
     */
    static InvalidChallengeDefinitionException invalidDefinition(Challenge challenge, String message) {
        return new InvalidChallengeDefinitionException(buildErrorMessage(challenge, message));
    }

    /**
     * Builds the exception raised for an invalid definition, keeping the parsing failure as cause.
     *
     * @param challenge challenge being validated
     * @param message   what is wrong
     * @param cause     underlying failure
     * @return the exception, with the challenge code in its message
     */
    static InvalidChallengeDefinitionException invalidDefinition(
        Challenge challenge,
        String message,
        Throwable cause
    ) {
        return new InvalidChallengeDefinitionException(buildErrorMessage(challenge, message), cause);
    }

    private static void validateCondition(Challenge challenge, ChallengeCondition condition) {
        if (condition == null) {
            throw invalidDefinition(challenge, "A challenge condition must not be null.");
        }

        if (condition.metric() == null) {
            throw invalidDefinition(challenge, "Every condition must define a metric.");
        }

        if (condition.operator() == null) {
            throw invalidDefinition(challenge, "Every condition must define an operator.");
        }

        if (condition.target() == null || condition.target().signum() < 0) {
            throw invalidDefinition(challenge, "Every condition must define a non-negative target.");
        }
    }

    private static void validateConditionCount(Challenge challenge, ChallengeDefinition definition) {
        // ALL is the only mode that combines conditions, so it is the only one taking more than one.
        boolean combining = definition.progressMode() == ProgressMode.ALL;

        if (combining && definition.conditions().size() < 2) {
            throw invalidDefinition(challenge, "An ALL challenge must contain at least two conditions.");
        }

        if (!combining && definition.conditions().size() != 1) {
            throw invalidDefinition(
                challenge,
                "A challenge that is not an ALL challenge must contain exactly one condition."
            );
        }
    }

    private static void validateProgressMode(Challenge challenge, ChallengeDefinition definition) {
        switch (definition.progressMode()) {
            case SUM -> validateSum(challenge, definition);
            case DISTINCT_COUNT, MAX_GROUP -> validateGrouped(challenge, definition);
            case COUNT_MATCHES -> validateOccurrences(challenge, definition);
            case MAX_STREAK -> validateStreak(challenge, definition);
            case RATIO -> validateRatio(challenge, definition);
            case BASELINE -> validateBaseline(challenge, definition);
            // ALL delegates every condition to the mode each one declares, so it constrains nothing of
            // its own beyond the condition count already checked by validateConditionCount.
            case ALL -> { }
        }
    }

    private static void validateSum(Challenge challenge, ChallengeDefinition definition) {
        if (definition.singleCondition().groupBy() != null) {
            throw invalidDefinition(challenge, "SUM conditions must not define groupBy.");
        }
    }

    private static void validateGrouped(Challenge challenge, ChallengeDefinition definition) {
        if (definition.singleCondition().groupBy() == null) {
            throw invalidDefinition(challenge, definition.progressMode() + " requires a groupBy value.");
        }
    }

    private static void validateOccurrences(Challenge challenge, ChallengeDefinition definition) {
        ChallengeCondition condition = definition.singleCondition();

        if (condition.scope() != ChallengeScope.PER_MATCH) {
            throw invalidDefinition(challenge, "COUNT_MATCHES requires the PER_MATCH scope.");
        }

        if (condition.occurrences() == null || condition.occurrences() <= 0) {
            throw invalidDefinition(challenge, "COUNT_MATCHES requires a positive occurrences value.");
        }
    }

    private static void validateStreak(Challenge challenge, ChallengeDefinition definition) {
        ChallengeCondition condition = definition.singleCondition();

        if (condition.scope() != ChallengeScope.PER_MATCH) {
            throw invalidDefinition(challenge, "MAX_STREAK requires the PER_MATCH scope.");
        }

        if (condition.streak() == null || condition.streak() <= 0) {
            throw invalidDefinition(challenge, "MAX_STREAK requires a positive streak value.");
        }
    }

    private static void validateRatio(Challenge challenge, ChallengeDefinition definition) {
        ChallengeCondition condition = definition.singleCondition();

        if (condition.minimumMatches() != null && condition.minimumMatches() <= 0) {
            throw invalidDefinition(challenge, "minimumMatches must be positive when provided.");
        }
    }

    private static void validateBaseline(Challenge challenge, ChallengeDefinition definition) {
        ChallengeCondition condition = definition.singleCondition();

        if (condition.target() == null || condition.target().signum() <= 0) {
            throw invalidDefinition(challenge, "BASELINE requires a positive improvement target, in percent.");
        }

        if (condition.minimumMatches() == null || condition.minimumMatches() <= 0) {
            throw invalidDefinition(challenge, "BASELINE requires a positive minimumMatches value.");
        }
    }

    private static String buildErrorMessage(Challenge challenge, String message) {
        String challengeCode = challenge.getCode() == null ? "<unknown>" : challenge.getCode();

        return "Invalid challenge definition [" + challengeCode + "]: " + message;
    }
}
