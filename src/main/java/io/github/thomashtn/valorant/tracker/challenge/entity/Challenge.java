package io.github.thomashtn.valorant.tracker.challenge.entity;

import io.github.thomashtn.valorant.tracker.challenge.model.*;
import io.github.thomashtn.valorant.tracker.shared.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Represents the challenge component.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "challenge")
public class Challenge extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChallengeDifficulty difficulty;

    @Column(nullable = false)
    private int points;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChallengeCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 20)
    private ChallengeRuleType ruleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "progress_mode", nullable = false, length = 30)
    private ProgressMode progressMode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conditions_json", nullable = false, columnDefinition = "jsonb")
    private String conditionsJson;

    @Column(name = "exclusion_group", length = 80)
    private String exclusionGroup;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "schema_version", nullable = false)
    private int schemaVersion;
}
