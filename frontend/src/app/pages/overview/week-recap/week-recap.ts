import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideX } from '@lucide/angular';

import { BossApi } from '@core/boss/boss-api';
import { formatDamage } from '@core/challenges/challenge-format.utils';
import { ColonyView } from '@core/colony/colony-view';
import { resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { RankingApi } from '@core/ranking/ranking-api';
import { WeekRecapDismissal } from '@core/week-recap/week-recap-dismissal';

/**
 * What the squad's last closed week amounted to, said once at the top of the overview.
 *
 * A week used to end in silence: Monday's rollover finalized it, five new challenges appeared, and
 * nothing anywhere reported whether the boss had fallen, who had been crowned, or what the town had
 * gained by it. This is the squad's weekly appointment — the moment the collective effort of seven
 * days is stated as an outcome rather than left as a figure that quietly changed.
 *
 * Closes and stays closed for that week (see `WeekRecapDismissal`); Monday names a new one and it
 * comes back. Reads only resources the overview already loads, so it costs no extra request.
 */
@Component({
  selector: 'app-week-recap',
  imports: [TranslatePipe, RouterLink, LucideX],
  templateUrl: './week-recap.html',
  // Taken out of the layout entirely when there is nothing to report, not merely left empty: the
  // host is a flex item of `page-body`, so a zero-height box still claimed one of its 40px gutters
  // and opened the overview on a band of nothing on every week without a recap — which is most of
  // them. Bound as a style rather than a `hidden` utility because the static `block` beside it would
  // otherwise win or lose on stylesheet order; an inline display settles it outright.
  host: { class: 'block', '[style.display]': "recap() ? null : 'none'" },
})
export class WeekRecap {
  private readonly rankingApi = inject(RankingApi);
  private readonly bossApi = inject(BossApi);
  private readonly colony = inject(ColonyView);
  private readonly translation = inject(Translation);
  private readonly dismissal = inject(WeekRecapDismissal);

  /**
   * The most recently finalized week, or `null` while it loads or before any week has closed.
   */
  private readonly week = computed(() => {
    const page = resourceValue(this.rankingApi.latestFinalizedWeek, null);
    return page?.content[0] ?? null;
  });

  /**
   * That week's fight, matched by its Monday, or `null` if the history does not carry it.
   */
  private readonly fight = computed(() => {
    const weekStart = this.week()?.weekStart;
    if (weekStart === undefined) {
      return null;
    }

    const history = resourceValue(this.bossApi.history, null)?.content ?? [];
    return history.find((entry) => entry.weekStart === weekStart) ?? null;
  });

  /**
   * The recap, resolved into everything the panel prints, or `null` while it cannot be built.
   *
   * One computed rather than a dozen: every line reads the same three resources, and resolving them
   * together is what keeps a half-loaded panel from printing a champion beside a blank outcome.
   *
   * The dismissal is folded in here rather than kept as a second signal beside it. It was one, and
   * the template guarded on this alone — so closing the panel wrote the record and changed nothing
   * on screen. One guard cannot be half-used.
   */
  public readonly recap = computed(() => {
    const week = this.week();
    const fight = this.fight();
    if (week === null || fight === null || this.dismissal.isDismissed(week.weekStart)) {
      return null;
    }

    const language = this.translation.language();
    const champion = [...week.ranking].sort((left, right) => left.position - right.position)[0];
    const payout = this.colony.bosses().find((boss) => boss.weekIndex === fight.runWeekIndex);

    return {
      weekStart: week.weekStart,
      runWeekIndex: fight.runWeekIndex,
      defeated: fight.defeated,
      bossName: fight.boss.name,
      damageLabel: formatDamage(fight.totalDamageDealt, language),
      hpLabel: formatDamage(fight.effectiveHp, language),
      finisher: fight.defeatedByPlayerDisplayName,
      championName: champion?.displayName ?? null,
      championDamageLabel:
        champion === undefined ? null : formatDamage(champion.totalDamage, language),
      materialsLabel: payout?.earned ? payout.materialsLabel : null,
      efficiencyLabel: payout?.earned ? payout.efficiencyLabel : null,
      moraleLabel: this.movedOrNull(payout?.moraleLabel),
    };
  });

  /**
   * A preformatted signed figure, or `null` when it reports no movement at all.
   *
   * These labels arrive already rendered (`+5`, `-7`, `0`), so a plain truthiness check keeps `"0"`
   * — and the panel announced "0 de moral pour la colonie", which is a line about nothing. A week
   * that moved no morale simply has no morale line.
   *
   * @param label - The formatted figure, if the week has one.
   * @returns The label, or `null` when it is absent or amounts to zero.
   */
  private movedOrNull(label: string | undefined): string | null {
    if (label === undefined || label === '') {
      return null;
    }

    return /[1-9]/.test(label) ? label : null;
  }

  /**
   * Closes the panel for the week on screen.
   */
  protected dismiss(): void {
    const weekStart = this.week()?.weekStart;
    if (weekStart !== undefined) {
      this.dismissal.dismiss(weekStart);
    }
  }
}
