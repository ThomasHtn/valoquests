import { ChartBar, ChartSeries } from '@shared/chart/chart.model';
import { ChallengeIcon } from '@core/challenges/challenge-visual.model';

/**
 * One color token as catalogued on the design system page.
 */
export interface ColorToken {
  readonly name: string;
  readonly cssVar: string;
  readonly swatchClass: string;
  readonly hex: string;
}

/**
 * A group of related color tokens, matching the sections of `src/styles/colors.css`.
 */
export interface ColorGroup {
  readonly label: string;
  readonly tokens: readonly ColorToken[];
}

/**
 * Every color token defined in `src/styles/colors.css`, grouped the same way, so this catalogue
 * cannot silently drift from the actual theme file.
 */
export const COLOR_GROUPS: readonly ColorGroup[] = [
  {
    label: 'Brand',
    tokens: [
      {
        name: 'brand-500',
        cssVar: '--color-brand-500',
        swatchClass: 'bg-brand-500',
        hex: '#d9954a',
      },
      {
        name: 'brand-400',
        cssVar: '--color-brand-400',
        swatchClass: 'bg-brand-400',
        hex: '#e8ab6b',
      },
    ],
  },
  {
    label: 'Surfaces',
    tokens: [
      {
        name: 'surface-sunken',
        cssVar: '--color-surface-sunken',
        swatchClass: 'bg-surface-sunken',
        hex: '#0a151d',
      },
      {
        name: 'surface-950',
        cssVar: '--color-surface-950',
        swatchClass: 'bg-surface-950',
        hex: '#0f1c26',
      },
      {
        name: 'surface-800',
        cssVar: '--color-surface-800',
        swatchClass: 'bg-surface-800',
        hex: '#1b2c3a',
      },
      {
        name: 'surface-700',
        cssVar: '--color-surface-700',
        swatchClass: 'bg-surface-700',
        hex: '#253645',
      },
      {
        name: 'surface-600',
        cssVar: '--color-surface-600',
        swatchClass: 'bg-surface-600',
        hex: '#33495b',
      },
    ],
  },
  {
    label: 'Text',
    tokens: [
      {
        name: 'text-primary',
        cssVar: '--color-text-primary',
        swatchClass: 'bg-text-primary',
        hex: '#ece8e1',
      },
      {
        name: 'text-secondary',
        cssVar: '--color-text-secondary',
        swatchClass: 'bg-text-secondary',
        hex: '#a4a7a6',
      },
      {
        name: 'text-muted',
        cssVar: '--color-text-muted',
        swatchClass: 'bg-text-muted',
        hex: '#868b8d',
      },
    ],
  },
  {
    label: 'Category accents',
    tokens: [
      {
        name: 'accent-red',
        cssVar: '--color-accent-red',
        swatchClass: 'bg-accent-red',
        hex: '#ff4655',
      },
      {
        name: 'accent-purple',
        cssVar: '--color-accent-purple',
        swatchClass: 'bg-accent-purple',
        hex: '#a78bfa',
      },
      {
        name: 'accent-gold',
        cssVar: '--color-accent-gold',
        swatchClass: 'bg-accent-gold',
        hex: '#d9954a',
      },
      {
        name: 'accent-blue',
        cssVar: '--color-accent-blue',
        swatchClass: 'bg-accent-blue',
        hex: '#5a96be',
      },
      {
        name: 'accent-green',
        cssVar: '--color-accent-green',
        swatchClass: 'bg-accent-green',
        hex: '#5fb88a',
      },
      {
        name: 'accent-cyan',
        cssVar: '--color-accent-cyan',
        swatchClass: 'bg-accent-cyan',
        hex: '#2dd4bf',
      },
      {
        name: 'accent-pink',
        cssVar: '--color-accent-pink',
        swatchClass: 'bg-accent-pink',
        hex: '#ec4899',
      },
    ],
  },
  {
    label: 'Podium & semantic',
    tokens: [
      {
        name: 'podium-bronze',
        cssVar: '--color-podium-bronze',
        swatchClass: 'bg-podium-bronze',
        hex: '#a07850',
      },
      { name: 'success', cssVar: '--color-success', swatchClass: 'bg-success', hex: '#5fb88a' },
      { name: 'danger', cssVar: '--color-danger', swatchClass: 'bg-danger', hex: '#ff4655' },
    ],
  },
];

/**
 * Every icon key {@link ChallengeIcon} accepts, in the order `app-challenge-icon-view`'s `@switch`
 * declares them.
 */
export const CHALLENGE_ICONS: readonly ChallengeIcon[] = [
  'skull',
  'crosshair',
  'trophy',
  'users',
  'star',
  'swords',
  'activity',
  'shield',
  'trending-up',
  'calendar',
  'target',
];

/**
 * Rank icon assets sampled from `public/ranks/` to illustrate {@link RankIconView}'s size presets.
 */
export const SAMPLE_RANK_ICONS: readonly { readonly src: string; readonly label: string }[] = [
  { src: '/ranks/iron-1.svg', label: 'Iron 1' },
  { src: '/ranks/gold-2.svg', label: 'Gold 2' },
  { src: '/ranks/diamond-3.svg', label: 'Diamond 3' },
  { src: '/ranks/radiant.svg', label: 'Radiant' },
];

/**
 * One option of a demo segmented control or tab strip.
 */
export interface SampleTabOption {
  readonly value: string;
  readonly label: string;
}

/**
 * Options for the segmented-control demo, illustrating the player-profile game-mode filter
 * pattern (`player-profile.html`'s primary-game-mode strip).
 */
export const SAMPLE_SEGMENT_OPTIONS: readonly SampleTabOption[] = [
  { value: 'duo', label: 'Duo' },
  { value: 'ranked', label: 'Ranked' },
  { value: 'unrated', label: 'Unrated' },
];

/**
 * Options for the underline-tabs demo, illustrating the player-profile view-mode toggle pattern.
 */
export const SAMPLE_TAB_OPTIONS: readonly SampleTabOption[] = [
  { value: 'week', label: 'Semaine' },
  { value: 'allTime', label: 'Total' },
];

/**
 * One language offered by the dropdown demo.
 */
export interface SampleDropdownLanguage {
  readonly code: string;
  readonly label: string;
}

/**
 * Languages for the dropdown demo, illustrating the sidebar's language-switcher listbox pattern.
 * Deliberately not backed by the real `Translation` service — see
 * {@link AdminDesignSystem.dropdownLanguages}.
 */
export const SAMPLE_DROPDOWN_LANGUAGES: readonly SampleDropdownLanguage[] = [
  { code: 'fr', label: 'Français' },
  { code: 'en', label: 'English' },
];

/**
 * Player status values, matching `PlayerStatus` as rendered by `admin-players`'s status badge.
 */
export const SAMPLE_PLAYER_STATUSES: readonly ('ACTIVE' | 'INACTIVE' | 'ARCHIVED')[] = [
  'ACTIVE',
  'INACTIVE',
  'ARCHIVED',
];

/**
 * API availability values behind the sidebar's status dot.
 */
export const SAMPLE_API_STATUSES: readonly ('online' | 'offline')[] = ['online', 'offline'];

/**
 * One row of the grid-based "table" demo.
 */
export interface SampleTableRow {
  readonly position: number;
  readonly name: string;
  readonly score: string;
}

/**
 * Sample rows illustrating the grid-based row pattern used instead of a native `<table>`
 * (leaderboard matrix, admin-players list).
 */
export const SAMPLE_TABLE_ROWS: readonly SampleTableRow[] = [
  { position: 1, name: 'ThomasHtn', score: '4 820' },
  { position: 2, name: 'Kaelis', score: '4 110' },
  { position: 3, name: 'Vaelune', score: '3 975' },
];

/**
 * Curves feeding the design system's `app-line-chart` sample.
 *
 * Colors come from the validated series palette, in slot order - the same rule the progression
 * view follows.
 */
export const SAMPLE_CHART_SERIES: readonly ChartSeries[] = [
  {
    label: 'Episode 10',
    color: 'var(--color-series-1)',
    points: [18, 21, 19, 24, 22, 26, 25, 28],
  },
  {
    label: 'Episode 11',
    color: 'var(--color-series-2)',
    points: [null, null, null, 30, 27, 31, 29, 33],
  },
];

/**
 * Bars feeding the design system's `app-bar-chart` sample, one of each state a bar can hold.
 */
export const SAMPLE_CHART_BARS: readonly ChartBar[] = [
  { label: 'Lun', value: 48, detail: '12 matchs', highlighted: false, muted: false },
  { label: 'Mar', value: 61, detail: '18 matchs', highlighted: true, muted: false },
  { label: 'Mer', value: 52, detail: '9 matchs', highlighted: false, muted: false },
  { label: 'Jeu', value: 100, detail: '2 matchs', highlighted: false, muted: true },
];
