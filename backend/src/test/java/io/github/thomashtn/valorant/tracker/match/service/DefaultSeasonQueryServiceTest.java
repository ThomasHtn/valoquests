package io.github.thomashtn.valorant.tracker.match.service;

import io.github.thomashtn.valorant.tracker.match.dto.SeasonResponse;
import io.github.thomashtn.valorant.tracker.match.entity.Season;
import io.github.thomashtn.valorant.tracker.match.repository.SeasonRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultSeasonQueryService}.
 */
@ExtendWith(MockitoExtension.class)
class DefaultSeasonQueryServiceTest {

    /**
     * Mocked season repository.
     */
    @Mock
    private SeasonRepository seasonRepository;

    /**
     * Service under test.
     */
    private DefaultSeasonQueryService service;

    /**
     * Creates the service under test before each test.
     */
    @BeforeEach
    void setUp() {
        service = new DefaultSeasonQueryService(seasonRepository);
    }

    /**
     * Verifies that persisted seasons are mapped to responses in the repository's order.
     */
    @Test
    void shouldMapSeasonsInRepositoryOrder() {
        Season recent = season(2L, "2025 Act 2", false);
        Season older = season(1L, "2025 Act 1", false);
        when(seasonRepository.findAllByOrderByIdDesc()).thenReturn(List.of(recent, older));

        List<SeasonResponse> result = service.findAll();

        assertThat(result).containsExactly(
            new SeasonResponse(2L, "2025 Act 2", false),
            new SeasonResponse(1L, "2025 Act 1", false)
        );
    }

    /**
     * Verifies that the absence of persisted seasons yields an empty list.
     */
    @Test
    void shouldReturnEmptyListWhenNoSeasonExists() {
        when(seasonRepository.findAllByOrderByIdDesc()).thenReturn(List.of());

        assertThat(service.findAll()).isEmpty();
    }

    private Season season(Long id, String name, boolean active) {
        Season season = new Season();
        season.setId(id);
        season.setExternalId("ext-" + id);
        season.setName(name);
        season.setActive(active);
        return season;
    }
}
