package io.github.thomashtn.valoquests.synchronization.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.thomashtn.valoquests.synchronization.dto.SynchronizationStatusResponse;
import io.github.thomashtn.valoquests.synchronization.service.SynchronizationQueryService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer tests for {@link SynchronizationController}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SynchronizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SynchronizationQueryService synchronizationQueryService;

    /**
     * Verifies that the status is public and exposes every field.
     */
    @Test
    void shouldExposeTheStatusWithoutAdminKey() throws Exception {
        when(synchronizationQueryService.findStatus())
            .thenReturn(new SynchronizationStatusResponse(
                true,
                Instant.parse("2026-09-30T10:00:00Z"),
                Instant.parse("2026-09-30T09:30:00Z")
            ));

        mockMvc.perform(get("/api/synchronization/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.inProgress").value(true))
            .andExpect(jsonPath("$.lastCompletedAt").value("2026-09-30T10:00:00Z"))
            .andExpect(jsonPath("$.lastImportedAt").value("2026-09-30T09:30:00Z"));
    }
}
