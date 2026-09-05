package io.github.thomashtn.valoquests.challenge.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.thomashtn.valoquests.challenge.model.ChallengeCondition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeGameMode;
import io.github.thomashtn.valoquests.challenge.model.ChallengeMetric;
import io.github.thomashtn.valoquests.challenge.model.ChallengeOperator;
import io.github.thomashtn.valoquests.challenge.model.ChallengeScope;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ChallengeDescriptionResolver}: the copy must read the resolved numbers.
 */
class ChallengeDescriptionResolverTest {

    /**
     * A cumulative target is rewritten in place.
     */
    @Test
    void rewritesCumulativeTarget() {
        ChallengeDefinition base = sum(cumulative(ChallengeMetric.ROUNDS_PLAYED, "55"));
        ChallengeDefinition resolved = sum(cumulative(ChallengeMetric.ROUNDS_PLAYED, "20"));

        String rewritten = ChallengeDescriptionResolver.resolve(
            "Jouer 55 rounds en Compétitif ou Non classé.", base, resolved
        );

        assertThat(rewritten).isEqualTo("Jouer 20 rounds en Compétitif ou Non classé.");
    }

    /**
     * A per-match condition names its occurrences before its target.
     */
    @Test
    void rewritesOccurrencesThenPerMatchTarget() {
        ChallengeDefinition base = count(perMatch(ChallengeMetric.KILLS, "16", 5));
        ChallengeDefinition resolved = count(perMatch(ChallengeMetric.KILLS, "15", 2));

        String rewritten = ChallengeDescriptionResolver.resolve(
            "Terminer 5 parties en Compétitif ou Non classé avec 16 kills ou plus.", base, resolved
        );

        assertThat(rewritten).isEqualTo("Terminer 2 parties en Compétitif ou Non classé avec 15 kills ou plus.");
    }

    /**
     * Thousands keep their French grouping and ratios keep their decimals.
     */
    @Test
    void keepsFrenchNumberFormatting() {
        ChallengeDefinition scoreBase = sum(cumulative(ChallengeMetric.SCORE, "12000"));
        ChallengeDefinition scoreResolved = sum(cumulative(ChallengeMetric.SCORE, "5000"));
        ChallengeDefinition ratioBase = count(perMatch(ChallengeMetric.KD, "0.9", 3));
        ChallengeDefinition ratioResolved = count(perMatch(ChallengeMetric.KD, "1.25", 3));

        assertThat(ChallengeDescriptionResolver.resolve(
            "Cumuler 12 000 de score en Compétitif ou Non classé.", scoreBase, scoreResolved
        )).isEqualTo("Cumuler 5 000 de score en Compétitif ou Non classé.");
        assertThat(ChallengeDescriptionResolver.resolve(
            "Terminer 3 parties avec un K/D de 0,90 ou plus.", ratioBase, ratioResolved
        )).isEqualTo("Terminer 3 parties avec un K/D de 1,25 ou plus.");
    }

    /**
     * Several conditions are rewritten in declaration order.
     */
    @Test
    void rewritesEveryCondition() {
        ChallengeDefinition base = new ChallengeDefinition(1, ProgressMode.ALL, List.of(
            cumulative(ChallengeMetric.KILLS, "300"),
            cumulative(ChallengeMetric.KILLS, "150")
        ));
        ChallengeDefinition resolved = new ChallengeDefinition(1, ProgressMode.ALL, List.of(
            cumulative(ChallengeMetric.KILLS, "120"),
            cumulative(ChallengeMetric.KILLS, "60")
        ));

        String rewritten = ChallengeDescriptionResolver.resolve(
            "Réaliser 300 kills en Deathmatch et 150 en Team Deathmatch.", base, resolved
        );

        assertThat(rewritten).isEqualTo("Réaliser 120 kills en Deathmatch et 60 en Team Deathmatch.");
    }

    /**
     * A count that resolves to one puts the words it counts in the singular.
     */
    @Test
    void agreesUnitsWithACountOfOne() {
        ChallengeDefinition winsBase = sum(cumulative(ChallengeMetric.MATCHES_WON, "3"));
        ChallengeDefinition winsResolved = sum(cumulative(ChallengeMetric.MATCHES_WON, "1"));
        ChallengeDefinition gamesBase = count(perMatch(ChallengeMetric.KILLS, "20", 5));
        ChallengeDefinition gamesResolved = count(perMatch(ChallengeMetric.KILLS, "20", 1));

        assertThat(ChallengeDescriptionResolver.resolve(
            "Remporter 3 parties en Compétitif ou Non classé.", winsBase, winsResolved
        )).isEqualTo("Remporter 1 partie en Compétitif ou Non classé.");
        assertThat(ChallengeDescriptionResolver.resolve(
            "Terminer 5 parties compétitives avec 20 kills ou plus.", gamesBase, gamesResolved
        )).isEqualTo("Terminer 1 partie compétitive avec 20 kills ou plus.");
    }

    /**
     * A number that is not the expected base is left alone, and an unscaled copy is unchanged.
     */
    @Test
    void leavesUnexpectedNumbersAndUnscaledCopyUntouched() {
        ChallengeDefinition base = sum(cumulative(ChallengeMetric.MATCHES_WON, "4"));
        ChallengeDefinition resolved = sum(cumulative(ChallengeMetric.MATCHES_WON, "2"));

        assertThat(ChallengeDescriptionResolver.resolve(
            "Remporter une partie 7 jours sur 7.", base, resolved
        )).isEqualTo("Remporter une partie 7 jours sur 7.");
        assertThat(ChallengeDescriptionResolver.resolve(
            "Remporter 4 parties compétitives.", base, base
        )).isEqualTo("Remporter 4 parties compétitives.");
    }

    private static ChallengeDefinition sum(ChallengeCondition condition) {
        return new ChallengeDefinition(1, ProgressMode.SUM, List.of(condition));
    }

    private static ChallengeDefinition count(ChallengeCondition condition) {
        return new ChallengeDefinition(1, ProgressMode.COUNT_MATCHES, List.of(condition));
    }

    private static ChallengeCondition cumulative(ChallengeMetric metric, String target) {
        return new ChallengeCondition(
            metric, ChallengeOperator.GTE, new BigDecimal(target), ChallengeGameMode.ANY,
            null, null, null, null, null
        );
    }

    private static ChallengeCondition perMatch(ChallengeMetric metric, String target, int occurrences) {
        return new ChallengeCondition(
            metric, ChallengeOperator.GTE, new BigDecimal(target), ChallengeGameMode.ANY,
            null, ChallengeScope.PER_MATCH, occurrences, null, null
        );
    }
}
