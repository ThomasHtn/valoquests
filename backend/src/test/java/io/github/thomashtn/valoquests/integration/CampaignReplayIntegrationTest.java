package io.github.thomashtn.valoquests.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.thomashtn.valoquests.campaign.CampaignRuleset;
import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.entity.CampaignDailySnapshot;
import io.github.thomashtn.valoquests.campaign.entity.CampaignPlayer;
import io.github.thomashtn.valoquests.campaign.entity.CampaignPlayerDay;
import io.github.thomashtn.valoquests.campaign.entity.CampaignWeek;
import io.github.thomashtn.valoquests.campaign.entity.Guardian;
import io.github.thomashtn.valoquests.campaign.model.CampaignSchedule;
import io.github.thomashtn.valoquests.campaign.model.CampaignStatus;
import io.github.thomashtn.valoquests.campaign.model.CampaignTier;
import io.github.thomashtn.valoquests.campaign.model.CampaignWeekShape;
import io.github.thomashtn.valoquests.campaign.model.GuardianCategory;
import io.github.thomashtn.valoquests.campaign.repository.CampaignDailySnapshotRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignPlayerDayRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignPlayerRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignWeekRepository;
import io.github.thomashtn.valoquests.campaign.repository.GuardianRepository;
import io.github.thomashtn.valoquests.campaign.service.CampaignReplayService;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.entity.Season;
import io.github.thomashtn.valoquests.match.entity.ValorantMatch;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.GameModeSource;
import io.github.thomashtn.valoquests.match.model.MatchResult;
import io.github.thomashtn.valoquests.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valoquests.match.repository.SeasonRepository;
import io.github.thomashtn.valoquests.match.repository.ValorantMatchRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies the campaign replay against a real PostgreSQL, end to end.
 *
 * <p>What cannot be trusted from unit tests: that the whole campaign is deleted and written again
 * without leaving an orphan behind, that two replays of the same inputs produce the same rows, and
 * that every {@code NUMERIC} and {@code JSONB} column the schema declares accepts what the engine
 * computes.
 *
 * <p>The campaign is placed entirely in the past so the replay reads a fixed range whatever day the
 * suite runs on.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "app.admin-api-key=test-admin-key-0123456789abcdef0",
        "app.scheduling.standard-synchronization-enabled=false",
        "app.scheduling.week-rollover-enabled=false",
        "app.scheduling.campaign-tick-enabled=false"
    }
)
@Transactional
class CampaignReplayIntegrationTest extends PostgreSqlIntegrationTest {

    /**
     * Monday the campaign starts on, comfortably in the past.
     */
    private static final LocalDate FIRST_WEEK_START = LocalDate.of(2025, 1, 6);

    /**
     * Reference the campaign is calibrated at.
     */
    private static final int REFERENCE = 5_300;

    @Autowired
    private CampaignReplayService replayService;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CampaignPlayerRepository campaignPlayerRepository;

    @Autowired
    private CampaignWeekRepository campaignWeekRepository;

    @Autowired
    private CampaignDailySnapshotRepository snapshotRepository;

    @Autowired
    private CampaignPlayerDayRepository playerDayRepository;

    @Autowired
    private GuardianRepository guardianRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private SeasonRepository seasonRepository;

    @Autowired
    private ValorantMatchRepository valorantMatchRepository;

    @Autowired
    private PlayerMatchRepository playerMatchRepository;

    @Autowired
    private CampaignRuleset ruleset;

    private Campaign campaign;

    private Player operator;

    @BeforeEach
    void setUp() {
        operator = playerRepository.findAllByStatusOrderByIdAsc(PlayerStatus.ACTIVE).getFirst();
        campaign = openCampaign();
    }

    @Test
    @DisplayName("Writes one day per day of the campaign and settles its ten weeks")
    void shouldWriteEveryDayAndSettleEveryWeek() {
        playMatches(FIRST_WEEK_START.plusDays(1), 4);

        replayService.replay(campaign);

        List<CampaignDailySnapshot> days = snapshotRepository.findAllByCampaignIdOrderByDayAsc(campaign.getId());
        assertThat(days).hasSize(CampaignSchedule.WEEK_COUNT * 7);
        assertThat(days.getFirst().getDay()).isEqualTo(FIRST_WEEK_START);
        assertThat(days.getLast().getDay()).isEqualTo(campaign.finalDay());

        assertThat(campaignWeekRepository.findAllByCampaignIdOrderByWeekIndexAsc(campaign.getId()))
            .hasSize(CampaignSchedule.WEEK_COUNT)
            .allSatisfy(week -> assertThat(week.isSettled()).isTrue());
    }

    @Test
    @DisplayName("Grows the base from the matches it was fed, and only from those")
    void shouldGrowTheBaseFromTheMatches() {
        playMatches(FIRST_WEEK_START.plusDays(1), 2);

        replayService.replay(campaign);

        CampaignDailySnapshot playedDay = snapshotRepository
            .findByCampaignIdAndDay(campaign.getId(), FIRST_WEEK_START.plusDays(1))
            .orElseThrow();

        assertThat(playedDay.getDamage()).isPositive();
        assertThat(playedDay.getPresenceCount()).isEqualTo(1);
        assertThat(playedDay.getGrowth()).isEqualByComparingTo(
            BigDecimal.valueOf(playedDay.getDamage() / CampaignRuleset.DAMAGE_PER_INHABITANT)
                .setScale(3, java.math.RoundingMode.HALF_UP)
        );

        assertThat(snapshotRepository.findByCampaignIdAndDay(campaign.getId(), FIRST_WEEK_START).orElseThrow())
            .satisfies(quietDay -> {
                assertThat(quietDay.getDamage()).isZero();
                assertThat(quietDay.getPresenceCount()).isZero();
            });
    }

    @Test
    @DisplayName("Stores what each operator produced on each day they played")
    void shouldStoreTheOperatorDays() {
        playMatches(FIRST_WEEK_START.plusDays(1), 3);

        replayService.replay(campaign);

        List<CampaignPlayerDay> operatorDays =
            playerDayRepository.findAllByCampaignIdAndPlayerIdOrderByDayAsc(campaign.getId(), operator.getId());

        assertThat(operatorDays).singleElement().satisfies(day -> {
            assertThat(day.getDay()).isEqualTo(FIRST_WEEK_START.plusDays(1));
            assertThat(day.getMatchCount()).isEqualTo(3);
            assertThat(day.getDamage()).isEqualTo(day.getFood() + day.getComponents());
            assertThat(day.getStreakDays()).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("Produces the same rows on a second replay of the same inputs")
    void shouldBeIdempotent() {
        playMatches(FIRST_WEEK_START.plusDays(1), 4);

        replayService.replay(campaign);
        List<String> first = fingerprint();

        replayService.replay(campaign);
        List<String> second = fingerprint();

        assertThat(second).isEqualTo(first);
        assertThat(snapshotRepository.findAllByCampaignIdOrderByDayAsc(campaign.getId()))
            .hasSize(CampaignSchedule.WEEK_COUNT * 7);
        assertThat(playerDayRepository.findAllByCampaignIdAndPlayerIdOrderByDayAsc(
            campaign.getId(),
            operator.getId()
        )).hasSize(1);
    }

    @Test
    @DisplayName("Leaves every guardian standing on a campaign nobody played")
    void shouldLeaveEveryGuardianStanding() {
        replayService.replay(campaign);

        assertThat(campaignWeekRepository.findAllByCampaignIdOrderByWeekIndexAsc(campaign.getId()))
            .allSatisfy(week -> {
                assertThat(week.isDefeated()).isFalse();
                assertThat(week.getDamageDealt()).isZero();
                assertThat(week.rescued()).isZero();
            });

        assertThat(snapshotRepository.findAllByCampaignIdOrderByDayAsc(campaign.getId()).getLast()
            .getPopulation()).isEqualByComparingTo("0.000");
    }

    /**
     * Opens a campaign whose ten weeks all sit in the past.
     *
     * @return the persisted campaign
     */
    private Campaign openCampaign() {
        Campaign opened = new Campaign();
        opened.setNumber(1);
        opened.setStatus(CampaignStatus.RUNNING);
        opened.setOpenedAt(Instant.parse("2025-01-03T10:00:00Z"));
        opened.setFirstWeekStart(FIRST_WEEK_START);
        opened.setLastWeekStart(FIRST_WEEK_START.plusWeeks(CampaignSchedule.WEEK_COUNT - 1L));
        opened.setRosterSize(1);
        opened.setReference(REFERENCE);
        opened.setTier(CampaignTier.NORMAL);
        opened.setVolumeFactor(BigDecimal.ONE);
        opened.setSkillAnchorsJson("{}");
        opened.setCalibrationWindowMonths(9);
        opened.setCalibrationFirstDay(FIRST_WEEK_START.minusMonths(9));
        opened = campaignRepository.save(opened);

        CampaignPlayer member = new CampaignPlayer();
        member.setCampaign(opened);
        member.setPlayer(operator);
        campaignPlayerRepository.save(member);

        List<Guardian> guardians = guardianRepository.findAll();

        for (CampaignWeekShape shape : CampaignSchedule.weeks()) {
            CampaignWeek week = new CampaignWeek();
            week.setCampaign(opened);
            week.setWeekIndex(shape.weekIndex());
            week.setWeekStart(FIRST_WEEK_START.plusWeeks(shape.weekIndex() - 1L));
            week.setPlanetName(shape.planetName());
            week.setCategory(GuardianCategory.MINOR);
            week.setGuardianWeight(BigDecimal.valueOf(shape.guardianWeight()));
            week.setGroupWeight(BigDecimal.valueOf(shape.groupWeight()));
            week.setGuardian(guardians.get(shape.weekIndex() - 1));
            week.setGuardianHitPoints(ruleset.guardianHitPoints(REFERENCE, shape.guardianWeight(), 1));
            week.setWoundedCount(ruleset.groupSize(REFERENCE, shape.groupWeight(), 1, 100));
            campaignWeekRepository.save(week);
        }

        return opened;
    }

    /**
     * Plays a number of competitive matches on one day.
     *
     * @param day    day the matches are played on
     * @param played number of matches
     */
    private void playMatches(LocalDate day, int played) {
        Season season = new Season();
        season.setExternalId("campaign-replay-season");
        season.setName("Campaign Replay Season");
        season.setActive(true);
        season = seasonRepository.save(season);

        for (int index = 0; index < played; index++) {
            Instant startedAt = day.atStartOfDay(ZoneOffset.UTC).toInstant().plusSeconds(3_600L * (index + 10));

            ValorantMatch match = new ValorantMatch();
            match.setExternalMatchId("campaign-replay-match-" + index);
            match.setSeason(season);
            match.setStartedAt(startedAt);
            match.setDurationSeconds(2_400);
            match.setMapId("ascent");
            match.setMapName("Ascent");
            match.setGameMode(GameMode.COMPETITIVE);
            match.setGameModeSource(GameModeSource.PROVIDED);
            match.setQueueId("competitive");
            match.setRedScore(13);
            match.setBlueScore(10);
            match = valorantMatchRepository.save(match);

            PlayerMatch playerMatch = new PlayerMatch();
            playerMatch.setPlayer(operator);
            playerMatch.setMatch(match);
            playerMatch.setTeamId("Blue");
            playerMatch.setAgentName("Omen");
            playerMatch.setResult(MatchResult.WIN);
            playerMatch.setKills(20);
            playerMatch.setDeaths(10);
            playerMatch.setAssists(5);
            playerMatch.setScore(5_000);
            playerMatch.setHeadshots(8);
            playerMatch.setBodyshots(20);
            playerMatch.setLegshots(0);
            playerMatch.setDamageDealt(4_000);
            playerMatch.setRoundsPlayed(23);
            playerMatch.setMvp(false);
            playerMatchRepository.save(playerMatch);
        }
    }

    /**
     * Reduces the campaign's stored rows to a comparable fingerprint.
     *
     * @return one line per day and per week, in order
     */
    private List<String> fingerprint() {
        List<String> lines = snapshotRepository.findAllByCampaignIdOrderByDayAsc(campaign.getId()).stream()
            .map(day -> day.getDay() + "|" + day.getPopulation() + "|" + day.getFoodStock()
                + "|" + day.getComponentsStock() + "|" + day.getDamage())
            .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));

        campaignWeekRepository.findAllByCampaignIdOrderByWeekIndexAsc(campaign.getId()).forEach(week ->
            lines.add(week.getWeekIndex() + "|" + week.getDamageDealt() + "|" + week.isDefeated()
                + "|" + week.rescued() + "|" + week.getLimiter() + "|" + week.getBaseLoss()));

        return lines;
    }
}
