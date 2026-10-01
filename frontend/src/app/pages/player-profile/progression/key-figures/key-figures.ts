import { NgOptimizedImage } from '@angular/common';
import { Component, input } from '@angular/core';
import {
  LucideArrowDownToLine,
  LucideArrowUpToLine,
  LucideChevronsDownUp,
  LucideChevronsUpDown,
  LucideGitCommitHorizontal,
  LucideMinus,
  LucideSwords,
  LucideTrendingDown,
  LucideTrendingUp,
} from '@lucide/angular';

import { KEY_FIGURE_TONE_CLASSES } from './key-figures.constants';
import { KeyFigure } from './key-figures.model';

/**
 * Strip of key figures closing a progression chart: an icon, a caption, the value and one line
 * qualifying it, so the chart's answer reads without hovering anything.
 */
@Component({
  selector: 'app-key-figures',
  imports: [
    NgOptimizedImage,
    LucideArrowDownToLine,
    LucideArrowUpToLine,
    LucideChevronsDownUp,
    LucideChevronsUpDown,
    LucideGitCommitHorizontal,
    LucideMinus,
    LucideSwords,
    LucideTrendingDown,
    LucideTrendingUp,
  ],
  templateUrl: './key-figures.html',
})
export class KeyFigures {
  /**
   * Figures to show, in reading order.
   */
  public readonly figures = input.required<readonly KeyFigure[]>();

  /**
   * Tile classes per tone.
   */
  protected readonly toneClasses = KEY_FIGURE_TONE_CLASSES;
}
