package io.github.thomashtn.valoquests.campaign;

import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.entity.CampaignPlayer;
import io.github.thomashtn.valoquests.campaign.entity.CampaignWeek;
import io.github.thomashtn.valoquests.campaign.entity.Guardian;
import io.github.thomashtn.valoquests.campaign.model.CampaignStatus;
import io.github.thomashtn.valoquests.campaign.model.CampaignTier;
import io.github.thomashtn.valoquests.campaign.model.GuardianCategory;
import io.github.thomashtn.valoquests.player.entity.Player;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Builds the campaign objects the unit tests of this package share.
 *
 * <p>Kept in one place because a campaign is only ever meaningful whole: a week without its
 * campaign has no reference, and a campaign without its roster has no denominator.
 */
public final class CampaignFixtures {

    /**
     * Monday the fixture campaign starts on.
     */
    public static final LocalDate FIRST_WEEK_START = LocalDate.of(2026, 9, 7);

    /**
     * Reference the catalogue's examples are written at.
     */
    public static final int REFERENCE = 5_300;

    /**
     * Instant the fixture campaign was opened at.
     */
    public static final Instant OPENED_AT = Instant.parse("2026-09-04T10:00:00Z");

    /**
     * Prevents instantiation of this fixture holder.
     */
    private CampaignFixtures() {
    }

    /**
     * Builds a running campaign of seven operators.
     *
     * @param id campaign identifier
     * @return the campaign
     */
    public static Campaign runningCampaign(long id) {
        Campaign campaign = new Campaign();
        campaign.setId(id);
        campaign.setNumber(1);
        campaign.setStatus(CampaignStatus.RUNNING);
        campaign.setOpenedAt(OPENED_AT);
        campaign.setFirstWeekStart(FIRST_WEEK_START);
        campaign.setLastWeekStart(FIRST_WEEK_START.plusWeeks(9));
        campaign.setRosterSize(7);
        campaign.setReference(REFERENCE);
        campaign.setTier(CampaignTier.NORMAL);
        campaign.setVolumeFactor(BigDecimal.ONE);
        campaign.setSkillAnchorsJson("{}");
        campaign.setCalibrationWindowMonths(9);
        campaign.setCalibrationFirstDay(FIRST_WEEK_START.minusMonths(9));

        return campaign;
    }

    /**
     * Builds one week of a campaign.
     *
     * @param campaign  campaign the week belongs to
     * @param weekIndex one-based position
     * @param hitPoints guardian hit points
     * @param wounded   wounded stranded on the planet
     * @return the week
     */
    public static CampaignWeek week(Campaign campaign, int weekIndex, int hitPoints, int wounded) {
        CampaignWeek week = new CampaignWeek();
        week.setId((long) weekIndex);
        week.setCampaign(campaign);
        week.setWeekIndex(weekIndex);
        week.setWeekStart(campaign.getFirstWeekStart().plusWeeks(weekIndex - 1L));
        week.setPlanetName("Orune");
        week.setCategory(GuardianCategory.MINOR);
        week.setGuardianWeight(new BigDecimal("0.60"));
        week.setGroupWeight(BigDecimal.ONE);
        week.setGuardian(guardian(1, GuardianCategory.MINOR));
        week.setGuardianHitPoints(hitPoints);
        week.setWoundedCount(wounded);

        return week;
    }

    /**
     * Builds one guardian catalogue entry.
     *
     * @param id       identifier
     * @param category weight class
     * @return the guardian
     */
    public static Guardian guardian(long id, GuardianCategory category) {
        Guardian guardian = new Guardian();
        guardian.setId(id);
        guardian.setCode("GUARDIAN_" + id);
        guardian.setName("Gardien " + id);
        guardian.setDescription("Une entité de test.");
        guardian.setCategory(category);

        return guardian;
    }

    /**
     * Builds a tracked player.
     *
     * @param id       identifier
     * @param gameName Riot name
     * @return the player
     */
    public static Player player(long id, String gameName) {
        Player player = new Player();
        player.setId(id);
        player.setGameName(gameName);
        player.setTagLine("EUW");
        player.setDisplayName(gameName);

        return player;
    }

    /**
     * Freezes one player into a campaign's roster.
     *
     * @param campaign campaign the roster belongs to
     * @param player   operator taking part
     * @return the roster row
     */
    public static CampaignPlayer member(Campaign campaign, Player player) {
        CampaignPlayer member = new CampaignPlayer();
        member.setCampaign(campaign);
        member.setPlayer(player);

        return member;
    }
}
