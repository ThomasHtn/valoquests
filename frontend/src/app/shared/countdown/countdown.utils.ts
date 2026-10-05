import { CountdownUnit } from './countdown.model';

/**
 * Slots of `secondsLeft`: days (when `withDays`), then zero-padded hours, minutes and seconds.
 * Without a days slot the hours absorb the days.
 */
export function countdownUnits(secondsLeft: number, withDays: boolean): readonly CountdownUnit[] {
  const pad = (value: number): string => String(value).padStart(2, '0');
  const totalHours = Math.floor(secondsLeft / 3_600);
  const clock: CountdownUnit[] = [
    { value: pad(withDays ? totalHours % 24 : totalHours), key: 'hours' },
    { value: pad(Math.floor(secondsLeft / 60) % 60), key: 'minutes' },
    { value: pad(secondsLeft % 60), key: 'seconds' },
  ];

  return withDays
    ? [{ value: String(Math.floor(secondsLeft / 86_400)), key: 'days' }, ...clock]
    : clock;
}
