import { NgOptimizedImage } from '@angular/common';
import { Component, input } from '@angular/core';

import { LucideDynamicIcon } from '@lucide/angular';

import { KEY_FIGURE_ICONS, KEY_FIGURE_TONE_MODIFIERS } from './key-figures.constants';
import { KeyFigure } from './key-figures.model';

/**
 * Key figures closing a progression chart, so its answer reads without hovering.
 */
@Component({
  selector: 'app-key-figures',
  imports: [LucideDynamicIcon, NgOptimizedImage],
  templateUrl: './key-figures.html',
  styleUrl: './key-figures.scss',
})
export class KeyFigures {
  /**
   * Figures to show, in reading order.
   */
  public readonly figures = input.required<readonly KeyFigure[]>();

  /**
   * Lucide icon of each pictogram.
   */
  protected readonly icons = KEY_FIGURE_ICONS;

  /**
   * Icon tile modifier per tone.
   */
  protected readonly toneModifiers = KEY_FIGURE_TONE_MODIFIERS;
}
