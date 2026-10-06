package io.github.thomashtn.valoquests.henrik.dto.match;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the root response returned by the Henrik match-history endpoint.
 *
 * <p>The Henrik API wraps the retrieved matches inside a {@code data} array.
 * Unknown properties are ignored so that additional fields introduced by
 * Henrik do not break deserialization.</p>
 *
 * @param data matches returned for the requested player
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HenrikMatchHistoryResponse(

    List<HenrikMatchData> data
) {

    /**
     * Normalizes the match collection while preserving possible null entries
     * returned by the remote API.
     *
     * <p>{@link List#copyOf(java.util.Collection)} cannot be used here because
     * it rejects null elements. Null match entries are intentionally preserved
     * so that the import layer can count and log them as rejected data instead
     * of failing during DTO construction.</p>
     */
    public HenrikMatchHistoryResponse {
        data = immutableNullableElementList(data);
    }

    /**
     * Returns the immutable match collection created by the canonical constructor.
     *
     * @return immutable matches returned by Henrik
     */
    @Override
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "The canonical constructor stores an unmodifiable defensive copy."
    )
    public List<HenrikMatchData> data() {
        return data;
    }

    /**
     * Represents one match returned by Henrik.
     *
     * @param metadata general information about the match
     * @param players players who participated in the match
     * @param teams teams and final result of the match
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HenrikMatchData(

        HenrikMatchMetadata metadata,
        List<HenrikMatchPlayer> players,
        List<HenrikMatchTeam> teams
    ) {

        /**
         * Normalizes nested collections while preserving possible null
         * entries returned by the remote API.
         */
        public HenrikMatchData {
            players = immutableNullableElementList(players);
            teams = immutableNullableElementList(teams);
        }

        /**
         * Returns the immutable player collection created by the canonical constructor.
         *
         * @return immutable match players
         */
        @Override
        @SuppressFBWarnings(
            value = "EI_EXPOSE_REP",
            justification = "The canonical constructor stores an unmodifiable defensive copy."
        )
        public List<HenrikMatchPlayer> players() {
            return players;
        }

        /**
         * Returns the immutable team collection created by the canonical constructor.
         *
         * @return immutable match teams
         */
        @Override
        @SuppressFBWarnings(
            value = "EI_EXPOSE_REP",
            justification = "The canonical constructor stores an unmodifiable defensive copy."
        )
        public List<HenrikMatchTeam> teams() {
            return teams;
        }

        /**
         * Returns the identifier of the season the match was played in.
         *
         * @return the season identifier, {@code null} when absent or blank
         */
        public String seasonId() {
            if (metadata == null || metadata.season() == null) {
                return null;
            }
            String id = metadata.season().id();
            return id == null || id.isBlank() ? null : id;
        }
    }

    /**
     * Returns an immutable defensive copy that accepts null elements.
     *
     * @param values source collection, possibly {@code null}
     * @param <T> element type
     * @return an immutable empty list when the source is null, otherwise an
     *         immutable defensive copy preserving every element
     */
    private static <T> List<T> immutableNullableElementList(List<T> values) {
        if (values == null) {
            return List.of();
        }

        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}
