package io.github.thomashtn.valoquests.henrik.dto.match;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Verifies the deserialization of Henrik match-history responses.
 */
class HenrikMatchHistoryResponseTest {

    /**
     * Jackson mapper configured with Java time support.
     */
    private final ObjectMapper objectMapper =

        new ObjectMapper().findAndRegisterModules();

    /**
     * Verifies the deserialization of match metadata, including the queue and
     * Valorant season.
     *
     * @throws Exception when JSON deserialization fails
     */
    @Test
    void shouldDeserializeMatchMetadata() throws Exception {
        String json = """
            {
              "status": 200,
              "data": [
                {
                  "metadata": {
                    "match_id": "match-123",
                    "map": {
                      "id": "map-123",
                      "name": "Ascent"
                    },
                    "game_length_in_ms": 2100000,
                    "started_at": "2026-07-17T12:30:00Z",
                    "is_completed": true,
                    "queue": {
                      "id": "competitive",
                      "name": "Competitive",
                      "mode_type": "Standard"
                    },
                    "season": {
                      "id": "season-123",
                      "short": "V26 Act 4"
                    }
                  }
                }
              ]
            }
            """;

        HenrikMatchHistoryResponse response =
            objectMapper.readValue(
                json,
                HenrikMatchHistoryResponse.class
            );

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.data()).hasSize(1);

        HenrikMatchMetadata metadata =
            response.data().getFirst().metadata();

        assertThat(metadata.matchId())
            .isEqualTo("match-123");

        assertThat(metadata.map()).isNotNull();
        assertThat(metadata.map().id())
            .isEqualTo("map-123");
        assertThat(metadata.map().name())
            .isEqualTo("Ascent");

        assertThat(metadata.gameLengthInMilliseconds())
            .isEqualTo(2_100_000L);

        assertThat(metadata.startedAt())
            .isEqualTo(
                Instant.parse("2026-07-17T12:30:00Z")
            );

        assertThat(metadata.completed()).isTrue();

        assertThat(metadata.queue()).isNotNull();
        assertThat(metadata.queue().id())
            .isEqualTo("competitive");
        assertThat(metadata.queue().name())
            .isEqualTo("Competitive");
        assertThat(metadata.queue().modeType())
            .isEqualTo("Standard");

        assertThat(metadata.season()).isNotNull();
        assertThat(metadata.season().id())
            .isEqualTo("season-123");
        assertThat(metadata.season().shortName())
            .isEqualTo("V26 Act 4");
    }

    /**
     * Verifies that a missing root data property results in an empty match
     * list.
     *
     * @throws Exception when JSON deserialization fails
     */
    @Test
    void shouldUseEmptyListWhenDataIsMissing() throws Exception {
        String json = """
            {
              "status": 200
            }
            """;

        HenrikMatchHistoryResponse response =
            objectMapper.readValue(
                json,
                HenrikMatchHistoryResponse.class
            );

        assertThat(response.data()).isEmpty();
    }

    /**
     * Verifies that unknown Henrik properties do not break deserialization.
     *
     * @throws Exception when JSON deserialization fails
     */
    @Test
    void shouldIgnoreUnknownProperties() throws Exception {
        String json = """
            {
              "status": 200,
              "unknown_root_property": "ignored",
              "data": [
                {
                  "unknown_match_property": "ignored",
                  "metadata": {
                    "match_id": "match-123",
                    "unknown_metadata_property": "ignored"
                  }
                }
              ]
            }
            """;

        HenrikMatchHistoryResponse response =
            objectMapper.readValue(
                json,
                HenrikMatchHistoryResponse.class
            );

        assertThat(response.data()).hasSize(1);
        assertThat(
            response.data()
                .getFirst()
                .metadata()
                .matchId()
        ).isEqualTo("match-123");
    }

    /**
     * Verifies the deserialization of a match player and their statistics.
     *
     * @throws Exception when JSON deserialization fails
     */
    @Test
    void shouldDeserializeMatchPlayers() throws Exception {
        String json = """
            {
              "status": 200,
              "data": [
                {
                  "metadata": {
                    "match_id": "match-123"
                  },
                  "players": [
                    {
                      "puuid": "player-puuid",
                      "name": "Psilonnix",
                      "tag": "EUW",
                      "team_id": "Red",
                      "agent": {
                        "id": "agent-id",
                        "name": "Omen"
                      },
                      "stats": {
                        "score": 7250,
                        "kills": 24,
                        "deaths": 15,
                        "assists": 8,
                        "headshots": 18,
                        "bodyshots": 42,
                        "legshots": 3,
                        "damage": {
                          "dealt": 4350,
                          "received": 3025
                        }
                      },
                      "tier": {
                        "id": 27,
                        "name": "Ascendant 3"
                      }
                    }
                  ]
                }
              ]
            }
            """;

        HenrikMatchHistoryResponse response =
            objectMapper.readValue(
                json,
                HenrikMatchHistoryResponse.class
            );

        assertThat(response.data()).hasSize(1);
        assertThat(
            response.data().getFirst().players()
        ).hasSize(1);

        HenrikMatchPlayer player =
            response.data()
                .getFirst()
                .players()
                .getFirst();

        assertThat(player.puuid())
            .isEqualTo("player-puuid");
        assertThat(player.name())
            .isEqualTo("Psilonnix");
        assertThat(player.tag())
            .isEqualTo("EUW");
        assertThat(player.teamId())
            .isEqualTo("Red");

        assertThat(player.agent()).isNotNull();
        assertThat(player.agent().id())
            .isEqualTo("agent-id");
        assertThat(player.agent().name())
            .isEqualTo("Omen");

        assertThat(player.stats()).isNotNull();
        assertThat(player.stats().score())
            .isEqualTo(7250);
        assertThat(player.stats().kills())
            .isEqualTo(24);
        assertThat(player.stats().deaths())
            .isEqualTo(15);
        assertThat(player.stats().assists())
            .isEqualTo(8);
        assertThat(player.stats().headshots())
            .isEqualTo(18);
        assertThat(player.stats().bodyshots())
            .isEqualTo(42);
        assertThat(player.stats().legshots())
            .isEqualTo(3);

        assertThat(player.stats().damage())
            .isNotNull();
        assertThat(player.stats().damage().dealt())
            .isEqualTo(4350);
        assertThat(player.stats().damage().received())
            .isEqualTo(3025);

        assertThat(player.tier()).isNotNull();
        assertThat(player.tier().id())
            .isEqualTo(27);
        assertThat(player.tier().name())
            .isEqualTo("Ascendant 3");
    }

    /**
     * Verifies that a match without players exposes an empty player list.
     *
     * @throws Exception when JSON deserialization fails
     */
    @Test
    void shouldUseEmptyPlayerListWhenPlayersAreMissing()
        throws Exception {

        String json = """
            {
              "status": 200,
              "data": [
                {
                  "metadata": {
                    "match_id": "match-123"
                  }
                }
              ]
            }
            """;

        HenrikMatchHistoryResponse response =
            objectMapper.readValue(
                json,
                HenrikMatchHistoryResponse.class
            );

        assertThat(
            response.data()
                .getFirst()
                .players()
        ).isEmpty();
    }

    /**
     * Verifies the deserialization of teams and their round results.
     *
     * @throws Exception when JSON deserialization fails
     */
    @Test
    void shouldDeserializeMatchTeams() throws Exception {
        String json = """
            {
              "status": 200,
              "data": [
                {
                  "metadata": {
                    "match_id": "match-123"
                  },
                  "teams": [
                    {
                      "team_id": "Red",
                      "won": true,
                      "rounds": {
                        "won": 13,
                        "lost": 9
                      }
                    },
                    {
                      "team_id": "Blue",
                      "won": false,
                      "rounds": {
                        "won": 9,
                        "lost": 13
                      }
                    }
                  ]
                }
              ]
            }
            """;

        HenrikMatchHistoryResponse response =
            objectMapper.readValue(
                json,
                HenrikMatchHistoryResponse.class
            );

        assertThat(response.data()).hasSize(1);
        assertThat(
            response.data().getFirst().teams()
        ).hasSize(2);

        HenrikMatchTeam redTeam =
            response.data()
                .getFirst()
                .teams()
                .getFirst();

        assertThat(redTeam.teamId())
            .isEqualTo("Red");
        assertThat(redTeam.won())
            .isTrue();
        assertThat(redTeam.rounds())
            .isNotNull();
        assertThat(redTeam.rounds().won())
            .isEqualTo(13);
        assertThat(redTeam.rounds().lost())
            .isEqualTo(9);

        HenrikMatchTeam blueTeam =
            response.data()
                .getFirst()
                .teams()
                .get(1);

        assertThat(blueTeam.teamId())
            .isEqualTo("Blue");
        assertThat(blueTeam.won())
            .isFalse();
        assertThat(blueTeam.rounds())
            .isNotNull();
        assertThat(blueTeam.rounds().won())
            .isEqualTo(9);
        assertThat(blueTeam.rounds().lost())
            .isEqualTo(13);
    }

    /**
     * Verifies that a match without teams exposes an empty team list.
     *
     * @throws Exception when JSON deserialization fails
     */
    @Test
    void shouldUseEmptyTeamListWhenTeamsAreMissing()
        throws Exception {

        String json = """
            {
              "status": 200,
              "data": [
                {
                  "metadata": {
                    "match_id": "match-123"
                  }
                }
              ]
            }
            """;

        HenrikMatchHistoryResponse response =
            objectMapper.readValue(
                json,
                HenrikMatchHistoryResponse.class
            );

        assertThat(
            response.data()
                .getFirst()
                .teams()
        ).isEmpty();
    }

    /**
     * Verifies that null entries returned inside Henrik arrays are preserved
     * for the import layer while the exposed collection remains immutable.
     */
    @Test
    void shouldPreserveNullMatchEntriesInImmutableList() {
        HenrikMatchHistoryResponse response =
            new HenrikMatchHistoryResponse(
                200,
                java.util.Collections.singletonList(null)
            );

        assertThat(response.data())
            .hasSize(1)
            .containsExactly((HenrikMatchHistoryResponse.HenrikMatchData) null);
        assertThat(response.data())
            .isUnmodifiable();
    }

}
