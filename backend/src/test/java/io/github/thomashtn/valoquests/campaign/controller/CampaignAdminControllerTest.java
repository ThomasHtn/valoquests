package io.github.thomashtn.valoquests.campaign.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.thomashtn.valoquests.campaign.CampaignFixtures;
import io.github.thomashtn.valoquests.campaign.exception.CampaignLifecycleException;
import io.github.thomashtn.valoquests.campaign.model.CampaignTier;
import io.github.thomashtn.valoquests.campaign.model.SquadCalibration;
import io.github.thomashtn.valoquests.campaign.service.AsyncHistoryBackfillRunner;
import io.github.thomashtn.valoquests.campaign.service.CampaignLifecycleService;
import io.github.thomashtn.valoquests.campaign.service.CampaignReplayService;
import io.github.thomashtn.valoquests.challenge.model.ChallengeScaling;
import io.github.thomashtn.valoquests.shared.config.AdminApiKeyFilter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer tests for {@link CampaignAdminController}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CampaignAdminControllerTest {

    /**
     * Administrative key configured for the test context.
     */
    private static final String ADMIN_KEY = "test-admin-key-0123456789abcdef0";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CampaignLifecycleService lifecycleService;

    @MockitoBean
    private CampaignReplayService replayService;

    @MockitoBean
    private AsyncHistoryBackfillRunner backfillRunner;

    /**
     * Verifies that the calibration preview commits to nothing.
     */
    @Test
    void shouldPreviewTheCalibration() throws Exception {
        when(lifecycleService.previewCalibration()).thenReturn(new SquadCalibration(
            5_300,
            CampaignTier.NORMAL,
            ChallengeScaling.NONE,
            9,
            CampaignFixtures.FIRST_WEEK_START.minusMonths(9),
            List.of()
        ));

        mockMvc.perform(get("/api/admin/campaigns/calibration")
                .header(AdminApiKeyFilter.HEADER_NAME, ADMIN_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reference").value(5_300))
            .andExpect(jsonPath("$.tier").value("NORMAL"))
            .andExpect(jsonPath("$.windowMonths").value(9));
    }

    /**
     * Verifies that opening a campaign answers with the ten weeks it just scheduled.
     */
    @Test
    void shouldOpenACampaign() throws Exception {
        when(lifecycleService.open()).thenReturn(CampaignFixtures.runningCampaign(1));

        mockMvc.perform(post("/api/admin/campaigns")
                .header(AdminApiKeyFilter.HEADER_NAME, ADMIN_KEY))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.number").value(1))
            .andExpect(jsonPath("$.firstWeekStart").value("2026-09-07"))
            .andExpect(jsonPath("$.lastWeekStart").value("2026-11-09"))
            .andExpect(jsonPath("$.rosterSize").value(7));
    }

    /**
     * Verifies that a second campaign is refused as a conflict rather than silently opened.
     */
    @Test
    void shouldRefuseASecondCampaign() throws Exception {
        when(lifecycleService.open())
            .thenThrow(new CampaignLifecycleException("A campaign is already opened or running."));

        mockMvc.perform(post("/api/admin/campaigns")
                .header(AdminApiKeyFilter.HEADER_NAME, ADMIN_KEY))
            .andExpect(status().isConflict());
    }

    /**
     * Verifies that the history backfill is accepted and left to run in the background.
     */
    @Test
    void shouldAcceptTheHistoryBackfill() throws Exception {
        mockMvc.perform(post("/api/admin/campaigns/backfill")
                .header(AdminApiKeyFilter.HEADER_NAME, ADMIN_KEY))
            .andExpect(status().isAccepted());

        verify(backfillRunner).run();
    }

    /**
     * Verifies that stopping a campaign reports the day it was frozen on.
     */
    @Test
    void shouldStopACampaign() throws Exception {
        when(lifecycleService.stop(any())).thenReturn(CampaignFixtures.runningCampaign(1));

        mockMvc.perform(post("/api/admin/campaigns/stop")
                .header(AdminApiKeyFilter.HEADER_NAME, ADMIN_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RUNNING"));
    }

    /**
     * Verifies that the replay route is a repair tool with no body of its own.
     */
    @Test
    void shouldReplayTheCampaign() throws Exception {
        mockMvc.perform(post("/api/admin/campaigns/replay")
                .header(AdminApiKeyFilter.HEADER_NAME, ADMIN_KEY))
            .andExpect(status().isNoContent());

        verify(replayService).replayRunningCampaign();
    }

    /**
     * Verifies that deleting a campaign removes it and answers without a body.
     */
    @Test
    void shouldDeleteACampaign() throws Exception {
        mockMvc.perform(delete("/api/admin/campaigns/7")
                .header(AdminApiKeyFilter.HEADER_NAME, ADMIN_KEY))
            .andExpect(status().isNoContent());

        verify(lifecycleService).delete(7L);
    }

    /**
     * Verifies that the whole lifecycle stays behind the administrative key.
     */
    @Test
    void shouldRejectAnUnauthenticatedCall() throws Exception {
        mockMvc.perform(post("/api/admin/campaigns"))
            .andExpect(status().isUnauthorized());
    }
}
