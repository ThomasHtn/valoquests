package io.github.thomashtn.valoquests.ranking.service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * Standard competition ranking (1, 1, 3): equal scores share a position and the next one skips ahead.
 */
final class CompetitionRanking {

    /**
     * Not instantiable: static helpers only.
     */
    private CompetitionRanking() {
    }

    /**
     * Assigns a position to each row of an already ordered list.
     *
     * @param ordered rows in ranking order, best first
     * @param ranked  tells whether a row takes a position at all
     * @param score   reads the score equal rows share a position on
     * @param <T>     row type
     * @return one position per row, in the same order, {@code null} for a row that is not ranked
     */
    static <T> List<Integer> positions(List<T> ordered, Predicate<T> ranked, ToIntFunction<T> score) {
        List<Integer> positions = new ArrayList<>(ordered.size());
        int rank = 0;
        int position = 0;
        Integer previousScore = null;

        for (T row : ordered) {
            if (!ranked.test(row)) {
                positions.add(null);
                continue;
            }
            rank++;
            int rowScore = score.applyAsInt(row);
            if (previousScore == null || rowScore != previousScore) {
                position = rank;
                previousScore = rowScore;
            }
            positions.add(position);
        }

        return positions;
    }
}
