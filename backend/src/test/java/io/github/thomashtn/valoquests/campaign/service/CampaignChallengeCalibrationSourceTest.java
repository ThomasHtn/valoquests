package io.github.thomashtn.valoquests.campaign.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.campaign.CampaignFixtures;
import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.model.CampaignStatus;
import io.github.thomashtn.valoquests.campaign.repository.CampaignRepository;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCalibration;
import io.github.thomashtn.valoquests.scoring.DefaultScoringRuleset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

/**
 * Verifies what challenges are priced against, in a campaign and between two of them.
 */
@ExtendWith(MockitoExtension.class)
class CampaignChallengeCalibrationSourceTest {

    @Mock
    private CampaignRepository campaignRepository;

    private CampaignChallengeCalibrationSource source;

    @BeforeEach
    void setUp() {
        source = new CampaignChallengeCalibrationSource(
            campaignRepository,
            new SkillAnchorCodec(JsonMapper.builder().build()),
            new DefaultScoringRuleset()
        );
    }

    @Test
    @DisplayName("Prices a week of the live campaign at its own reference and week index")
    void shouldUseTheLiveCampaign() {
        Campaign campaign = CampaignFixtures.runningCampaign(1);
        when(campaignRepository.findByStatusNot(CampaignStatus.CLOSED)).thenReturn(Optional.of(campaign));

        ChallengeCalibration calibration = source.forWeek(CampaignFixtures.FIRST_WEEK_START.plusWeeks(4));

        assertThat(calibration.reference()).isEqualTo(CampaignFixtures.REFERENCE);
        assertThat(calibration.weekIndex()).isEqualTo(5);
    }

    @Test
    @DisplayName("Pays at week one in the gap between opening and the first Monday")
    void shouldClampTheWeekIndexBeforeTheStart() {
        Campaign campaign = CampaignFixtures.runningCampaign(1);
        campaign.setStatus(CampaignStatus.OPENED);
        when(campaignRepository.findByStatusNot(CampaignStatus.CLOSED)).thenReturn(Optional.of(campaign));

        ChallengeCalibration calibration = source.forWeek(CampaignFixtures.FIRST_WEEK_START.minusWeeks(1));

        assertThat(calibration.reference()).isEqualTo(CampaignFixtures.REFERENCE);
        assertThat(calibration.weekIndex()).isEqualTo(1);
    }

    @Test
    @DisplayName("Keeps the last closed campaign's reference between two campaigns")
    void shouldFallBackOnTheLastClosedCampaign() {
        Campaign closed = CampaignFixtures.runningCampaign(1);
        closed.setStatus(CampaignStatus.CLOSED);
        closed.setReference(9_400);

        when(campaignRepository.findByStatusNot(CampaignStatus.CLOSED)).thenReturn(Optional.empty());
        when(campaignRepository.findAllByStatusOrderByNumberDesc(CampaignStatus.CLOSED))
            .thenReturn(List.of(closed));

        ChallengeCalibration calibration = source.forWeek(CampaignFixtures.FIRST_WEEK_START.plusWeeks(20));

        assertThat(calibration.reference()).isEqualTo(9_400);
        assertThat(calibration.weekIndex()).isEqualTo(1);
    }

    @Test
    @DisplayName("Falls back on the floor when no campaign has ever existed")
    void shouldFallBackOnTheFloor() {
        when(campaignRepository.findByStatusNot(CampaignStatus.CLOSED)).thenReturn(Optional.empty());
        when(campaignRepository.findAllByStatusOrderByNumberDesc(CampaignStatus.CLOSED)).thenReturn(List.of());

        ChallengeCalibration calibration = source.forWeek(CampaignFixtures.FIRST_WEEK_START);

        assertThat(calibration.reference()).isEqualTo(new DefaultScoringRuleset().referenceFloor());
        assertThat(calibration.weekIndex()).isEqualTo(1);
        assertThat(calibration.scaling().anchors()).isEmpty();
    }
}
