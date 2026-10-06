package io.github.thomashtn.valoquests.match.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.henrik.dto.match.HenrikMatchMetadata;
import io.github.thomashtn.valoquests.match.entity.Season;
import io.github.thomashtn.valoquests.match.repository.SeasonRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link SeasonResolutionService}.
 */
@ExtendWith(MockitoExtension.class)
class SeasonResolutionServiceTest {

    @Mock
    private SeasonRepository seasonRepository;

    /**
     * Service under test.
     */
    private SeasonResolutionService service;

    /**
     * Creates the service under test before each test.
     */
    @BeforeEach
    void setUp() {
        service = new SeasonResolutionService(seasonRepository);
    }

    @Test
    @DisplayName("Creates a season first seen, named after its short name")
    void shouldCreateASeasonFirstSeen() {
        when(seasonRepository.findByExternalId("act-1")).thenReturn(Optional.empty());
        when(seasonRepository.save(any(Season.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Season season = service.resolve(new HenrikMatchMetadata.HenrikSeason("act-1", "e10a1"));

        assertThat(season.getExternalId()).isEqualTo("act-1");
        assertThat(season.getName()).isEqualTo("e10a1");
    }

    @Test
    @DisplayName("Stores a newer short name Henrik reports for a known season")
    void shouldStoreANewerName() {
        Season stored = season("act-1", "act-1");
        when(seasonRepository.findByExternalId("act-1")).thenReturn(Optional.of(stored));
        when(seasonRepository.save(stored)).thenReturn(stored);

        Season season = service.resolve(new HenrikMatchMetadata.HenrikSeason("act-1", "e10a1"));

        assertThat(season.getName()).isEqualTo("e10a1");
        verify(seasonRepository).save(stored);
    }

    @Test
    @DisplayName("Leaves a known season untouched when its name has not changed")
    void shouldNotRewriteAnUnchangedSeason() {
        Season stored = season("act-1", "e10a1");
        when(seasonRepository.findByExternalId("act-1")).thenReturn(Optional.of(stored));

        assertThat(service.resolve(new HenrikMatchMetadata.HenrikSeason("act-1", "e10a1")))
            .isSameAs(stored);
        verify(seasonRepository, never()).save(any());
    }

    /**
     * Builds a stored season.
     */
    private Season season(String externalId, String name) {
        Season season = new Season();
        season.setExternalId(externalId);
        season.setName(name);
        return season;
    }
}
