import { NgOptimizedImage } from '@angular/common';
import { Component, input } from '@angular/core';
import {
  LucideArrowDownToLine,
  LucideArrowUpToLine,
  LucideChevronsDownUp,
  LucideChevronsUpDown,
  LucideGitCommitHorizontal,
  LucideMinus,
  LucideTrendingDown,
  LucideTrendingUp,
  LucideDynamicIcon,
} from '@lucide/angular';

import { KEY_FIGURE_TONE_CLASSES } from './key-figures.constants';
import { KeyFigure } from './key-figures.model';
import { CONCEPT_ICONS } from '@core/concepts/concept.constants';

/**
 * Strip of key figures closing a progression chart: an icon, a caption, the value and one line
 * qualifying it, so the chart's answer reads without hovering anything.
 */
@Component({
  selector: 'app-key-figures',
  imports: [
    LucideDynamicIcon,
    NgOptimizedImage,
    LucideArrowDownToLine,
    LucideArrowUpToLine,
    LucideChevronsDownUp,
    LucideChevronsUpDown,
    LucideGitCommitHorizontal,
    LucideMinus,
    LucideTrendingDown,
    LucideTrendingUp,
  ],
  templateUrl: './key-figures.html',
})
export class KeyFigures {
  /**
   * The one icon of each concept, read by the template's `svg[lucideIcon]`.
   */
  protected readonly concepts = CONCEPT_ICONS;

  /**
   * Figures to show, in reading order.
   */
  public readonly figures = input.required<readonly KeyFigure[]>();

  /**
   * Tile classes per tone.
   */
  protected readonly toneClasses = KEY_FIGURE_TONE_CLASSES;
}
