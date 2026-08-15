package io.github.thomashtn.valorant.tracker.player.controller;

import static io.github.thomashtn.valorant.tracker.shared.config.OpenApiConfig.ADMIN_KEY_SECURITY_SCHEME;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valorant.tracker.player.dto.PlayerAdminResponse;
import io.github.thomashtn.valorant.tracker.player.dto.PlayerCreateRequest;
import io.github.thomashtn.valorant.tracker.player.dto.PlayerDeletionResponse;
import io.github.thomashtn.valorant.tracker.player.dto.PlayerStatusUpdateRequest;
import io.github.thomashtn.valorant.tracker.player.dto.PlayerUpdateRequest;
import io.github.thomashtn.valorant.tracker.player.service.PlayerAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the protected roster-management operations.
 */
@RestController
@RequestMapping(value = "/api/admin/players", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Administration - Players", description = "Tracked roster management.")
@SecurityRequirement(name = ADMIN_KEY_SECURITY_SCHEME)
public class PlayerAdminController {

    /**
     * Application service managing the tracked roster.
     */
    private final PlayerAdminService service;

    /**
     * Creates the administrative player controller.
     *
     * @param service player administration service
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = """
            The service is a stateless Spring singleton holding nothing but its own injected
            collaborators, and this controller is one of its callers rather than its owner. Copying
            it is meaningless and would defeat the container's proxying, which is what carries the
            transaction boundaries the service declares.
            """
    )
    public PlayerAdminController(PlayerAdminService service) {
        this.service = service;
    }

    /**
     * Lists every player, archived ones included.
     *
     * @return every tracked player
     */
    @GetMapping
    @Operation(
        summary = "List every player",
        description = """
            Returns every player configured in the application, including archived ones which the
            public listing hides. Each entry reports its Riot identity, lifecycle status,
            synchronization state and whether finalized campaign data depends on it — which is what
            decides whether deleting it removes it or archives it.
            """
    )
    @ApiResponse(responseCode = "200", description = "Players returned successfully.")
    public List<PlayerAdminResponse> getPlayers() {
        return service.findAll();
    }

    /**
     * Adds a player to the tracked roster.
     *
     * @param request player identity
     * @return the created player
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Add a player to the roster",
        description = """
            Starts tracking a Valorant account. The Riot PUUID is not resolved here: the first
            synchronization of the player resolves it, so adding a player never depends on the
            Henrik API being reachable. Until then the player simply has no stored history.
            """
    )
    @ApiResponse(responseCode = "201", description = "Player added successfully.")
    @ApiResponse(responseCode = "400", description = "One or more fields are invalid.")
    @ApiResponse(responseCode = "409", description = "The Riot identity is already tracked.")
    public PlayerAdminResponse createPlayer(@Valid @RequestBody PlayerCreateRequest request) {
        return service.create(request);
    }

    /**
     * Updates the identity of a tracked player.
     *
     * @param playerId internal player identifier
     * @param request  new identity
     * @return the updated player
     */
    @PutMapping(value = "/{playerId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Update a player's identity",
        description = """
            Corrects a player's Riot identity, display name or portrait. Changing the Riot game name
            or tag line clears the stored PUUID, so the next synchronization resolves the account
            the new identity designates rather than going on importing the previous one's matches.
            The lifecycle status is changed through its own route.
            """
    )
    @ApiResponse(responseCode = "200", description = "Player updated successfully.")
    @ApiResponse(responseCode = "400", description = "One or more fields are invalid.")
    @ApiResponse(responseCode = "404", description = "Tracked player not found.")
    @ApiResponse(responseCode = "409", description = "The Riot identity belongs to another player.")
    public PlayerAdminResponse updatePlayer(
        @Parameter(description = "Internal player identifier.", example = "3", required = true)
        @PathVariable long playerId,
        @Valid @RequestBody PlayerUpdateRequest request
    ) {
        return service.update(playerId, request);
    }

    /**
     * Moves a tracked player to another lifecycle status.
     *
     * @param playerId internal player identifier
     * @param request  status to apply
     * @return the updated player
     */
    @PatchMapping(value = "/{playerId}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Change a player's status",
        description = """
            ACTIVE means the player competes in full. INACTIVE keeps synchronizing it and lets it
            complete challenges individually, but it stops contributing boss damage and stops
            consuming a ranking slot. ARCHIVED takes it off the roster entirely while keeping the
            finalized weeks that name it readable.

            This route is also how an archived player is restored, by moving it back to ACTIVE or
            INACTIVE.
            """
    )
    @ApiResponse(responseCode = "200", description = "Status applied successfully.")
    @ApiResponse(responseCode = "400", description = "The status is missing or unknown.")
    @ApiResponse(responseCode = "404", description = "Tracked player not found.")
    public PlayerAdminResponse changePlayerStatus(
        @Parameter(description = "Internal player identifier.", example = "3", required = true)
        @PathVariable long playerId,
        @Valid @RequestBody PlayerStatusUpdateRequest request
    ) {
        return service.changeStatus(playerId, request.status());
    }

    /**
     * Removes a player from the roster.
     *
     * @param playerId internal player identifier
     * @return what the request actually did
     */
    @DeleteMapping("/{playerId}")
    @Operation(
        summary = "Remove a player from the roster",
        description = """
            Deletes the player along with its match history and synchronization traces when nothing
            finalized depends on it, and archives it otherwise. A player that took part in the
            campaign cannot be deleted: a closed week may credit it with the kill that ended a boss
            or hold its ranking position, and those weeks are immutable.

            The response states which of the two happened, since the caller cannot know in advance.
            An archived player stays restorable through the status route.
            """
    )
    @ApiResponse(responseCode = "200", description = "Player deleted or archived.")
    @ApiResponse(responseCode = "404", description = "Tracked player not found.")
    public PlayerDeletionResponse deletePlayer(
        @Parameter(description = "Internal player identifier.", example = "3", required = true)
        @PathVariable long playerId
    ) {
        return service.removeFromRoster(playerId);
    }
}
