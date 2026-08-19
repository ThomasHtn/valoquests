import { Component, signal } from '@angular/core';
import {
  LucideChevronLeft,
  LucideChevronRight,
  LucideEllipsisVertical,
  LucideEye,
  LucideEyeOff,
  LucideLanguages,
  LucideMenu,
  LucideTriangleAlert,
  LucideX,
} from '@lucide/angular';

import { AdminActionState, IDLE_ACTION } from '@core/admin/admin-action.model';
import { RemainingTime } from '@core/date/week-period.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { AdminActionCard } from '@pages/admin/admin-action-card/admin-action-card';
import { PAGE_LAYOUT_CLASS } from '@pages/page-layout.constants';
import { Avatar } from '@shared/avatar/avatar';
import { Button, ButtonVariant } from '@shared/button/button';
import { ChallengeIconView } from '@shared/challenge-icon-view/challenge-icon-view';
import { ChampionBadge } from '@shared/champion-badge/champion-badge';
import { ConfirmDialog } from '@shared/confirm-dialog/confirm-dialog';
import { InlineMessage } from '@shared/inline-message/inline-message';
import { NavChip } from '@shared/nav-chip/nav-chip';
import { PageHeader } from '@shared/page-header/page-header';
import { PositionBadge } from '@shared/position-badge/position-badge';
import { ProgressBar } from '@shared/progress-bar/progress-bar';
import { ProgressCircle } from '@shared/progress-circle/progress-circle';
import { RankIconView } from '@shared/rank-icon-view/rank-icon-view';
import { ResourceState } from '@shared/resource-state/resource-state';
import { SectionDivider } from '@shared/section-divider/section-divider';
import { Select } from '@shared/select/select';
import { StatTile } from '@shared/stat-tile/stat-tile';
import { StatusBadge, StatusBadgeTone } from '@shared/status-badge/status-badge';
import { TextField, TextFieldInput } from '@shared/text-field/text-field';
import { SelectOption } from '@shared/select/select.model';
import { Tooltip } from '@shared/tooltip/tooltip';
import { WeekCountdown } from '@shared/week-countdown/week-countdown';

import {
  CHALLENGE_ICONS,
  COLOR_GROUPS,
  SAMPLE_API_STATUSES,
  SAMPLE_DROPDOWN_LANGUAGES,
  SAMPLE_PLAYER_STATUSES,
  SAMPLE_RANK_ICONS,
  SAMPLE_SEGMENT_OPTIONS,
  SAMPLE_TABLE_ROWS,
  SAMPLE_TAB_OPTIONS,
} from './design-system.constants';

/**
 * Fixed mock time left before the weekly rollover, since this page renders `app-week-countdown` in
 * isolation rather than behind a real week resource.
 */
const SAMPLE_REMAINING_TIME: RemainingTime = { days: 2, hours: 14, minutes: 30 };

/**
 * Backoffice catalogue of the application's design primitives: color tokens, typography and every
 * shared component, rendered live with sample data.
 *
 * Exists to give the coach one screen to compare how a component actually renders across its
 * variants, rather than hunting through pages for each usage. Deliberately static: every input
 * below is a fixed mock value, not a resource, since this page's only job is to show what a
 * primitive looks like, not to operate on real data.
 */
@Component({
  selector: 'app-admin-design-system',
  imports: [
    AdminActionCard,
    Avatar,
    Button,
    ChallengeIconView,
    ChampionBadge,
    ConfirmDialog,
    InlineMessage,
    NavChip,
    PageHeader,
    LucideChevronLeft,
    LucideChevronRight,
    LucideEllipsisVertical,
    LucideEye,
    LucideEyeOff,
    LucideLanguages,
    LucideMenu,
    LucideTriangleAlert,
    LucideX,
    PositionBadge,
    ProgressBar,
    ProgressCircle,
    RankIconView,
    ResourceState,
    SectionDivider,
    Select,
    StatTile,
    StatusBadge,
    TextField,
    TextFieldInput,
    Tooltip,
    TranslatePipe,
    WeekCountdown,
  ],
  templateUrl: './admin-design-system.html',
  host: { class: PAGE_LAYOUT_CLASS },
})
export class AdminDesignSystem {
  /**
   * Every button variant, in the order `[appButton]` declares its `ButtonVariant` union.
   */
  protected readonly buttonVariants: readonly ButtonVariant[] = [
    'primary',
    'secondary',
    'ghost',
    'accent',
    'danger',
    'danger-outline',
  ];

  /**
   * Color token groups catalogued from `src/styles/colors.css`.
   */
  protected readonly colorGroups = COLOR_GROUPS;

  /**
   * Every icon `app-challenge-icon-view` can render.
   */
  protected readonly challengeIcons = CHALLENGE_ICONS;

  /**
   * Rank icon assets sampled to show `app-rank-icon-view`'s size presets.
   */
  protected readonly sampleRankIcons = SAMPLE_RANK_ICONS;

  /**
   * Fixed time left, feeding the `app-week-countdown` sample.
   */
  protected readonly sampleRemainingTime = SAMPLE_REMAINING_TIME;

  /**
   * Value typed into the sample plain text input.
   */
  protected readonly sampleInputValue = signal('');

  /**
   * Whether the sample password-style input reveals its value, mirroring
   * `admin-login`'s show/hide key button.
   */
  protected readonly sampleInputRevealed = signal(false);

  /**
   * Options offered by the sample `app-select`.
   */
  protected readonly selectOptions: readonly SelectOption<string>[] = [
    { value: 'iron', label: 'Iron' },
    { value: 'gold', label: 'Gold' },
    { value: 'radiant', label: 'Radiant' },
  ];

  /**
   * Value currently held by the sample `app-select`.
   */
  protected readonly selectValue = signal<string | null>('gold');

  /**
   * Whether the sample `app-confirm-dialog` is open.
   */
  protected readonly dialogOpen = signal(false);

  /**
   * Options offered by the sample segmented control (game-mode filter pattern).
   */
  protected readonly segmentOptions = SAMPLE_SEGMENT_OPTIONS;

  /**
   * Value currently held by the sample segmented control.
   */
  protected readonly segmentValue = signal(SAMPLE_SEGMENT_OPTIONS[1].value);

  /**
   * Options offered by the sample underline tabs (player-profile view-mode pattern).
   */
  protected readonly tabOptions = SAMPLE_TAB_OPTIONS;

  /**
   * Value currently held by the sample underline tabs.
   */
  protected readonly tabValue = signal(SAMPLE_TAB_OPTIONS[0].value);

  /**
   * Languages offered by the sample dropdown (sidebar language-switcher pattern). Self-contained —
   * switching it does not touch the real `Translation` service, since this is a static illustration
   * of the pattern, not the sidebar's own switcher.
   */
  protected readonly dropdownLanguages = SAMPLE_DROPDOWN_LANGUAGES;

  /**
   * Value currently held by the sample language dropdown.
   */
  protected readonly dropdownValue = signal(SAMPLE_DROPDOWN_LANGUAGES[0].code);

  /**
   * Whether the sample language dropdown's panel is open.
   */
  protected readonly dropdownOpen = signal(false);

  /**
   * Whether the sample overflow menu (player-profile "more game modes" pattern) is open.
   */
  protected readonly overflowMenuOpen = signal(false);

  /**
   * Value currently checked in the sample overflow menu.
   */
  protected readonly overflowMenuValue = signal('a');

  /**
   * Player status values, in the order `admin-players` renders their badge.
   */
  protected readonly playerStatuses = SAMPLE_PLAYER_STATUSES;

  /**
   * API availability values behind the sidebar's status dot.
   */
  protected readonly apiStatuses = SAMPLE_API_STATUSES;

  /**
   * Sample rows for the grid-based "table" pattern (leaderboard matrix / admin-players list).
   */
  protected readonly tableRows = SAMPLE_TABLE_ROWS;

  /**
   * State driving the sample `app-admin-action-card`, cycled through `idle` → `running` → `done` on
   * each click to illustrate the component without a real backend command behind it.
   */
  protected readonly actionCardState = signal<AdminActionState>(IDLE_ACTION);

  /**
   * Maps a roster status to the tone `app-status-badge` renders it with, the same way
   * `admin-players` does on its own rows.
   *
   * @param status - The status to map.
   * @returns The badge tone for that status.
   */
  protected playerStatusTone(status: 'ACTIVE' | 'INACTIVE' | 'ARCHIVED'): StatusBadgeTone {
    return status === 'ACTIVE' ? 'brand' : status === 'INACTIVE' ? 'neutral' : 'danger';
  }

  /**
   * Runs one cycle of {@link actionCardState}, mirroring how a real backoffice command reports its
   * outcome next to the button that triggered it.
   */
  protected runActionCardDemo(): void {
    this.actionCardState.set({ status: 'running', message: '' });
    setTimeout(() => {
      this.actionCardState.set({ status: 'done', message: 'Opération terminée avec succès.' });
    }, 900);
  }

  /**
   * Opens or closes the sample language dropdown's panel.
   */
  protected toggleDropdown(): void {
    this.dropdownOpen.update((open) => !open);
  }

  /**
   * Selects `code` in the sample language dropdown and closes its panel.
   *
   * @param code - The option to select.
   */
  protected selectDropdownValue(code: string): void {
    this.dropdownValue.set(code);
    this.dropdownOpen.set(false);
  }

  /**
   * Opens or closes the sample overflow menu.
   */
  protected toggleOverflowMenu(): void {
    this.overflowMenuOpen.update((open) => !open);
  }

  /**
   * Checks `value` in the sample overflow menu and closes it.
   *
   * @param value - The option to select.
   */
  protected selectOverflowMenuOption(value: string): void {
    this.overflowMenuValue.set(value);
    this.overflowMenuOpen.set(false);
  }
}
