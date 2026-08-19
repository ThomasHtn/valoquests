package io.github.thomashtn.valorant.tracker.match.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valorant.tracker.match.dto.SeasonResponse;
import io.github.thomashtn.valorant.tracker.match.entity.Season;
import io.github.thomashtn.valorant.tracker.match.repository.SeasonRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
     * Verifies that seasons are returned most recent first, whatever order they were created in.
     *
     * <p>Insertion order is deliberately the reverse of the chronological one here: seasons are
     * created as matches are imported, so an older season can easily carry a greater
     * identifier.</p>
     */
    @Test
    void shouldOrderSeasonsByEpisodeAndActDescending() {
        when(seasonRepository.findAllByOrderByIdDesc()).thenReturn(List.of(
            season(4L, "e9a1", false),
            season(3L, "e10a3", false),
            season(2L, "e10a1", false),
            season(1L, "e11a2", true)
        ));

        List<SeasonResponse> result = service.findAll();

        assertThat(result).containsExactly(
            new SeasonResponse(1L, "e11a2", true),
            new SeasonResponse(3L, "e10a3", false),
            new SeasonResponse(2L, "e10a1", false),
            new SeasonResponse(4L, "e9a1", false)
        );
    }

    /**
     * Verifies that a season whose name carries no episode and act is kept, after every season
     * that can be placed chronologically.
     */
    @Test
    void shouldPlaceUndatableSeasonsLast() {
        when(seasonRepository.findAllByOrderByIdDesc()).thenReturn(List.of(
            season(2L, "0df9ce4a-4d1e-1234-9ba5-a1b2c3d4e5f6", false),
            season(1L, "e11a1", true)
        ));

        List<SeasonResponse> result = service.findAll();

        assertThat(result).containsExactly(
            new SeasonResponse(1L, "e11a1", true),
            new SeasonResponse(2L, "0df9ce4a-4d1e-1234-9ba5-a1b2c3d4e5f6", false)
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

    /**
     * Verifies that the current season resolves to the most recent one by episode and act, not the
     * one with the greatest identifier.
     */
    @Test
    void shouldResolveCurrentSeasonAsTheMostRecentByEpisodeAndAct() {
        when(seasonRepository.findAllByOrderByIdDesc()).thenReturn(List.of(
            season(4L, "e9a1", false),
            season(3L, "e10a3", false),
            season(2L, "e10a1", false),
            season(1L, "e11a2", false)
        ));

        assertThat(service.resolveCurrentSeasonId()).isEqualTo(1L);
    }

    /**
     * Verifies that the current season resolves to {@code null} when no season is known yet.
     */
    @Test
    void shouldResolveNullCurrentSeasonWhenNoSeasonExists() {
        when(seasonRepository.findAllByOrderByIdDesc()).thenReturn(List.of());

        assertThat(service.resolveCurrentSeasonId()).isNull();
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
