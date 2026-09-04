package io.github.thomashtn.valoquests.campaign.service;

import io.github.thomashtn.valoquests.campaign.CampaignRuleset;
import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.entity.CampaignPlayer;
import io.github.thomashtn.valoquests.campaign.entity.CampaignWeek;
import io.github.thomashtn.valoquests.campaign.entity.Guardian;
import io.github.thomashtn.valoquests.campaign.exception.CampaignLifecycleException;
import io.github.thomashtn.valoquests.campaign.model.CampaignSchedule;
import io.github.thomashtn.valoquests.campaign.model.CampaignStatus;
import io.github.thomashtn.valoquests.campaign.model.CampaignWeekShape;
import io.github.thomashtn.valoquests.campaign.model.GuardianCategory;
import io.github.thomashtn.valoquests.campaign.model.NewCampaign;
import io.github.thomashtn.valoquests.campaign.model.SquadCalibration;
import io.github.thomashtn.valoquests.campaign.repository.GuardianRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.scoring.ScoringRuleset;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds a whole campaign at opening: its row, its frozen roster and its ten weeks.
 *
 * <p>The ten weeks exist before the first match is played, guardians included. The map is what the
 * squad plans against — a week ten with the biggest group behind the biggest guardian only means
 * something if it can be seen coming — and drawing a guardian week by week would also let a
 * catalogue edited mid-campaign change a fight that was already announced.
 *
 * <p>Guardian names are drawn without repetition inside their weight class, seeded on the campaign
 * number so a campaign reopened at the same number faces the same guardians. The catalogue holds
 * six minor, ten standard and six elite entries for two, six and two weeks, so a draw can never run
 * out.
 */
@Service
@Transactional(readOnly = true)
public class CampaignFactory {

    /**
     * Repository holding the guardian catalogue.
     */
    private final GuardianRepository guardianRepository;

    /**
     * Barème sizing guardians and groups.
     */
    private final CampaignRuleset campaignRuleset;

    /**
     * Barème holding the weekly reward progression.
     */
    private final ScoringRuleset scoringRuleset;

    /**
     * Codec serializing the calibration's anchors.
     */
    private final SkillAnchorCodec skillAnchorCodec;

    /**
     * Clock stamping the opening instant.
     */
    private final Clock clock;

    /**
     * Creates the campaign factory.
     *
     * @param guardianRepository guardian repository
     * @param campaignRuleset    campaign ruleset
     * @param scoringRuleset     scoring ruleset
     * @param skillAnchorCodec   skill anchor codec
     * @param clock              clock
     */
    public CampaignFactory(
        GuardianRepository guardianRepository,
        CampaignRuleset campaignRuleset,
        ScoringRuleset scoringRuleset,
        SkillAnchorCodec skillAnchorCodec,
        Clock clock
    ) {
        this.guardianRepository = guardianRepository;
        this.campaignRuleset = campaignRuleset;
        this.scoringRuleset = scoringRuleset;
        this.skillAnchorCodec = skillAnchorCodec;
        this.clock = clock;
    }

    /**
     * Builds one campaign, unsaved.
     *
     * @param number         campaign number, one more than the last one ever opened
     * @param roster         players to freeze, never empty
     * @param calibration    what the squad was measured at
     * @param firstWeekStart Monday the campaign starts on, strictly after today
     * @return the campaign, its roster and its ten weeks
     */
    public NewCampaign build(
        int number,
        List<Player> roster,
        SquadCalibration calibration,
        LocalDate firstWeekStart
    ) {
        Campaign campaign = new Campaign();
        campaign.setNumber(number);
        campaign.setStatus(CampaignStatus.OPENED);
        campaign.setOpenedAt(clock.instant());
        campaign.setFirstWeekStart(firstWeekStart);
        campaign.setLastWeekStart(firstWeekStart.plusWeeks(CampaignSchedule.WEEK_COUNT - 1L));
        campaign.setRosterSize(roster.size());
        campaign.setReference(calibration.reference());
        campaign.setTier(calibration.tier());
        campaign.setVolumeFactor(calibration.scaling().volumeFactor());
        campaign.setSkillAnchorsJson(skillAnchorCodec.toJson(calibration.scaling().anchors()));
        campaign.setCalibrationWindowMonths(calibration.windowMonths());
        campaign.setCalibrationFirstDay(calibration.firstDay());

        return new NewCampaign(campaign, roster(campaign, roster), weeks(campaign));
    }

    /**
     * Freezes the roster onto the campaign.
     *
     * @param campaign campaign being opened
     * @param roster   players to freeze
     * @return the roster rows
     */
    private List<CampaignPlayer> roster(Campaign campaign, List<Player> roster) {
        return roster.stream()
            .map(player -> {
                CampaignPlayer member = new CampaignPlayer();
                member.setCampaign(campaign);
                member.setPlayer(player);

                return member;
            })
            .toList();
    }

    /**
     * Builds the campaign's ten weeks, guardians drawn.
     *
     * @param campaign campaign being opened
     * @return the ten weeks, week one first
     */
    private List<CampaignWeek> weeks(Campaign campaign) {
        Map<GuardianCategory, Deque<Guardian>> draw = draw(campaign.getNumber());
        List<CampaignWeek> weeks = new ArrayList<>(CampaignSchedule.WEEK_COUNT);

        for (CampaignWeekShape shape : CampaignSchedule.weeks()) {
            weeks.add(week(campaign, shape, draw.get(shape.category()).poll()));
        }

        return weeks;
    }

    /**
     * Builds one week from its shape and the guardian drawn for it.
     *
     * @param campaign campaign being opened
     * @param shape    week's written shape
     * @param guardian guardian drawn for the week
     * @return the week, unsaved
     */
    private CampaignWeek week(Campaign campaign, CampaignWeekShape shape, Guardian guardian) {
        int progressionPercent = scoringRuleset.rewardProgressionPercent(shape.weekIndex());

        CampaignWeek week = new CampaignWeek();
        week.setCampaign(campaign);
        week.setWeekIndex(shape.weekIndex());
        week.setWeekStart(campaign.getFirstWeekStart().plusWeeks(shape.weekIndex() - 1L));
        week.setPlanetName(shape.planetName());
        week.setCategory(shape.category());
        week.setGuardianWeight(BigDecimal.valueOf(shape.guardianWeight()));
        week.setGroupWeight(BigDecimal.valueOf(shape.groupWeight()));
        week.setGuardian(guardian);
        week.setGuardianHitPoints(campaignRuleset.guardianHitPoints(
            campaign.getReference(),
            shape.guardianWeight(),
            campaign.getRosterSize()
        ));
        week.setWoundedCount(campaignRuleset.groupSize(
            campaign.getReference(),
            shape.groupWeight(),
            campaign.getRosterSize(),
            progressionPercent
        ));

        return week;
    }

    /**
     * Shuffles the catalogue once per weight class, so each class is drawn without repetition.
     *
     * @param seed campaign number, making the draw reproducible
     * @return one queue of guardians per weight class
     * @throws CampaignLifecycleException when a weight class holds too few entries
     */
    private Map<GuardianCategory, Deque<Guardian>> draw(int seed) {
        Map<GuardianCategory, Deque<Guardian>> draw = new EnumMap<>(GuardianCategory.class);

        for (GuardianCategory category : GuardianCategory.values()) {
            List<Guardian> candidates =
                new ArrayList<>(guardianRepository.findAllByEnabledTrueAndCategoryOrderByIdAsc(category));
            long required = CampaignSchedule.weeks().stream()
                .filter(shape -> shape.category() == category)
                .count();

            if (candidates.size() < required) {
                throw new CampaignLifecycleException(
                    "The guardian catalogue holds " + candidates.size() + " enabled " + category
                        + " entries but a campaign needs " + required + "."
                );
            }

            Collections.shuffle(candidates, new Random(seed * 31L + category.ordinal()));
            draw.put(category, new ArrayDeque<>(candidates));
        }

        return draw;
    }
}
