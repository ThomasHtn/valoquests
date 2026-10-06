package io.github.thomashtn.valoquests.campaign.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.campaign.CampaignFixtures;
import io.github.thomashtn.valoquests.campaign.entity.CampaignWeek;
import io.github.thomashtn.valoquests.campaign.entity.Guardian;
import io.github.thomashtn.valoquests.campaign.exception.CampaignLifecycleException;
import io.github.thomashtn.valoquests.campaign.model.CampaignSchedule;
import io.github.thomashtn.valoquests.campaign.model.CampaignStatus;
import io.github.thomashtn.valoquests.campaign.model.GuardianCategory;
import io.github.thomashtn.valoquests.campaign.model.NewCampaign;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.scoring.model.CampaignDifficulty;
import io.github.thomashtn.valoquests.scoring.service.DefaultScoringRuleset;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifies that opening a campaign decides the whole of its ten weeks, once.
 */
@ExtendWith(MockitoExtension.class)
class CampaignFactoryTest {

    /**
     * Monday the campaign starts on.
     */
    private static final LocalDate FIRST_WEEK_START = CampaignFixtures.FIRST_WEEK_START;

    /**
     * Friday the campaign is opened on, before its first Monday.
     */
    private static final LocalDate OPENING_DAY = FIRST_WEEK_START.minusDays(3);

    /**
     * Roster the fixtures freeze.
     */
    private static final List<Player> ROSTER = IntStream.rangeClosed(1, 7)
        .mapToObj(index -> CampaignFixtures.player(index, "Op" + index))
        .toList();

    @Mock
    private GuardianRepositoryStub guardianRepository;

    @Mock
    private PlayerRepository playerRepository;

    private CampaignFactory factory;

    @BeforeEach
    void setUp() {
        factory = new CampaignFactory(
            guardianRepository,
            playerRepository,
            new DefaultScoringRuleset(),
            Clock.fixed(CampaignFixtures.OPENED_AT, ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("Freezes the roster, the difficulty and the ten weeks in one go")
    void shouldBuildTheWholeCampaign() {
        givenActiveRoster();
        stockCatalogue(6, 10, 6);

        NewCampaign built = factory.build(3, CampaignDifficulty.AMATEUR, FIRST_WEEK_START, OPENING_DAY);

        assertThat(built.campaign().getNumber()).isEqualTo(3);
        assertThat(built.campaign().getStatus()).isEqualTo(CampaignStatus.OPENED);
        assertThat(built.campaign().getOpenedAt()).isEqualTo(CampaignFixtures.OPENED_AT);
        assertThat(built.campaign().getFirstWeekStart()).isEqualTo(FIRST_WEEK_START);
        assertThat(built.campaign().getLastWeekStart()).isEqualTo(FIRST_WEEK_START.plusWeeks(9));
        assertThat(built.campaign().getRosterSize()).isEqualTo(7);
        assertThat(built.campaign().reference()).isEqualTo(CampaignFixtures.REFERENCE);
        assertThat(built.campaign().getDifficulty()).isEqualTo(CampaignDifficulty.AMATEUR);
        assertThat(built.roster()).hasSize(7);
        assertThat(built.weeks()).hasSize(CampaignSchedule.WEEK_COUNT);
    }

    @Test
    @DisplayName("Starts a campaign right away when its first Monday is today")
    void shouldStartACampaignOpenedOnItsFirstMonday() {
        givenActiveRoster();
        stockCatalogue(6, 10, 6);

        NewCampaign built = factory.build(1, CampaignDifficulty.AMATEUR, FIRST_WEEK_START, FIRST_WEEK_START);

        assertThat(built.campaign().getStatus()).isEqualTo(CampaignStatus.RUNNING);
    }

    @Test
    @DisplayName("Refuses to open a campaign nobody is active for")
    void shouldRefuseAnEmptyRoster() {
        when(playerRepository.findAllByStatusOrderByIdAsc(PlayerStatus.ACTIVE)).thenReturn(List.of());

        assertThatThrownBy(() -> factory.build(1, CampaignDifficulty.AMATEUR, FIRST_WEEK_START, OPENING_DAY))
            .isInstanceOf(CampaignLifecycleException.class)
            .hasMessageContaining("No player is active");
    }

    @Test
    @DisplayName("Sizes each week's guardian and group from the reference and the roster")
    void shouldSizeEveryWeek() {
        givenActiveRoster();
        stockCatalogue(6, 10, 6);

        NewCampaign built = factory.build(1, CampaignDifficulty.AMATEUR, FIRST_WEEK_START, OPENING_DAY);
        CampaignWeek first = built.weeks().getFirst();
        CampaignWeek last = built.weeks().getLast();

        assertThat(first.getWeekStart()).isEqualTo(FIRST_WEEK_START);
        assertThat(first.getGuardianHitPoints()).isEqualTo(24_486);
        assertThat(first.getWoundedCount()).isEqualTo(1_855);
        assertThat(last.getWeekStart()).isEqualTo(FIRST_WEEK_START.plusWeeks(9));
        assertThat(last.getCategory()).isEqualTo(GuardianCategory.ELITE);
        assertThat(last.getWoundedCount()).isGreaterThan(first.getWoundedCount());
    }

    @Test
    @DisplayName("Never draws the same guardian twice inside one campaign")
    void shouldDrawEveryGuardianOnlyOnce() {
        givenActiveRoster();
        stockCatalogue(6, 10, 6);

        NewCampaign built = factory.build(1, CampaignDifficulty.AMATEUR, FIRST_WEEK_START, OPENING_DAY);

        assertThat(built.weeks())
            .extracting(week -> week.getGuardian().getCode())
            .doesNotHaveDuplicates();
        assertThat(built.weeks())
            .allSatisfy(week -> assertThat(week.getGuardian().getCategory()).isEqualTo(week.getCategory()));
    }

    @Test
    @DisplayName("Draws the same guardians for the same campaign number")
    void shouldDrawReproducibly() {
        givenActiveRoster();
        stockCatalogue(6, 10, 6);

        List<String> first = codesOf(factory.build(4, CampaignDifficulty.AMATEUR, FIRST_WEEK_START, OPENING_DAY));
        List<String> second = codesOf(factory.build(4, CampaignDifficulty.AMATEUR, FIRST_WEEK_START, OPENING_DAY));

        assertThat(first).isEqualTo(second);
    }

    @Test
    @DisplayName("Refuses to open on a catalogue too small to fill a weight class")
    void shouldRefuseAThinCatalogue() {
        givenActiveRoster();
        // Only the classes the draw reaches before it gives up: it walks minor, then standard.
        when(guardianRepository.findAllByEnabledTrueAndCategoryOrderByIdAsc(GuardianCategory.MINOR))
            .thenReturn(entries(GuardianCategory.MINOR, 0, 6));
        when(guardianRepository.findAllByEnabledTrueAndCategoryOrderByIdAsc(GuardianCategory.STANDARD))
            .thenReturn(entries(GuardianCategory.STANDARD, 100, 3));

        assertThatThrownBy(() -> factory.build(1, CampaignDifficulty.AMATEUR, FIRST_WEEK_START, OPENING_DAY))
            .isInstanceOf(CampaignLifecycleException.class)
            .hasMessageContaining("STANDARD");
    }

    /**
     * Declares the fixtures' roster as the players active today.
     */
    private void givenActiveRoster() {
        when(playerRepository.findAllByStatusOrderByIdAsc(PlayerStatus.ACTIVE)).thenReturn(ROSTER);
    }

    /**
     * Stocks the catalogue with a number of entries per weight class.
     *
     * @param minor    minor entries
     * @param standard standard entries
     * @param elite    elite entries
     */
    private void stockCatalogue(int minor, int standard, int elite) {
        when(guardianRepository.findAllByEnabledTrueAndCategoryOrderByIdAsc(GuardianCategory.MINOR))
            .thenReturn(entries(GuardianCategory.MINOR, 0, minor));
        when(guardianRepository.findAllByEnabledTrueAndCategoryOrderByIdAsc(GuardianCategory.STANDARD))
            .thenReturn(entries(GuardianCategory.STANDARD, 100, standard));
        when(guardianRepository.findAllByEnabledTrueAndCategoryOrderByIdAsc(GuardianCategory.ELITE))
            .thenReturn(entries(GuardianCategory.ELITE, 200, elite));
    }

    /**
     * Builds a run of catalogue entries.
     *
     * @param category weight class
     * @param offset   identifier offset keeping the codes unique across classes
     * @param count    entries to build
     * @return the entries
     */
    private List<Guardian> entries(GuardianCategory category, int offset, int count) {
        List<Guardian> guardians = new ArrayList<>(count);

        for (int index = 1; index <= count; index++) {
            guardians.add(CampaignFixtures.guardian(offset + index, category));
        }

        return guardians;
    }

    /**
     * Reads the guardian codes a campaign drew, week one first.
     *
     * @param built campaign that was built
     * @return the codes in week order
     */
    private List<String> codesOf(NewCampaign built) {
        return built.weeks().stream().map(week -> week.getGuardian().getCode()).toList();
    }

    /**
     * Marker interface letting Mockito stub the repository without a Spring context.
     */
    interface GuardianRepositoryStub
        extends io.github.thomashtn.valoquests.campaign.repository.GuardianRepository {
    }
}
