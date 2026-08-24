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
     * Year-era season short name, as {@code v<yy>a<act>}, for example {@code v26a4}.
     *
     * <p>Riot renamed its seasons once: episodes ran until 2025, years took over from 2026. Both
     * spellings therefore coexist in a database built from imported matches, and both have to order
     * against each other — while only the episode form was read here, every year-era season scored
     * {@link #UNDATABLE_SEASON_KEY} and sorted <em>behind</em> the episodes it actually follows, so
     * the "current" season resolved to a stale act as soon as the era changed.
     */
    private static final Pattern YEAR_ACT_NAME = Pattern.compile("^v(\\d{2})a(\\d+)$", Pattern.CASE_INSENSITIVE);

    /**
     * Offset turning a two-digit year into its full form, so a year-era key always outranks an
     * episode-era one: episodes stop in the tens, years start at 2026.
     */
    private static final long YEAR_ERA_BASE = 2_000L;

    /**
     * Multiplier scaling the era's leading number past any act count, so one number orders the pair.
     */
    private static final long ERA_SCALE = 1_000L;

    /**
     * Sort key given to a season whose name follows neither supported spelling, placing it after
     * every datable season rather than at an arbitrary point in the middle of them.
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
     * <p>Acts are numbered from one within an episode or a year, so scaling that leading number
     * past any act count makes a single number order the pair. A year is expanded to its full form
     * first, which is what places the whole year era after the whole episode era.</p>
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

    private SeasonResponse toResponse(Season season) {
        return new SeasonResponse(season.getId(), season.getName(), season.isActive());
    }
}
