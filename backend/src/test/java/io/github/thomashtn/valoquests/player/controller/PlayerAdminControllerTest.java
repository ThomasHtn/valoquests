package io.github.thomashtn.valoquests.player.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.thomashtn.valoquests.player.dto.PlayerAdminResponse;
import io.github.thomashtn.valoquests.player.dto.PlayerCreateRequest;
import io.github.thomashtn.valoquests.player.dto.PlayerDeletionResponse;
import io.github.thomashtn.valoquests.player.dto.PlayerUpdateRequest;
import io.github.thomashtn.valoquests.player.model.PlayerDeletionOutcome;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.service.PlayerAdminService;
import io.github.thomashtn.valoquests.shared.config.AdminApiKeyFilter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer tests for {@link PlayerAdminController}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PlayerAdminControllerTest {

    /**
     * Administrative key configured for the test context.
     */
    private static final String ADMIN_KEY = "test-admin-key-0123456789abcdef0";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlayerAdminService service;

    /**
     * Verifies that the listing exposes the fields the administration screen edits.
     */
    @Test
    void shouldListEveryPlayer() throws Exception {
        when(service.findAll()).thenReturn(List.of(response(PlayerStatus.ARCHIVED)));

        mockMvc.perform(
                get("/api/admin/players")
                    .header(AdminApiKeyFilter.HEADER_NAME, ADMIN_KEY)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].gameName").value("Jett"))
            .andExpect(jsonPath("$[0].status").value("ARCHIVED"))
            .andExpect(jsonPath("$[0].hasCampaignContribution").value(true));
    }

    /**
     * Verifies that a created player answers 201.
     */
    @Test
    void shouldCreateAPlayer() throws Exception {
        when(service.create(any(PlayerCreateRequest.class)))
            .thenReturn(response(PlayerStatus.ACTIVE));

        mockMvc.perform(
                post("/api/admin/players")
                    .header(AdminApiKeyFilter.HEADER_NAME, ADMIN_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"gameName":"Jett","tagLine":"EUW","displayName":"Jett","status":"ACTIVE"}
                        """)
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(3));
    }

    /**
     * Verifies that a blank Riot identity is rejected before reaching the service.
     */
    @Test
    void shouldRejectABlankRiotIdentity() throws Exception {
        mockMvc.perform(
                post("/api/admin/players")
                    .header(AdminApiKeyFilter.HEADER_NAME, ADMIN_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"gameName":"","tagLine":"EUW","displayName":"Jett","status":"ACTIVE"}
                        """)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(service);
    }

    /**
     * Verifies that an identity update delegates with its path identifier.
     */
    @Test
    void shouldUpdateAPlayerIdentity() throws Exception {
        when(service.update(org.mockito.ArgumentMatchers.eq(3L), any(PlayerUpdateRequest.class)))
            .thenReturn(response(PlayerStatus.ACTIVE));

        mockMvc.perform(
                put("/api/admin/players/3")
                    .header(AdminApiKeyFilter.HEADER_NAME, ADMIN_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"gameName":"Jett","tagLine":"EUW","displayName":"Jett"}
                        """)
            )
            .andExpect(status().isOk());

        verify(service).update(org.mockito.ArgumentMatchers.eq(3L), any(PlayerUpdateRequest.class));
    }

    /**
     * Verifies that a status change unwraps the request body.
     */
    @Test
    void shouldChangeAPlayerStatus() throws Exception {
        when(service.changeStatus(3L, PlayerStatus.INACTIVE))
            .thenReturn(response(PlayerStatus.INACTIVE));

        mockMvc.perform(
                patch("/api/admin/players/3/status")
                    .header(AdminApiKeyFilter.HEADER_NAME, ADMIN_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"status\":\"INACTIVE\"}")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("INACTIVE"));

        verify(service).changeStatus(3L, PlayerStatus.INACTIVE);
    }

    /**
     * Verifies that a deletion reports which of the two outcomes happened.
     */
    @Test
    void shouldReportThatADeletionArchivedThePlayer() throws Exception {
        when(service.removeFromRoster(3L))
            .thenReturn(new PlayerDeletionResponse(3L, PlayerDeletionOutcome.ARCHIVED));

        mockMvc.perform(
                delete("/api/admin/players/3")
                    .header(AdminApiKeyFilter.HEADER_NAME, ADMIN_KEY)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.outcome").value("ARCHIVED"));
    }

    /**
     * Creates an administration player representation.
     *
     * @param status lifecycle status
     * @return the representation
     */
    private PlayerAdminResponse response(PlayerStatus status) {
        return new PlayerAdminResponse(
            3L,
            "Jett",
            "EUW",
            "Jett",
            null,
            status,
            null,
            null,
            true,
            true
        );
    }
}
