package io.github.thomashtn.valoquests.ranking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCadence;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCondition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.challenge.model.ChallengeMetric;
import io.github.thomashtn.valoquests.challenge.model.ChallengeOperator;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import io.github.thomashtn.valoquests.challenge.parser.ChallengeDefinitionParser;
import io.github.thomashtn.valoquests.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valoquests.challenge.repository.WeeklyChallengeRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.ranking.RankingFixtures;
import io.github.thomashtn.valoquests.ranking.dto.CurrentRankingResponse.ChallengeProgressResponse;
import io.github.thomashtn.valoquests.ranking.service.RankingProgressMapper.WeekBoard;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifies the board laid out under the ranking: one line per player and per challenge shown.
 */
@ExtendWith(MockitoExtension.class)
class RankingProgressMapperTest {

    /**
     * Monday of the week.
     */
    private static final LocalDate WEEK_START = RankingFixtures.WEEK_START;

    /**
     * Day whose daily challenge sits next to the pack.
     */
    private static final LocalDate TODAY = WEEK_START.plusDays(2);

    /**
     * Player with rows.
     */
    private static final Player ALPHA = RankingFixtures.player(1, "Alpha", PlayerStatus.ACTIVE);

    /**
     * Player without any row yet.
     */
    private static final Player BRAVO = RankingFixtures.player(2, "Bravo", PlayerStatus.ACTIVE);

    @Mock
    private WeeklyChallengeRepository weeklyChallengeRepository;

    @Mock
    private PlayerChallengeProgressRepository progressRepository;

    @Mock
    private ChallengeDefinitionParser definitionParser;

    @Mock
    private ChallengePointsReader challengePointsReader;

    @InjectMocks
    private RankingProgressMapper mapper;

    private WeeklyChallenge weekly;

    private WeeklyChallenge todaysDaily;

    @BeforeEach
    void setUp() {
        weekly = selection(10, "KILLS_100", ChallengeCadence.WEEKLY, ChallengeDifficulty.MEDIUM, null);
        todaysDaily = selection(11, "DAILY_HS", ChallengeCadence.DAILY, null, TODAY);
        WeeklyChallenge yesterdaysDaily = selection(12, "DAILY_OLD", ChallengeCadence.DAILY, null, TODAY.minusDays(1));

        when(weeklyChallengeRepository.findAllByWeekStartOrderByIdAsc(WEEK_START))
            .thenReturn(List.of(weekly, todaysDaily, yesterdaysDaily));
        when(challengePointsReader.referenceFor(WEEK_START)).thenReturn(2_000);
        when(challengePointsReader.pointsOf(any(), anyInt(), anyInt())).thenReturn(54);
    }

    @Test
    @DisplayName("Shows the pack and today's daily to every player, at zero when not evaluated yet")
    void shouldLayOutOneLinePerChallengeShown() {
        when(definitionParser.parse(any(WeeklyChallenge.class)))
            .thenReturn(definition(condition(ChallengeMetric.KILLS, 100)));
        PlayerChallengeProgress alphaWeekly = progress(ALPHA, weekly, "40", true);
        when(progressRepository.findAllByWeeklyChallengeWeekStartOrderByPlayerIdAscWeeklyChallengeIdAsc(WEEK_START))
            .thenReturn(List.of(alphaWeekly));

        WeekBoard board = mapper.forWeek(WEEK_START, TODAY, List.of(ALPHA.getId(), BRAVO.getId()));

        assertThat(board.weeklyChallengeCount()).isEqualTo(1);
        assertThat(board.of(ALPHA.getId())).extracting(ChallengeProgressResponse::id).containsExactly(10L, 11L);

        ChallengeProgressResponse alphaLine = board.of(ALPHA.getId()).getFirst();
        assertThat(alphaLine.code()).isEqualTo("KILLS_100");
        assertThat(alphaLine.cadence()).isEqualTo(ChallengeCadence.WEEKLY);
        assertThat(alphaLine.difficulty()).isEqualTo(ChallengeDifficulty.MEDIUM);
        assertThat(alphaLine.metric()).isEqualTo("KILLS");
        assertThat(alphaLine.unit()).isEqualTo("kills");
        assertThat(alphaLine.currentValue()).isEqualByComparingTo("40");
        assertThat(alphaLine.targetValue()).isEqualByComparingTo("100");
        assertThat(alphaLine.completed()).isTrue();
        assertThat(alphaLine.rankingPoints()).isEqualTo(54);

        ChallengeProgressResponse bravoDaily = board.of(BRAVO.getId()).get(1);
        assertThat(bravoDaily.id()).isEqualTo(11L);
        assertThat(bravoDaily.day()).isEqualTo(TODAY);
        assertThat(bravoDaily.currentValue()).isEqualByComparingTo("0");
        assertThat(bravoDaily.targetValue()).isEqualByComparingTo("100");
        assertThat(bravoDaily.completed()).isFalse();
    }

    @Test
    @DisplayName("Joins the metrics of a composite challenge and leaves its unit out")
    void shouldJoinCompositeMetrics() {
        when(definitionParser.parse(any(WeeklyChallenge.class))).thenReturn(new ChallengeDefinition(
            3,
            ProgressMode.ALL,
            List.of(condition(ChallengeMetric.KILLS, 10), condition(ChallengeMetric.ASSISTS, 10))
        ));
        when(progressRepository.findAllByWeeklyChallengeWeekStartOrderByPlayerIdAscWeeklyChallengeIdAsc(WEEK_START))
            .thenReturn(List.of());

        ChallengeProgressResponse line = mapper.forWeek(WEEK_START, TODAY, List.of(ALPHA.getId()))
            .of(ALPHA.getId())
            .getFirst();

        assertThat(line.metric()).isEqualTo("KILLS + ASSISTS");
        assertThat(line.unit()).isNull();
        assertThat(line.targetValue()).isEqualByComparingTo("20");
    }

    private static ChallengeDefinition definition(ChallengeCondition... conditions) {
        return new ChallengeDefinition(3, ProgressMode.SUM, List.of(conditions));
    }

    private static ChallengeCondition condition(ChallengeMetric metric, int target) {
        return new ChallengeCondition(
            metric,
            ChallengeOperator.GTE,
            BigDecimal.valueOf(target),
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    private static WeeklyChallenge selection(
        long id,
        String code,
        ChallengeCadence cadence,
        ChallengeDifficulty difficulty,
        LocalDate day
    ) {
        Challenge challenge = new Challenge();
        challenge.setCode(code);
        challenge.setName(code);
        challenge.setCadence(cadence);
        challenge.setDifficulty(difficulty);

        WeeklyChallenge selection = new WeeklyChallenge();
        selection.setId(id);
        selection.setWeekStart(WEEK_START);
        selection.setCadence(cadence);
        selection.setDay(day);
        selection.setChallenge(challenge);

        return selection;
    }

    private static PlayerChallengeProgress progress(
        Player player,
        WeeklyChallenge selection,
        String current,
        boolean completed
    ) {
        PlayerChallengeProgress progress = new PlayerChallengeProgress();
        progress.setPlayer(player);
        progress.setWeeklyChallenge(selection);
        progress.setCurrentValue(new BigDecimal(current));
        progress.setTargetValue(BigDecimal.valueOf(100));
        progress.setCompleted(completed);

        return progress;
    }
}
