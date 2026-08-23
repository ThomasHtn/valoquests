package io.github.thomashtn.valoquests.match.service;

import io.github.thomashtn.valoquests.match.dto.SeasonResponse;
import io.github.thomashtn.valoquests.match.entity.Season;
import io.github.thomashtn.valoquests.match.repository.SeasonRepository;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements season consultation from persisted season data.
 */
@Service
@Transactional(readOnly = true)
public class DefaultSeasonQueryService implements SeasonQueryService {

    /**
     * Henrik season short name, as {@code e<episode>a<act>}, for example {@code e11a4}.
     */
    private static final Pattern EPISODE_ACT_NAME = Pattern.compile("^e(\\d+)a(\\d+)$");

    /**
     * Sort key given to a season whose name does not follow {@link #EPISODE_ACT_NAME}, placing it
     * after every datable season rather than at an arbitrary point in the middle of them.
     */
    private static final long UNDATABLE_SEASON_KEY = -1L;

    /**
     * Repository used to load persisted seasons.
     */
    private final SeasonRepository seasonRepository;

    /**
     * Creates the persisted season query service.
     *
     * @param seasonRepository repository used to load persisted seasons
     */
    public DefaultSeasonQueryService(SeasonRepository seasonRepository) {
        this.seasonRepository = seasonRepository;
    }

    /**
     * Returns every known season, most recent first.
     *
     * <p>Ordered by the episode and act encoded in the season name rather than by identifier:
     * seasons are created on demand as matches are imported, so their insertion order follows the
     * order Henrik happens to return matches in and is not chronological. Seasons whose name
     * cannot be read that way keep the repository's order and come last.</p>
     *
     * @return known seasons
     */
    @Override
    public List<SeasonResponse> findAll() {
        return chronologicallyOrderedSeasons().stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Resolves the season currently in progress as the most recent one known - seasons are created
     * on demand from imported matches, so the most recent one is always the one still being played.
     *
     * @return the current season's identifier, or {@code null} if no season is known yet
     */
    @Override
    public Long resolveCurrentSeasonId() {
        List<Season> seasons = chronologicallyOrderedSeasons();
        return seasons.isEmpty() ? null : seasons.get(0).getId();
    }

    private List<Season> chronologicallyOrderedSeasons() {
        return seasonRepository.findAllByOrderByIdDesc().stream()
            .sorted(Comparator.comparingLong(DefaultSeasonQueryService::chronologicalKey).reversed())
            .toList();
    }

    /**
     * Builds the sort key ranking a season against the others, greater being more recent.
     *
     * <p>Acts are numbered from one within an episode, so scaling the episode past any act count
     * makes a single number order the pair.</p>
     */
    private static long chronologicalKey(Season season) {
        Matcher matcher = EPISODE_ACT_NAME.matcher(season.getName());
        if (!matcher.matches()) {
            return UNDATABLE_SEASON_KEY;
        }

        return Long.parseLong(matcher.group(1)) * 1_000L + Long.parseLong(matcher.group(2));
    }

    private SeasonResponse toResponse(Season season) {
        return new SeasonResponse(season.getId(), season.getName(), season.isActive());
    }
}
