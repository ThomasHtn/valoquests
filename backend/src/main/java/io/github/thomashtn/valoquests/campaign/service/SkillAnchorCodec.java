package io.github.thomashtn.valoquests.campaign.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.challenge.model.ChallengeScaling;
import io.github.thomashtn.valoquests.challenge.model.SkillAnchor;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Reads and writes a campaign's skill anchors as the JSON column stores them.
 *
 * <p>Stored as JSON rather than as ten columns: the anchor list follows the challenge catalogue,
 * which is content, and adding a metric to the catalogue must not need a migration on the campaign
 * table. The anchors are written once at opening and only ever read back afterwards.
 */
@Component
public class SkillAnchorCodec {

    /**
     * Shape the column is decoded into.
     */
    private static final TypeReference<Map<SkillAnchor, BigDecimal>> ANCHOR_MAP = new TypeReference<>() {
    };

    /**
     * Mapper used for both directions.
     */
    private final ObjectMapper objectMapper;

    /**
     * Creates the codec.
     *
     * @param objectMapper object mapper
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The ObjectMapper is a Spring-managed singleton, thread-safe and not owned by this class."
    )
    public SkillAnchorCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Serializes one set of anchors.
     *
     * @param anchors anchors to store
     * @return the JSON the column holds
     */
    public String toJson(Map<SkillAnchor, BigDecimal> anchors) {
        return objectMapper.writeValueAsString(anchors);
    }

    /**
     * Rebuilds a campaign's scaling from what it stored at opening.
     *
     * @param volumeFactor     stored volume factor
     * @param skillAnchorsJson stored anchors
     * @return the scaling the challenge targets are resolved against
     * @throws IllegalStateException when the stored anchors cannot be read
     */
    public ChallengeScaling toScaling(BigDecimal volumeFactor, String skillAnchorsJson) {
        try {
            Map<SkillAnchor, BigDecimal> anchors = objectMapper.readValue(skillAnchorsJson, ANCHOR_MAP);

            return new ChallengeScaling(volumeFactor, anchors);
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                "The campaign's stored skill anchors cannot be read: " + exception.getMessage(),
                exception
            );
        }
    }
}
