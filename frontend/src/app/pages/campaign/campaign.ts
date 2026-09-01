import { NgTemplateOutlet } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  LucideGauge,
  LucidePackage,
  LucideUsers,
  LucideUtensils,
  LucideWheat,
} from '@lucide/angular';

import { BossCampaign } from '@core/boss/boss-campaign';
import { BossTimelineNode } from '@core/boss/boss-timeline.model';
import { ColonyView } from '@core/colony/colony-view';
import { RULE_ANCHOR } from '@core/rules/rule-anchor.constants';
import { resolveColonyDeltaColorClass } from '@core/colony/colony-visual.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { PageHeader } from '@layout/page-header/page-header';
import { LadderStrip } from '@shared/ladder-strip/ladder-strip';
import { ResourceState } from '@shared/resource-state/resource-state';
import { BossDetail } from './boss-detail/boss-detail';
import { WeekFrieze, WeekFriezeEntry } from './week-frieze/week-frieze';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';
import { CountUp } from '@shared/count-up/count-up';

/**
 * Campaign page: the run told as one screen, at rest.
 *
 * The ten weeks hang off a straight rail as milestones, alternating above and below it — solid
 * behind the week being fought, dotted ahead of it, and that week's own marker larger than the
 * nine others. A marker click unfolds its week's panel flush under the frieze and clicking it again
 * folds it back; the page stands with no panel at rest.
 *
 * Under it, the run read two ways it cannot be read anywhere else: its population curve with the
 * days still to play under a veil, and the ladder the whole economy climbs. Then the economy
 * spelled out as the chain it is, and the scoreboard of every campaign. What the week's own fight
 * is worth belongs to the drawer a marker opens, and is not restated beside it.
 */
@Component({
  selector: 'app-campaign',
  imports: [
    TranslatePipe,
    BossDetail,
    ResourceState,
    LadderStrip,
    WeekFrieze,
    NgTemplateOutlet,
    RouterLink,
    LucideGauge,
    LucidePackage,
    LucideUsers,
    CountUp,
    LucideUtensils,
    LucideWheat,
    PageHeader,
  ],
  templateUrl: './campaign.html',
  styleUrl: './campaign.css',
  host: { class: PAGE_LAYOUT_CLASS },
  providers: [BossCampaign, ColonyView],
})
export class Campaign {
  /**
   * The campaign itself: every week resolved into a display-ready node, plus the loading and error
   * state of the resources behind them.
   */
  protected readonly campaign = inject(BossCampaign);

  /**
   * The colony the campaign feeds, resolved into display-ready view models.
   */
  protected readonly colony = inject(ColonyView);

  /**
   * Named rulebook fragments, so a link into the rules lands on the beat it is about rather than at
   * the top of a four-thousand-pixel page.
   */
  protected readonly ruleAnchor = RULE_ANCHOR;

  /**
   * Week asked for in the URL as `?week=YYYY-MM-DD`, or `null` when the page was opened plain.
   *
   * Read once from the snapshot rather than followed reactively, the same restraint
   * `Leaderboard.requestedWeekStart` takes: this is a deep link from a closed week's own row on
   * `/leaderboard`, and the reader's own clicks take over from the moment they touch a marker.
   */
  private readonly requestedWeekStart = inject(ActivatedRoute).snapshot.queryParamMap.get('week');

  /**
   * Id of the node whose detail panel is open, `null` while explicitly closed, or `undefined`
   * while the reader has not touched a marker yet — the frieze's resting state, and the one a
   * reviewer should judge as the page's normal look.
   */
  private readonly selectedNodeId = signal<string | null | undefined>(undefined);

  /**
   * Whether either half of the page is still resolving, or has failed. The two are reported as one:
   * the colony and the campaign are the same run, and half a run on screen reads as a bug.
   */
  protected readonly isLoading = computed(
    () => this.campaign.isLoading() || this.colony.isLoading(),
  );
  protected readonly hasError = computed(() => this.campaign.hasError() || this.colony.hasError());

  /**
   * The ten weeks of the frieze, oldest first, each joined with what its fight is worth the colony.
   * The two are the same ten weeks, joined on the run week each of them carries — never on their
   * position in the list, which a week that closed without a fight would shift.
   */
  protected readonly nodesWithBoss = computed<readonly WeekFriezeEntry[]>(() => {
    const bossByRunWeek = new Map(this.colony.bosses().map((boss) => [boss.weekIndex, boss]));

    return this.campaign.nodes().map((node) => ({
      node,
      boss: bossByRunWeek.get(node.runWeekIndex) ?? null,
    }));
  });

  /**
   * The node whose detail panel is open, or `null` while the panel is closed.
   *
   * Falls back to {@link requestedWeekStart} only while {@link selectedNodeId} is still
   * `undefined` — a deep link opens the matching node once the campaign's nodes have loaded, but
   * never fights back a reader's own click or close.
   */
  protected readonly selectedNode = computed<BossTimelineNode | null>(() => {
    const nodes = this.campaign.nodes();
    const selected = this.selectedNodeId();

    if (selected !== undefined) {
      return nodes.find((node) => node.id === selected) ?? null;
    }

    return this.requestedWeekStart === null
      ? null
      : (nodes.find((node) => node.weekStart === this.requestedWeekStart) ?? null);
  });

  /**
   * Id of the week whose panel is unfolded, or `null` while none is.
   */
  protected readonly selectedId = computed(() => this.selectedNode()?.id ?? null);

  /**
   * What {@link selectedNode}'s fight changed in the colony, joined on the run week — never on the
   * node's position in the list, which a week that closed without a fight would shift.
   */
  protected readonly selectedBossReport = computed(() => {
    const node = this.selectedNode();
    if (node === null) {
      return null;
    }

    return this.colony.bossReports().get(node.runWeekIndex) ?? null;
  });

  /**
   * Position of {@link selectedNode} within the campaign's nodes, or `-1` while the panel is
   * closed.
   */
  private readonly selectedIndex = computed(() => {
    const selectedId = this.selectedNode()?.id;
    return selectedId === undefined
      ? -1
      : this.campaign.nodes().findIndex((node) => node.id === selectedId);
  });

  /**
   * Whether the panel can step to an earlier / later week without leaving the campaign.
   */
  protected readonly hasPreviousNode = computed(() => this.selectedIndex() > 0);
  protected readonly hasNextNode = computed(() => {
    const index = this.selectedIndex();
    return index >= 0 && index < this.campaign.nodes().length - 1;
  });

  /**
   * Text color of the population delta, by the direction the night moved.
   */
  protected readonly deltaColorClass = resolveColonyDeltaColorClass;

  /**
   * Unfolds the detail panel on one week, or folds it back when its own marker is clicked again —
   * the panel sits under the frieze rather than over it, so the marker is a toggle, not just an
   * opener.
   *
   * @param node - The week to detail.
   */
  protected select(node: BossTimelineNode): void {
    this.selectedNodeId.set(this.selectedId() === node.id ? null : node.id);
  }

  /**
   * Steps the open panel to the adjacent week, if there is one in that direction.
   *
   * @param offset - `-1` for the previous week, `1` for the next one.
   */
  protected step(offset: -1 | 1): void {
    const target = this.campaign.nodes()[this.selectedIndex() + offset];
    if (target) {
      this.selectedNodeId.set(target.id);
    }
  }

  /**
   * Closes the detail panel.
   */
  protected closePanel(): void {
    this.selectedNodeId.set(null);
  }

  /**
   * Reloads every backing resource after a failure, on both halves of the page.
   */
  protected reload(): void {
    this.campaign.reload();
    this.colony.reload();
  }
}
