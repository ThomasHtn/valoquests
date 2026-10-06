import {
  BarController,
  BarElement,
  CategoryScale,
  Chart,
  Filler,
  LinearScale,
  LineController,
  LineElement,
  PointElement,
  ScatterController,
  Tooltip,
  TooltipOptions,
} from 'chart.js';

import {
  AXIS_TITLE_FONT,
  SERIES_COLOR_COUNT,
  SERIES_COLOR_VARIABLES,
} from './chart-theme.constants';
import { ChartTheme } from './chart-theme.model';

/**
 * Chart.js registry is global, so registering once is enough.
 */
let registered = false;

/**
 * Registers the Chart.js pieces the app uses; no legend plugin, legends are HTML.
 */
export function registerChartComponents(): void {
  if (registered) {
    return;
  }
  Chart.register(
    LineController,
    BarController,
    ScatterController,
    LineElement,
    PointElement,
    BarElement,
    LinearScale,
    CategoryScale,
    Tooltip,
    Filler,
  );
  registered = true;
}

/**
 * Design token read off the root (canvas is unreachable by CSS), `fallback` when missing.
 */
export function token(variable: string, fallback: string): string {
  const value = getComputedStyle(document.documentElement).getPropertyValue(variable).trim();
  return value || fallback;
}

/**
 * Any CSS color expression as a computed `rgb()` literal, since canvas cannot resolve `var()`.
 */
export function resolveCssColor(expression: string): string {
  const probe = document.createElement('span');
  probe.style.color = expression;
  document.body.appendChild(probe);
  const resolved = getComputedStyle(probe).color;
  probe.remove();

  return resolved;
}

/**
 * Chart palette from the current design tokens.
 */
export function resolveChartTheme(): ChartTheme {
  // Translucent fills stay literals: canvas cannot mix a token; they are text-primary and brand-500.
  return {
    grid: 'rgb(236 232 225 / 0.08)',
    tick: token('--color-text-muted', '#8f9496'),
    tooltipSurface: token('--color-surface-sunken', '#0a151d'),
    tooltipBorder: 'rgb(217 149 74 / 0.5)',
    tooltipText: token('--color-text-primary', '#ece8e1'),
    highlight: token('--color-accent-green', '#5fb88a'),
    bar: 'rgb(217 149 74 / 0.55)',
    muted: 'rgb(236 232 225 / 0.12)',
  };
}

/**
 * Canvas backing-store scale, at least 2: at ratio 1 a fractional CSS width smears the labels
 * because Chart.js floors the backing store to whole pixels.
 */
export function chartPixelRatio(): number {
  return Math.max(Math.ceil(window.devicePixelRatio || 1), 2);
}

/**
 * Axis title options, hidden when `text` is empty.
 */
export function axisTitleOptions(
  theme: ChartTheme,
  text: string,
): { display: boolean; text: string; color: string; font: typeof AXIS_TITLE_FONT } {
  return { display: text.length > 0, text, color: theme.tick, font: AXIS_TITLE_FONT };
}

/**
 * Series color of a slot; pass a stable rank so filtering never repaints the others.
 */
export function resolveSeriesColor(index: number): string {
  const variable = SERIES_COLOR_VARIABLES[index % SERIES_COLOR_COUNT];
  return token(variable, '#c48235');
}

/**
 * Whether the reader asked for reduced motion.
 */
export function prefersReducedMotion(): boolean {
  return window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;
}

/**
 * Square-cornered tooltip styling shared by every chart.
 */
export function chartTooltipOptions(theme: ChartTheme): Partial<TooltipOptions> {
  return {
    backgroundColor: theme.tooltipSurface,
    borderColor: theme.tooltipBorder,
    borderWidth: 1,
    cornerRadius: 0,
    padding: 12,
    titleColor: theme.tooltipText,
    bodyColor: theme.tooltipText,
    titleFont: { family: 'Barlow Condensed, sans-serif', size: 15, weight: 600 },
    bodyFont: { family: 'Barlow Condensed, sans-serif', size: 15 },
    displayColors: false,
  };
}
