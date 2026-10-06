import { SynchronizationRunStatus } from './admin-synchronization.model';

/**
 * Statuses of a synchronization still in flight.
 */
export const IN_FLIGHT_SYNCHRONIZATION_STATUSES: readonly SynchronizationRunStatus[] = ['RUNNING'];

/**
 * Poll period of the running synchronization, in ms; Henrik rate limits make faster useless.
 */
export const SYNCHRONIZATION_POLL_INTERVAL_MS = 3_000;
