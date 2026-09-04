package io.github.thomashtn.valoquests.campaign.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies the written shape of a campaign: ten weeks, two peaks, and a catalogue big enough to
 * fill each weight class without repeating a guardian.
 */
class CampaignScheduleTest {

    @Test
    @DisplayName("Holds exactly ten weeks, numbered one to ten")
    void shouldHoldTenNumberedWeeks() {
        assertThat(CampaignSchedule.weeks()).hasSize(CampaignSchedule.WEEK_COUNT);
        assertThat(CampaignSchedule.weeks())
            .extracting(CampaignWeekShape::weekIndex)
            .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    @DisplayName("Spends two minor weeks, six standard ones and two elite peaks")
    void shouldSpendTheDocumentedWeightClasses() {
        assertThat(CampaignSchedule.weeks())
            .filteredOn(week -> week.category() == GuardianCategory.MINOR)
            .extracting(CampaignWeekShape::weekIndex)
            .containsExactly(1, 6);

        assertThat(CampaignSchedule.weeks())
            .filteredOn(week -> week.category() == GuardianCategory.ELITE)
            .extracting(CampaignWeekShape::weekIndex)
            .containsExactly(5, 10);

        assertThat(CampaignSchedule.weeks())
            .filteredOn(week -> week.category() == GuardianCategory.STANDARD)
            .hasSize(6);
    }

    @Test
    @DisplayName("Gives every week a planet of its own")
    void shouldNameTenDistinctPlanets() {
        assertThat(CampaignSchedule.weeks())
            .extracting(CampaignWeekShape::planetName)
            .doesNotHaveDuplicates()
            .hasSize(CampaignSchedule.WEEK_COUNT);
    }

    @Test
    @DisplayName("Ends on the biggest group behind the biggest guardian")
    void shouldEndOnTheHardestWeek() {
        CampaignWeekShape last = CampaignSchedule.weeks().getLast();

        assertThat(last.guardianWeight())
            .isEqualTo(CampaignSchedule.weeks().stream()
                .mapToDouble(CampaignWeekShape::guardianWeight).max().orElseThrow());
        assertThat(last.groupWeight())
            .isEqualTo(CampaignSchedule.weeks().stream()
                .mapToDouble(CampaignWeekShape::groupWeight).max().orElseThrow());
    }
}
