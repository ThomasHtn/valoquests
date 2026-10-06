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
     * Episode-era season short name, as {@code e<episode>a<act>}, for example {@code e11a4}.
     */
    private static final Pattern EPISODE_ACT_NAME = Pattern.compile("^e(\\d+)a(\\d+)$", Pattern.CASE_INSENSITIVE);

    /**
     * Year-era season short name, as {@code v<yy>a<act>}, which replaced episodes in 2026.
     */
    private static final Pattern YEAR_ACT_NAME = Pattern.compile("^v(\\d{2})a(\\d+)$", Pattern.CASE_INSENSITIVE);

    /**
     * Offset turning a two-digit year into its full form, so year-era keys outrank episode-era ones.
     */
    private static final long YEAR_ERA_BASE = 2_000L;

    /**
     * Multiplier scaling the era's leading number past any act count, so one number orders the pair.
     */
    private static final long ERA_SCALE = 1_000L;

    /**
     * Sort key placing a season whose name follows neither spelling after every datable season.
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
     * <p>Ordered by the act encoded in the name, since insertion order follows Henrik, not time.
     *
     * @return known seasons
     */
    @Override
    public List<SeasonResponse> findAll() {
        List<Season> seasons = chronologicallyOrderedSeasons();
        Long currentSeasonId = seasons.isEmpty() ? null : seasons.getFirst().getId();

        return seasons.stream()
            .map(season -> new SeasonResponse(
                season.getId(),
                season.getName(),
                season.getId().equals(currentSeasonId)
            ))
            .toList();
    }

    /**
     * Resolves the season currently in progress as the most recent one known.
     *
     * @return the current season's identifier, or {@code null} if no season is known yet
     */
    @Override
    public Long resolveCurrentSeasonId() {
        List<Season> seasons = chronologicallyOrderedSeasons();
        return seasons.isEmpty() ? null : seasons.get(0).getId();
    }

    /**
     * Every season, newest first, ordered by era and act rather than by identifier.
     */
    private List<Season> chronologicallyOrderedSeasons() {
        return seasonRepository.findAllByOrderByIdDesc().stream()
            .sorted(Comparator.comparingLong(DefaultSeasonQueryService::chronologicalKey).reversed())
            .toList();
    }

    /**
     * Builds the sort key ranking a season against the others, greater being more recent.
     */
    private static long chronologicalKey(Season season) {
        Matcher episode = EPISODE_ACT_NAME.matcher(season.getName());
        if (episode.matches()) {
            return actKey(Long.parseLong(episode.group(1)), episode.group(2));
        }

        Matcher year = YEAR_ACT_NAME.matcher(season.getName());
        if (year.matches()) {
            return actKey(YEAR_ERA_BASE + Long.parseLong(year.group(1)), year.group(2));
        }

        return UNDATABLE_SEASON_KEY;
    }

    /**
     * Combines an era's leading number and an act into one comparable key.
     *
     * @param eraNumber episode number, or full year for the year era
     * @param act       act number within that era, as matched
     * @return ordering key, greater being more recent
     */
    private static long actKey(long eraNumber, String act) {
        return eraNumber * ERA_SCALE + Long.parseLong(act);
    }
}
