package io.github.thomashtn.valoquests.player.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.campaign.repository.CampaignPlayerRepository;
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
}
