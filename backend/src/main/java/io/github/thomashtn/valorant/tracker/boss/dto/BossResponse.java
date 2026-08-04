package io.github.thomashtn.valorant.tracker.boss.dto;

import io.github.thomashtn.valorant.tracker.scoring.model.BossCategory;

/**
 * Exposes the catalogue identity of one drawn boss.
 *
 * @param code        stable catalogue code
 * @param name        boss name
 * @param description boss description
 * @param imageUrl    visual asset reference, {@code null} until real artwork replaces the provisional
 *                    catalogue
 * @param category    weight class governing base hit points
 */
public record BossResponse(
    String code,
    String name,
    String description,
    String imageUrl,
    BossCategory category
) {
}
