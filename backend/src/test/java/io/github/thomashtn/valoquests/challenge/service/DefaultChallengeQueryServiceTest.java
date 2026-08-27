package io.github.thomashtn.valoquests.challenge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.challenge.dto.CurrentChallengesResponse;
import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCondition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.challenge.model.ChallengeMetric;
import io.github.thomashtn.valoquests.challenge.model.ChallengeOperator;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import io.github.thomashtn.valoquests.challenge.parser.ChallengeDefinitionParser;
import io.github.thomashtn.valoquests.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valoquests.challenge.repository.WeeklyChallengeRepository;
import io.github.thomashtn.valoquests.colony.DefaultColonyRuleset;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.scoring.DefaultScoringRuleset;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link DefaultChallengeQueryService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Current challenge queries")
class DefaultChallengeQueryServiceTest {

    /**
     * Monday of the week under test.
     */
    private static final LocalDate WEEK_START = LocalDate.of(2026, 7, 13);

    /**
     * Instant inside that week.
     */
    private static final Instant MIDWEEK = Instant.parse("2026-07-15T12:00:00Z");

    @Mock
    private WeeklyChallengeRepository weeklyChallengeRepository;

    @Mock
    private PlayerChallengeProgressRepository progressRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private ChallengeDefinitionParser definitionParser;

    private DefaultChallengeQueryService service;

    @BeforeEach
    void setUp() {
        DefaultScoringRuleset scoringRuleset = new DefaultScoringRuleset();

        service = new DefaultChallengeQueryService(
            weeklyChallengeRepository,
            progressRepository,
            playerRepository,
            definitionParser,
            scoringRuleset,
            new DefaultColonyRuleset(scoringRuleset),
            new WeekCalendar(Clock.fixed(MIDWEEK, ZoneOffset.UTC), ZoneOffset.UTC)
        );
    }

    /**
     * Wires the repositories for one week's pack, progress rows and player count.
     */
    private void given(
        List<WeeklyChallenge> pack,
        List<PlayerChallengeProgress> progress,
        long activePlayers
    ) {
        when(weeklyChallengeRepository
            .findAllByWeekStartAndFinalizedAtIsNullOrderByIdAsc(WEEK_START))
            .thenReturn(pack);
        when(progressRepository
            .findAllByWeeklyChallengeWeekStartOrderByPlayerIdAscWeeklyChallengeIdAsc(WEEK_START))
            .thenReturn(progress);
        when(playerRepository.countByStatus(PlayerStatus.ACTIVE)).thenReturn(activePlayers);
        when(playerRepository.findLatestSuccessfulSynchronizationAt())
            .thenReturn(Optional.of(MIDWEEK));
    }

    @Test
    @DisplayName("reports the week as a Monday-to-Sunday span with the last synchronization")
    void shouldReportTheWeekAndLastSynchronization() {
        given(List.of(), List.of(), 6);

        CurrentChallengesResponse response = service.findCurrent();

        assertThat(response.weekStart()).isEqualTo(WEEK_START);
        assertThat(response.weekEnd()).isEqualTo(LocalDate.of(2026, 7, 19));
        assertThat(response.lastSuccessfulSynchronizationAt()).isEqualTo(MIDWEEK);
        assertThat(response.challenges()).isEmpty();
    }

    @Test
    @DisplayName("counts only completed players and expresses them as a percentage of the group")
    void shouldCountOnlyCompletedPlayersAsAPercentageOfTheGroup() {
        WeeklyChallenge challenge = weeklyChallenge(10L, "Kill them all");
        given(
            List.of(challenge),
            List.of(
                progress(1L, challenge, true),
                progress(2L, challenge, true),
                progress(3L, challenge, false)
            ),
            6
        );
        when(definitionParser.parse(challenge.getChallenge()))
            .thenReturn(definition(ChallengeMetric.KILLS, BigDecimal.valueOf(50)));

        CurrentChallengesResponse.ChallengeProgressResponse response =
            service.findCurrent().challenges().getFirst();

        assertThat(response.completedPlayers()).isEqualTo(2);
        assertThat(response.totalPlayers()).isEqualTo(6);
        // 2 of 6 players.
        assertThat(response.completionPercentage()).isEqualByComparingTo("33.33");
    }

    @Test
    @DisplayName("reports zero completion rather than dividing by an empty roster")
    void shouldReportZeroCompletionForAnEmptyRoster() {
        WeeklyChallenge challenge = weeklyChallenge(10L, "Kill them all");
        given(List.of(challenge), List.of(), 0);
        when(definitionParser.parse(challenge.getChallenge()))
            .thenReturn(definition(ChallengeMetric.KILLS, BigDecimal.valueOf(50)));

        CurrentChallengesResponse.ChallengeProgressResponse response =
            service.findCurrent().challenges().getFirst();

        assertThat(response.totalPlayers()).isZero();
        assertThat(response.completionPercentage()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("exposes the challenge catalogue fields alongside its target")
    void shouldExposeTheCatalogueFieldsAlongsideItsTarget() {
        WeeklyChallenge challenge = weeklyChallenge(10L, "Kill them all");
        given(List.of(challenge), List.of(), 6);
        when(definitionParser.parse(challenge.getChallenge()))
            .thenReturn(definition(ChallengeMetric.KILLS, BigDecimal.valueOf(50)));

        CurrentChallengesResponse.ChallengeProgressResponse response =
            service.findCurrent().challenges().getFirst();

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("Kill them all");
        assertThat(response.description()).isEqualTo("Kill them all description");
        assertThat(response.difficulty()).isEqualTo(ChallengeDifficulty.MEDIUM);
        assertThat(response.damage()).isEqualTo(2_200);
        assertThat(response.metric()).isEqualTo("KILLS");
        assertThat(response.targetValue()).isEqualByComparingTo("50");

        // The other half of what a challenge is worth: the damage moves the ranking and the fight, the
        // materials move the town. Derived from that same damage, so the two can never disagree.
        assertThat(response.materials()).isEqualTo(22);
    }

    @Test
    @DisplayName("takes a composite challenge's target from a stored progress row")
    void shouldTakeACompositeTargetFromAStoredProgressRow() {
        WeeklyChallenge challenge = weeklyChallenge(10L, "Do both");
        PlayerChallengeProgress stored = progress(1L, challenge, false);
        stored.setTargetValue(BigDecimal.valueOf(120));

        given(List.of(challenge), List.of(stored), 6);
        when(definitionParser.parse(challenge.getChallenge()))
            .thenReturn(new ChallengeDefinition(
                3,
                ProgressMode.ALL,
                List.of(
                    condition(ChallengeMetric.KILLS, BigDecimal.TEN),
                    condition(ChallengeMetric.ASSISTS, BigDecimal.ONE)
                )
            ));

        CurrentChallengesResponse.ChallengeProgressResponse response =
            service.findCurrent().challenges().getFirst();

        assertThat(response.metric()).isEqualTo("KILLS + ASSISTS");
        assertThat(response.targetValue()).isEqualByComparingTo("120");
    }

    @Test
    @DisplayName("leaves a composite target unset when no progress row carries one")
    void shouldLeaveACompositeTargetUnsetWithoutAStoredRow() {
        WeeklyChallenge challenge = weeklyChallenge(10L, "Do both");
        given(List.of(challenge), List.of(), 6);
        when(definitionParser.parse(challenge.getChallenge()))
            .thenReturn(new ChallengeDefinition(
                3,
                ProgressMode.ALL,
                List.of(
                    condition(ChallengeMetric.KILLS, BigDecimal.TEN),
                    condition(ChallengeMetric.ASSISTS, BigDecimal.ONE)
                )
            ));

        assertThat(service.findCurrent().challenges().getFirst().targetValue()).isNull();
    }

    @Test
    @DisplayName("attributes each progress row to its own challenge")
    void shouldAttributeEachProgressRowToItsOwnChallenge() {
        WeeklyChallenge easy = weeklyChallenge(10L, "Easy");
        WeeklyChallenge hard = weeklyChallenge(11L, "Hard");

        given(
            List.of(easy, hard),
            List.of(
                progress(1L, easy, true),
                progress(2L, easy, true),
                progress(1L, hard, false)
            ),
            4
        );
        when(definitionParser.parse(easy.getChallenge()))
            .thenReturn(definition(ChallengeMetric.KILLS, BigDecimal.TEN));
        when(definitionParser.parse(hard.getChallenge()))
            .thenReturn(definition(ChallengeMetric.HEADSHOTS, BigDecimal.valueOf(99)));

        assertThat(service.findCurrent().challenges())
            .extracting(
                CurrentChallengesResponse.ChallengeProgressResponse::id,
                CurrentChallengesResponse.ChallengeProgressResponse::completedPlayers
            )
            .containsExactly(tuple(10L, 2), tuple(11L, 0));
    }

    private WeeklyChallenge weeklyChallenge(long id, String name) {
        Challenge challenge = new Challenge();
        challenge.setName(name);
        challenge.setDescription(name + " description");
        challenge.setDifficulty(ChallengeDifficulty.MEDIUM);

        WeeklyChallenge weeklyChallenge = new WeeklyChallenge();
        weeklyChallenge.setId(id);
        weeklyChallenge.setChallenge(challenge);
        weeklyChallenge.setWeekStart(WEEK_START);
        return weeklyChallenge;
    }

    private PlayerChallengeProgress progress(
        long playerId,
        WeeklyChallenge weeklyChallenge,
        boolean completed
    ) {
        Player player = new Player();
        player.setId(playerId);

        PlayerChallengeProgress progress = new PlayerChallengeProgress();
        progress.setPlayer(player);
        progress.setWeeklyChallenge(weeklyChallenge);
        progress.setCompleted(completed);
        return progress;
    }

    private ChallengeDefinition definition(ChallengeMetric metric, BigDecimal target) {
        return new ChallengeDefinition(
            3,
            ProgressMode.SUM,
            List.of(condition(metric, target))
        );
    }

    private ChallengeCondition condition(ChallengeMetric metric, BigDecimal target) {
        return new ChallengeCondition(
            metric,
            ChallengeOperator.GTE,
            target,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }
}
