package io.github.thomashtn.valoquests.profile.service;

import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.repository.CampaignRepository;
import io.github.thomashtn.valoquests.match.dto.MatchHistoryFilter;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.exception.MatchNotFoundException;
import io.github.thomashtn.valoquests.match.repository.PlayerMatchHistoryCriteria;
import io.github.thomashtn.valoquests.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valoquests.match.service.MatchFilterParser;
import io.github.thomashtn.valoquests.player.exception.PlayerNotFoundException;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.profile.dto.MatchDetailResponse;
import io.github.thomashtn.valoquests.profile.dto.MatchResponse;
import io.github.thomashtn.valoquests.profile.dto.SquadMatchResponse;
import io.github.thomashtn.valoquests.profile.mapper.MatchResponseMapper;
import io.github.thomashtn.valoquests.scoring.model.DailyOutput;
import io.github.thomashtn.valoquests.scoring.model.ValuedMatch;
import io.github.thomashtn.valoquests.scoring.service.DailyOutputReader;
import io.github.thomashtn.valoquests.shared.dto.PageResponse;
import io.github.thomashtn.valoquests.shared.time.WeekCalendar;
import io.github.thomashtn.valoquests.shared.util.PaginationGuard;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements filtered and paginated player match-history consultation.
 */
@Service
@Transactional(readOnly = true)
public class DefaultMatchQueryService implements MatchQueryService {

    /**
     * Roster players the squad history lists: every public listing leaves archived players out.
     */
    private static final Set<PlayerStatus> SQUAD_STATUSES =
        EnumSet.complementOf(EnumSet.of(PlayerStatus.ARCHIVED));

    /**
     * Newest first, the identifier breaking ties between rows of one shared match.
     */
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "match.startedAt", "id");

    /**
     * Repository used to load tracked players.
     */
    private final PlayerRepository playerRepository;

    /**
     * Repository used to query persisted player matches.
     */
    private final PlayerMatchRepository playerMatchRepository;

    /**
     * Calendar resolving a week's instant bounds.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Resolves the campaign whose roster the squad history lists.
     */
    private final CampaignRepository campaignRepository;

    /**
     * Prices each match with both multipliers, so the history can say what a game was worth to the
     * squad and not only how it went.
     */
    private final DailyOutputReader dailyOutputReader;

    /**
     * Maps priced player matches to the history DTOs.
     */
    private final MatchResponseMapper matchResponseMapper;

    /**
     * Creates the persisted match query service.
     *
     * @param playerRepository      repository used to validate tracked players
     * @param playerMatchRepository repository used to query persisted player matches
     * @param weekCalendar          calendar resolving a week's instant bounds
     * @param dailyOutputReader     reader pricing each match with both multipliers
     * @param campaignRepository    repository resolving the campaign the site shows
     * @param matchResponseMapper   mapper building the history DTOs
     */
    public DefaultMatchQueryService(
        PlayerRepository playerRepository,
        PlayerMatchRepository playerMatchRepository,
        WeekCalendar weekCalendar,
        DailyOutputReader dailyOutputReader,
        CampaignRepository campaignRepository,
        MatchResponseMapper matchResponseMapper
    ) {
        this.playerRepository = playerRepository;
        this.playerMatchRepository = playerMatchRepository;
        this.weekCalendar = weekCalendar;
        this.dailyOutputReader = dailyOutputReader;
        this.campaignRepository = campaignRepository;
        this.matchResponseMapper = matchResponseMapper;
    }

    /**
     * Returns one filtered page of matches for a tracked player.
     *
     * @param playerId internal player identifier
     * @param page     zero-based page index
     * @param size     requested page size
     * @param filter   optional season, map, agent, result and game mode filters
     * @return requested page of player matches
     */
    @Override
    public PageResponse<MatchResponse> findByPlayer(
        long playerId,
        int page,
        int size,
        MatchHistoryFilter filter
    ) {
        PaginationGuard.assertValidPageRequest(page, size);
        if (!playerRepository.existsById(playerId)) {
            throw new PlayerNotFoundException(playerId);
        }
        PlayerMatchHistoryCriteria criteria = MatchFilterParser.toCriteria(filter, weekCalendar);
        Page<PlayerMatch> matches = playerMatchRepository.findHistory(
            playerId,
            criteria,
            PageRequest.of(page, size, NEWEST_FIRST)
        );
        List<PlayerMatch> pageMatches = matches.getContent();
        Map<Long, ValuedMatch> valuedByPlayerMatchId = valueByPlayerMatchId(
            pageMatches,
            (firstDay, lastDay) -> dailyOutputReader.readPlayer(playerId, firstDay, lastDay)
        );
        return PageResponse.from(
            matches,
            pageMatches.stream()
                .map(playerMatch -> matchResponseMapper.toResponse(playerMatch, valuedByPlayerMatchId))
                .toList()
        );
    }

    /**
     * Returns one page of the squad's matches of the day.
     *
     * <p>The roster is the shown campaign's: the live one, else the last closed one.
     *
     * @param page zero-based page index
     * @param size requested page size
     * @return requested page of the squad's matches
     */
    @Override
    public PageResponse<SquadMatchResponse> findSquad(int page, int size) {
        PaginationGuard.assertValidPageRequest(page, size);
        Optional<Campaign> campaign = campaignRepository.findShown();
        if (campaign.isEmpty()) {
            return new PageResponse<>(List.of(), page, size, 0, 0);
        }
        LocalDate today = weekCalendar.today();
        Page<PlayerMatch> matches = playerMatchRepository.findSquadHistory(
            campaign.get().getId(),
            SQUAD_STATUSES,
            weekCalendar.startOfDay(today),
            weekCalendar.endOfDay(today),
            PageRequest.of(page, size, NEWEST_FIRST)
        );
        List<PlayerMatch> pageMatches = matches.getContent();
        // Priced through the same reader as each player's own history, so both show one amount.
        Map<Long, ValuedMatch> valuedByPlayerMatchId = valueByPlayerMatchId(
            pageMatches,
            (firstDay, lastDay) -> dailyOutputReader.read(SQUAD_STATUSES, firstDay, lastDay)
        );
        return PageResponse.from(
            matches,
            pageMatches.stream()
                .map(playerMatch -> matchResponseMapper.toSquadResponse(playerMatch, valuedByPlayerMatchId))
                .toList()
        );
    }

    /**
     * Prices every match on the page.
     *
     * <p>A match's amount depends on how the rest of <em>that day</em> went and on the days played
     * before it, so the page alone cannot price itself: a page boundary routinely cuts a day in half.
     * The whole span of days the page touches is therefore read through the same reader the ranking
     * and the campaign use, which is what keeps them from disagreeing. One extra query per page.
     *
     * @param pageMatches the matches the page is about to return
     * @param reader      reads the output of an inclusive range of days
     * @return valued matches indexed by player-match identifier, unvalued matches absent
     */
    private Map<Long, ValuedMatch> valueByPlayerMatchId(
        List<PlayerMatch> pageMatches,
        BiFunction<LocalDate, LocalDate, DailyOutput> reader
    ) {
        if (pageMatches.isEmpty()) {
            return Map.of();
        }

        LocalDate firstDay = null;
        LocalDate lastDay = null;
        for (PlayerMatch playerMatch : pageMatches) {
            LocalDate day = weekCalendar.dayOf(playerMatch.getMatch().getStartedAt());
            firstDay = firstDay == null || day.isBefore(firstDay) ? day : firstDay;
            lastDay = lastDay == null || day.isAfter(lastDay) ? day : lastDay;
        }

        DailyOutput output = reader.apply(firstDay, lastDay);

        return output.valuedMatches().stream()
            .collect(Collectors.toMap(ValuedMatch::playerMatchId, Function.identity()));
    }

    /**
     * Loads full detail for one of a tracked player's matches, priced like every
     * other history entry and joined with every other tracked player found in the same match.
     *
     * @param playerId      internal player identifier
     * @param playerMatchId internal player-match identifier
     * @return the requested match's full detail
     */
    @Override
    public MatchDetailResponse findDetail(long playerId, long playerMatchId) {
        if (!playerRepository.existsById(playerId)) {
            throw new PlayerNotFoundException(playerId);
        }
        PlayerMatch playerMatch = playerMatchRepository
            .findByIdAndPlayerId(playerMatchId, playerId)
            .orElseThrow(() -> new MatchNotFoundException(playerMatchId));

        Map<Long, ValuedMatch> valuedByPlayerMatchId = valueByPlayerMatchId(
            List.of(playerMatch),
            (firstDay, lastDay) -> dailyOutputReader.readPlayer(playerId, firstDay, lastDay)
        );
        List<PlayerMatch> others = playerMatchRepository
            .findByMatchIdAndPlayerIdNot(playerMatch.getMatch().getId(), playerId);
        return matchResponseMapper.toDetail(playerMatch, valuedByPlayerMatchId, others);
    }
}
