package io.github.thomashtn.valoquests.challenge.entity;

import io.github.thomashtn.valoquests.challenge.model.ChallengeCategory;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.challenge.model.ChallengeRuleType;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import io.github.thomashtn.valoquests.shared.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Defines one reusable challenge from the challenge catalogue.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "challenge")
public class Challenge extends AuditableEntity {

    /**
     * Internal database identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Stable application code used by migrations and rule implementations.
     */
    @Column(nullable = false, unique = true, length = 80)
    private String code;

    /**
     * Human-readable challenge title.
     */
    @Column(nullable = false, length = 120)
    private String name;

    /**
     * User-facing challenge instructions.
     */
    @Column(nullable = false, length = 500)
    private String description;

    /**
     * Difficulty tier controlling weekly selection and reward size.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChallengeDifficulty difficulty;

    /**
     * Damage awarded when the challenge is completed.
     */
    @Column(nullable = false)
    private int damage;

    /**
     * Functional category used to diversify weekly challenge packs.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChallengeCategory category;

    /**
     * Structural rule type used by the future calculator registry.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 20)
    private ChallengeRuleType ruleType;

    /**
     * Aggregation strategy used to compute player progress.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "progress_mode", nullable = false, length = 30)
    private ProgressMode progressMode;

    /**
     * Versioned JSON rule definition interpreted by challenge calculators.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
        name = "conditions_json",
        nullable = false,
        columnDefinition = "jsonb"
    )
    private String conditionsJson;

    /**
     * Optional group preventing incompatible challenges from being selected together.
     */
    @Column(name = "exclusion_group", length = 80)
    private String exclusionGroup;

    /**
     * Whether the challenge is eligible for future weekly selection.
     */
    @Column(nullable = false)
    private boolean enabled = true;

    /**
     * Schema version of the JSON condition document.
     */
    @Column(name = "schema_version", nullable = false)
    private int schemaVersion;
}
