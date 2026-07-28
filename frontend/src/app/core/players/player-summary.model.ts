/**
 * Synchronization status of one tracked player, as exposed by `GET /api/players`.
 *
 * Only the fields required to resolve the sidebar's global last-synchronization timestamp are
 * declared here; the endpoint also returns identity and statistics fields consumed by other
 * screens once implemented.
 */
export interface PlayerSynchronizationStatus {
  /**
   * Instant of the player's last successful synchronization, as an ISO-8601 instant, or `null`
   * when the player has never been synchronized successfully.
   */
  readonly lastSuccessfulSynchronizationAt: string | null;
}
