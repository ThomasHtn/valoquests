/**
 * Days in a game week, Monday to Sunday.
 */
export const WEEK_DAYS = 7;

/**
 * Time zone the backend's `WeekCalendar` resolves campaign days and weeks in.
 */
export const CAMPAIGN_TIME_ZONE = 'Europe/Paris';

/**
 * Writes a time of day as `HH:MM` on the campaign's clock, whatever the reader's own zone.
 */
export const CAMPAIGN_CLOCK = new Intl.DateTimeFormat('fr-FR', {
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
  timeZone: CAMPAIGN_TIME_ZONE,
});
