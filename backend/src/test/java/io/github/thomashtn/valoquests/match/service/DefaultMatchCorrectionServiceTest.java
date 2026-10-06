package io.github.thomashtn.valoquests.match.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.match.dto.MatchCorrectionResponse;
import io.github.thomashtn.valoquests.match.entity.ValorantMatch;
import io.github.thomashtn.valoquests.match.exception.MatchNotFoundException;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.GameModeSource;
import io.github.thomashtn.valoquests.match.repository.ValorantMatchRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link DefaultMatchCorrectionService}.
 */
@ExtendWith(MockitoExtension.class)
class DefaultMatchCorrectionServiceTest {

    @Mock
    private ValorantMatchRepository matchRepository;

    @Test
    @DisplayName("Stores the corrected mode as a manual correction and reports the match state")
    void shouldCorrectTheGameModeManually() {
        ValorantMatch match = new ValorantMatch();
        match.setId(1204L);
        match.setGameMode(GameMode.SKIRMISH);
        match.setGameModeSource(GameModeSource.INFERRED);
        when(matchRepository.findById(1204L)).thenReturn(Optional.of(match));
        when(matchRepository.save(any(ValorantMatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchCorrectionResponse response =
            new DefaultMatchCorrectionService(matchRepository).correctGameMode(1204L, GameMode.DEATHMATCH);

        assertThat(response).isEqualTo(
            new MatchCorrectionResponse(1204L, GameMode.DEATHMATCH, GameModeSource.MANUALLY_CORRECTED)
        );
    }

    @Test
    @DisplayName("Rejects a correction of an unknown match")
    void shouldRejectAnUnknownMatch() {
        when(matchRepository.findById(999L)).thenReturn(Optional.empty());

        DefaultMatchCorrectionService service = new DefaultMatchCorrectionService(matchRepository);

        assertThatThrownBy(() -> service.correctGameMode(999L, GameMode.DEATHMATCH))
            .isInstanceOf(MatchNotFoundException.class);
    }
}
