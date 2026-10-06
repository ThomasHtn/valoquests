package io.github.thomashtn.valoquests.henrik.dto.mmr;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Verifies the deserialization of Henrik MMR responses.
 */
class HenrikMmrResponseTest {

    /**
     * Verifies that the rank survives a payload carrying fields the record no longer declares.
     */
    @Test
    @DisplayName("Reads the tier name and rank rating, ignoring the status, the tier id and the elo")
    void shouldReadTheRankIgnoringUndeclaredFields() {
        String json = """
            {
              "status": 200,
              "data": {
                "current": {
                  "tier": { "id": 22, "name": "Diamond 2" },
                  "rr": 73,
                  "elo": 1873
                }
              }
            }
            """;

        HenrikMmrResponse response = JsonMapper.builder().build().readValue(json, HenrikMmrResponse.class);

        assertThat(response.data().current().tier().name()).isEqualTo("Diamond 2");
        assertThat(response.data().current().rankRating()).isEqualTo(73);
    }
}
