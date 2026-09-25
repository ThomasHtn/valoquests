package io.github.thomashtn.valoquests.player.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.campaign.CampaignFixtures;
import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.model.CampaignStatus;
import io.github.thomashtn.valoquests.campaign.repository.CampaignPlayerRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifies that a player is protected exactly when a campaign froze them into its roster.
 */
@ExtendWith(MockitoExtension.class)
class PlayerCampaignContributionResolverTest {

    /**
     * Identifier used across the cases.
     */
    private static final long PLAYER_ID = 7L;

    @Mock
    private CampaignPlayerRepository campaignPlayerRepository;

    @Mock
    private CampaignRepository campaignRepository;

    @InjectMocks
    private PlayerCampaignContributionResolver resolver;

    @Test
    @DisplayName("Protects a player a campaign froze into its roster")
    void shouldReportContributionForRosterMember() {
        when(campaignPlayerRepository.existsByPlayerId(PLAYER_ID)).thenReturn(true);

        assertThat(resolver.hasContributed(PLAYER_ID)).isTrue();
    }

    @Test
    @DisplayName("Leaves a player no campaign ever froze deletable")
    void shouldReportNoContributionOutsideEveryRoster() {
        when(campaignPlayerRepository.existsByPlayerId(PLAYER_ID)).thenReturn(false);

        assertThat(resolver.hasContributed(PLAYER_ID)).isFalse();
    }

    @Test
    @DisplayName("Places a player on the live roster only when the live campaign froze them into it")
    void shouldReportLiveRosterMembership() {
        Campaign live = CampaignFixtures.runningCampaign(1);
        live.setId(3L);
        when(campaignRepository.findByStatusNot(CampaignStatus.CLOSED)).thenReturn(Optional.of(live));
        when(campaignPlayerRepository.existsByCampaignIdAndPlayerId(3L, PLAYER_ID)).thenReturn(true);

        assertThat(resolver.isCampaignLive()).isTrue();
        assertThat(resolver.isOnLiveRoster(PLAYER_ID)).isTrue();
    }

    @Test
    @DisplayName("Puts nobody on a live roster between two campaigns")
    void shouldReportNoLiveRosterBetweenCampaigns() {
        when(campaignRepository.findByStatusNot(CampaignStatus.CLOSED)).thenReturn(Optional.empty());

        assertThat(resolver.isCampaignLive()).isFalse();
        assertThat(resolver.isOnLiveRoster(PLAYER_ID)).isFalse();
    }
}
