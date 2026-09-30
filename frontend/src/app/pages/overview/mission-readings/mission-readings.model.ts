/**
 * One operator's segment of the duel track, with what its bubble spells out.
 */
export interface Strike {
  /**
   * Internal identifier of the player.
   */
  readonly playerId: number;

  /**
   * Display name of the operator.
   */
  readonly name: string;

  /**
   * Hit points the operator took from the guardian, the segment's weight.
   */
  readonly damage: number;

  /**
   * The damage, formatted for the bubble.
   */
  readonly damageLabel: string;

  /**
   * Share of the squad's damage, in whole percent.
   */
  readonly percent: number;

  /**
   * Challenge points the operator earned this week; they never touch the guardian.
   */
  readonly challengePoints: number;

  /**
   * The bubble's content as one sentence, for assistive technology.
   */
  readonly summary: string;
}
