package io.github.thomashtn.valoquests.match.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.match.dto.SeasonResponse;
import io.github.thomashtn.valoquests.match.entity.Season;
import io.github.thomashtn.valoquests.match.repository.SeasonRepository;
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
     * Verifies that seasons are returned most recent first, with only that one flagged as in progress.
     *
     * <p>Insertion order is reversed on purpose: seasons follow imports, so an older one can have a greater id.</p>
     */
    @Test
    void shouldOrderSeasonsByEpisodeAndActDescending() {
        when(seasonRepository.findAllByOrderByIdDesc()).thenReturn(List.of(
            season(4L, "e9a1"),
            season(3L, "e10a3"),
            season(2L, "e10a1"),
            season(1L, "e11a2")
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
            season(2L, "0df9ce4a-4d1e-1234-9ba5-a1b2c3d4e5f6"),
            season(1L, "e11a1")
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
            season(4L, "e9a1"),
            season(3L, "e10a3"),
            season(2L, "e10a1"),
            season(1L, "e11a2")
        ));

        assertThat(service.resolveCurrentSeasonId()).isEqualTo(1L);
    }

    /**
     * Verifies that the year era Riot renamed its seasons to outranks every episode-era season.
     *
     * <p>Regression: year-era seasons once sorted behind episodes, so the current season resolved to a stale act.
     */
    @Test
    void shouldOrderYearEraSeasonsAfterEveryEpisodeEraSeason() {
        when(seasonRepository.findAllByOrderByIdDesc()).thenReturn(List.of(
            season(4L, "V26A2"),
            season(3L, "e11a4"),
            season(2L, "v26a4"),
            season(1L, "e10a1")
        ));

        List<SeasonResponse> result = service.findAll();

        assertThat(result).extracting(SeasonResponse::name)
            .containsExactly("v26a4", "V26A2", "e11a4", "e10a1");
        assertThat(result).extracting(SeasonResponse::active)
            .containsExactly(true, false, false, false);
        assertThat(service.resolveCurrentSeasonId()).isEqualTo(2L);
    }

    /**
     * Verifies that the current season resolves to {@code null} when no season is known yet.
     */
    @Test
    void shouldResolveNullCurrentSeasonWhenNoSeasonExists() {
        when(seasonRepository.findAllByOrderByIdDesc()).thenReturn(List.of());

        assertThat(service.resolveCurrentSeasonId()).isNull();
    }

    private Season season(Long id, String name) {
        Season season = new Season();
        season.setId(id);
        season.setExternalId("ext-" + id);
        season.setName(name);
        return season;
    }
}
