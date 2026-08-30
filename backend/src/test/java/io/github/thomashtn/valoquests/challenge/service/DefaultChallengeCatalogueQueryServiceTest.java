package io.github.thomashtn.valoquests.challenge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.challenge.dto.ChallengeCatalogueResponse;
import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCondition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.challenge.model.ChallengeMetric;
import io.github.thomashtn.valoquests.challenge.model.ChallengeOperator;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import io.github.thomashtn.valoquests.challenge.parser.ChallengeDefinitionParser;
import io.github.thomashtn.valoquests.challenge.repository.ChallengeRepository;
import io.github.thomashtn.valoquests.colony.DefaultColonyRuleset;
import io.github.thomashtn.valoquests.scoring.DefaultScoringRuleset;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link DefaultChallengeCatalogueQueryService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Challenge catalogue queries")
class DefaultChallengeCatalogueQueryServiceTest {

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private ChallengeDefinitionParser definitionParser;

    private DefaultChallengeCatalogueQueryService service;

    @BeforeEach
    void setUp() {
        DefaultScoringRuleset scoringRuleset = new DefaultScoringRuleset();

        service = new DefaultChallengeCatalogueQueryService(
            challengeRepository,
            definitionParser,
            scoringRuleset,
            new DefaultColonyRuleset(scoringRuleset)
        );
    }

    @Test
    @DisplayName("exposes a simple challenge's target, damage and materials")
    void shouldExposeASimpleChallengesFields() {
        Challenge challenge = challenge(10L, "Kill them all");
        when(challengeRepository.findAllByEnabledTrueOrderByIdAsc()).thenReturn(List.of(challenge));
        when(definitionParser.parse(challenge)).thenReturn(new ChallengeDefinition(
            3,
            ProgressMode.SUM,
            List.of(condition(ChallengeMetric.KILLS, BigDecimal.valueOf(50)))
        ));

        ChallengeCatalogueResponse.ChallengeCatalogueEntry entry =
            service.findCatalogue().challenges().getFirst();

        assertThat(entry.id()).isEqualTo(10L);
        assertThat(entry.name()).isEqualTo("Kill them all");
        assertThat(entry.description()).isEqualTo("Kill them all description");
        assertThat(entry.difficulty()).isEqualTo(ChallengeDifficulty.MEDIUM);
        assertThat(entry.metric()).isEqualTo("KILLS");
        assertThat(entry.targetValue()).isEqualByComparingTo("50");
        assertThat(entry.damage()).isEqualTo(2_200);
        // Same derivation the current-week endpoint uses: never disagrees with `damage`.
        assertThat(entry.materials()).isEqualTo(22);
    }

    @Test
    @DisplayName("leaves a composite challenge's target unset, with no week to draw one from")
    void shouldLeaveACompositeTargetUnset() {
        Challenge challenge = challenge(11L, "Do both");
        when(challengeRepository.findAllByEnabledTrueOrderByIdAsc()).thenReturn(List.of(challenge));
        when(definitionParser.parse(challenge)).thenReturn(new ChallengeDefinition(
            3,
            ProgressMode.ALL,
            List.of(
                condition(ChallengeMetric.KILLS, BigDecimal.TEN),
                condition(ChallengeMetric.ASSISTS, BigDecimal.ONE)
            )
        ));

        ChallengeCatalogueResponse.ChallengeCatalogueEntry entry =
            service.findCatalogue().challenges().getFirst();

        assertThat(entry.metric()).isEqualTo("KILLS + ASSISTS");
        assertThat(entry.targetValue()).isNull();
    }

    @Test
    @DisplayName("only ever lists enabled challenges, ordered by identifier")
    void shouldOnlyListEnabledChallenges() {
        when(challengeRepository.findAllByEnabledTrueOrderByIdAsc()).thenReturn(List.of());

        assertThat(service.findCatalogue().challenges()).isEmpty();
    }

    private Challenge challenge(long id, String name) {
        Challenge challenge = new Challenge();
        challenge.setId(id);
        challenge.setName(name);
        challenge.setDescription(name + " description");
        challenge.setDifficulty(ChallengeDifficulty.MEDIUM);
        return challenge;
    }

    private ChallengeCondition condition(ChallengeMetric metric, BigDecimal target) {
        return new ChallengeCondition(
            metric,
            ChallengeOperator.GTE,
            target,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }
}
