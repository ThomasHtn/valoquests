package io.github.thomashtn.valoquests.campaign.service;

import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.repository.CampaignRepository;
import io.github.thomashtn.valoquests.challenge.service.ChallengeCalibrationSource;
import io.github.thomashtn.valoquests.scoring.model.CampaignDifficulty;
import io.github.thomashtn.valoquests.scoring.model.ChallengeCalibration;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves a week's challenge calibration (reference, grid and reward progression) from the campaign
 * that covers it.
 *
 * <p>Falls back twice. A week outside any live campaign takes the last closed campaign's
 * calibration, so a squad between two campaigns keeps the targets it earned rather than dropping
 * back to a beginner's; a database that never had a campaign takes the amateur one.
 *
 * <p>The week index is clamped to the campaign's own ten weeks, so a challenge drawn in the gap
 * between opening and the first Monday already pays at the campaign's reference but at week one's
 * progression. Between two campaigns everything pays at week one: the reward progression belongs to
 * a campaign, and inheriting week ten's would pay a bonus nobody is playing for.
 */
@Service
@Transactional(readOnly = true)
public class CampaignChallengeCalibrationSource implements ChallengeCalibrationSource {

    /**
     * Repository resolving the campaign in force.
     */
    private final CampaignRepository campaignRepository;

    /**
     * Creates the campaign-backed calibration source.
     *
     * @param campaignRepository campaign repository
     */
    public CampaignChallengeCalibrationSource(CampaignRepository campaignRepository) {
        this.campaignRepository = campaignRepository;
    }

    /**
     * Returns the calibration in force for one week.
     *
     * @param weekStart Monday identifying the week
     * @return the covering campaign's calibration, the last closed one's, or the amateur one
     */
    @Override
    public ChallengeCalibration forWeek(LocalDate weekStart) {
        Optional<Campaign> live = campaignRepository.findLive();

        if (live.isPresent()) {
            Campaign campaign = live.orElseThrow();

            return calibrationOf(campaign, campaign.scheduleWeekIndexOf(weekStart));
        }

        return campaignRepository.findLatestClosed()
            .map(campaign -> calibrationOf(campaign, 1))
            .orElseGet(CampaignChallengeCalibrationSource::amateurCalibration);
    }

    /**
     * Returns the calibration read outside any campaign: the amateur grid, on its first week.
     *
     * @return the fallback calibration
     */
    private static ChallengeCalibration amateurCalibration() {
        return new ChallengeCalibration(CampaignDifficulty.AMATEUR.reference(), 1, CampaignDifficulty.AMATEUR);
    }

    /**
     * Builds one campaign's calibration for a given week of it.
     *
     * @param campaign  campaign in force
     * @param weekIndex one-based week the reward progression is read at
     * @return the calibration
     */
    private ChallengeCalibration calibrationOf(Campaign campaign, int weekIndex) {
        return new ChallengeCalibration(
            campaign.reference(),
            weekIndex,
            campaign.getDifficulty()
        );
    }
}
