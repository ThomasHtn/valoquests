package io.github.thomashtn.valoquests.campaign.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.thomashtn.valoquests.campaign.dto.CampaignResponse;
import io.github.thomashtn.valoquests.campaign.dto.CampaignTodayResponse;
import io.github.thomashtn.valoquests.campaign.service.CampaignQueryService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer tests for {@link CampaignController}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CampaignControllerTest {

    /**
     * Day the answers are computed on.
     */
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 16);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CampaignQueryService queryService;

    /**
     * Verifies that the site can read "no campaign" from the same call as a running one.
     */
    @Test
    void shouldAnswerWithoutACampaign() throws Exception {
        when(queryService.currentCampaign()).thenReturn(CampaignResponse.none(TODAY));

        mockMvc.perform(get("/api/campaign"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").doesNotExist())
            .andExpect(jsonPath("$.today").value("2026-09-16"))
            .andExpect(jsonPath("$.weeks").isEmpty());
    }

    /**
     * Verifies that the day in progress is exposed on its own route.
     */
    @Test
    void shouldExposeTheDayInProgress() throws Exception {
        when(queryService.today()).thenReturn(CampaignTodayResponse.none(TODAY));

        mockMvc.perform(get("/api/campaign/today"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.day").value("2026-09-16"))
            .andExpect(jsonPath("$.players").isEmpty());
    }

    /**
     * Verifies that the history answers an empty list rather than a 404.
     */
    @Test
    void shouldExposeAnEmptyHistory() throws Exception {
        when(queryService.history()).thenReturn(List.of());

        mockMvc.perform(get("/api/campaign/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }
}
