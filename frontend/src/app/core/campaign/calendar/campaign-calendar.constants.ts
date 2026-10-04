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

/**
 * Formatter reading an instant's wall-clock fields in the campaign time zone.
 */
export const CAMPAIGN_WALL_CLOCK = new Intl.DateTimeFormat('en-US', {
  timeZone: CAMPAIGN_TIME_ZONE,
  hourCycle: 'h23',
  year: 'numeric',
  month: 'numeric',
  day: 'numeric',
  hour: 'numeric',
  minute: 'numeric',
  second: 'numeric',
});
