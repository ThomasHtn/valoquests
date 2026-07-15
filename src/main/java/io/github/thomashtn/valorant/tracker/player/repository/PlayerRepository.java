package io.github.thomashtn.valorant.tracker.player.repository;

import io.github.thomashtn.valorant.tracker.player.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides persistence operations for player entities.
 */
public interface PlayerRepository extends JpaRepository<Player, Long> {
}
