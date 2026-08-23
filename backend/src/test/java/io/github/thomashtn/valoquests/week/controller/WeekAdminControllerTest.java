package io.github.thomashtn.valoquests.week.controller;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.thomashtn.valoquests.shared.config.AdminApiKeyFilter;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import io.github.thomashtn.valoquests.week.service.WeeklyLifecycleCoordinator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer tests for {@link WeekAdminController}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WeekAdminControllerTest {

    /**
     * Administrative key configured for the test context.
     */
    private static final String ADMIN_KEY = "test-admin-key-0123456789abcdef0";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WeekCalendar weekCalendar;

    @MockitoBean
    private WeeklyLifecycleCoordinator weeklyLifecycleCoordinator;

    /**
     * Verifies that the route opens the week currently in progress.
     *
     * <p>Pinning the week matters: opening any other one would draw challenges and a boss for a
     * week nobody is playing, while leaving the broken one just as empty.
     */
    @Test
    void shouldOpenTheWeekCurrentlyInProgress() throws Exception {
        mockMvc.perform(
                post("/api/admin/weeks/current/selection")
                    .header(AdminApiKeyFilter.HEADER_NAME, ADMIN_KEY)
            )
            .andExpect(status().isNoContent());

        verify(weeklyLifecycleCoordinator).openWeek(weekCalendar.currentWeekStart());
    }
}
