import {
  ArcElement,
  BarController,
  BarElement,
  CategoryScale,
  Chart,
  DoughnutController,
  LinearScale,
  LineController,
  LineElement,
  PointElement,
  Tooltip,
  TooltipOptions,
} from 'chart.js';

/**
 * Number of series the validated palette covers.
 *
 * A caller with more entities than this folds the extras away rather than generating a sixth hue:
 * see {@link SERIES_COLOR_VARIABLES}.
 */
export const SERIES_COLOR_COUNT = 5;

/**
 * Theme variables holding the chart series palette, in the order they must be assigned.
 *
 * The order is not cosmetic. The palette was validated as an ordered set against the dark page
 * surface — lightness band, chroma floor, colorblind separation between *adjacent* slots, contrast
 * — and permuting it drops the worst deuteranopia pair from ΔE 9.9 to 3.9, which is two curves a
 * colorblind reader cannot tell apart. Assign slots in sequence and re-run the data-viz validator
 * before touching either this list or the values behind it in `styles/colors.css`.
 */
const SERIES_COLOR_VARIABLES = [
  '--color-series-1',
  '--color-series-2',
  '--color-series-3',
  '--color-series-4',
  '--color-series-5',
] as const;

/**
 * Whether the Chart.js controllers, scales and elements this application uses are registered.
 *
 * Chart.js ships nothing by default so the unused half of the library tree-shakes away; the
 * registry is global, so registering once for the whole application is enough.
 */
let registered = false;

/**
 * Colors every chart borrows from the design system, resolved once per chart build.
 */
export interface ChartTheme {
  /**
   * Grid lines and axis borders, recessive enough to sit behind the data.
   */
  readonly grid: string;

  /**
   * Axis tick labels.
   */
  readonly tick: string;

  /**
   * Tooltip background.
   */
  readonly tooltipSurface: string;

  /**
   * Tooltip border.
   */
  readonly tooltipBorder: string;

  /**
   * Tooltip text.
   */
  readonly tooltipText: string;

  /**
   * The state color marking a player's strongest slot on the schedule charts.
   */
  readonly highlight: string;

  /**
   * Fill of a bar whose sample is too small to be judged.
   */
  readonly muted: string;
}

/**
 * Registers the Chart.js pieces this application draws with.
 *
 * The legend plugin is deliberately left out: every chart here that needs one renders it in HTML,
 * so it can carry each series' average beside its name and stay readable by a screen reader.
 */
export function registerChartComponents(): void {
  if (registered) {
    return;
  }
  Chart.register(
    LineController,
    BarController,
    DoughnutController,
    LineElement,
    PointElement,
    BarElement,
    ArcElement,
    LinearScale,
    CategoryScale,
    Tooltip,
  );
  registered = true;
}

/**
 * Reads a design token off the document root.
 *
 * Charts paint onto a canvas, which no stylesheet reaches, so the tokens have to be resolved in
 * script and handed to Chart.js as plain values.
 *
 * @param variable custom property to read, including its leading dashes
 * @param fallback value to use when the property is missing, as in a test document
 * @returns the token's computed value
 */
function token(variable: string, fallback: string): string {
  const value = getComputedStyle(document.documentElement).getPropertyValue(variable).trim();
  return value || fallback;
}

/**
 * Resolves any CSS colour expression — a custom property, a `color-mix()`, anything the cascade
 * understands — into the literal value the browser computed for it.
 *
 * `token()` above only reads a bare custom property; this is the general form, for a caller
 * handing Chart.js a whole expression such as `color-mix(in oklab, var(--color-brand-500) 15%,
 * transparent)`. Canvas's `fillStyle` cannot resolve `var()` or an unresolved `color-mix()` itself
 * — it wants a value already computed, which is why every colour this app hands to Chart.js has to
 * pass through here first rather than being written straight into a dataset.
 *
 * @param expression - Any valid CSS colour, as a string.
 * @returns The resolved literal colour, in the form `getComputedStyle` reports it (`rgb(...)` /
 *   `rgba(...)`).
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
 * Resolves the chart palette from the design tokens currently in force.
 *
 * @returns the colors charts draw their furniture with
 */
export function resolveChartTheme(): ChartTheme {
  return {
    grid: 'rgb(236 232 225 / 0.08)',
    tick: token('--color-text-muted', '#868b8d'),
    tooltipSurface: token('--color-surface-sunken', '#0a151d'),
    tooltipBorder: 'rgb(217 149 74 / 0.5)',
    tooltipText: token('--color-text-primary', '#ece8e1'),
    highlight: token('--color-accent-green', '#5fb88a'),
    muted: 'rgb(236 232 225 / 0.12)',
  };
}

/**
 * Backing-store scale the canvas is drawn at.
 *
 * Deliberately not the raw device pixel ratio. A chart sitting in a fractional grid column gets a
 * fractional CSS width (437.5px, say), and Chart.js floors the backing store to whole pixels — at
 * ratio 1 the browser then stretches 437 device pixels over 437.5 CSS pixels and every tick label
 * smears. Rounding the ratio up and never going below 2 makes that rounding error invisible and
 * supersamples the text, which is what keeps the bars and their labels sharp.
 *
 * @returns the ratio to render at
 */
export function chartPixelRatio(): number {
  return Math.max(Math.ceil(window.devicePixelRatio || 1), 2);
}

/**
 * Font of an axis title. Larger than a tick label, since it names the whole axis rather than one
 * value on it.
 */
export const AXIS_TITLE_FONT = {
  family: 'Barlow Condensed, sans-serif',
  size: 13,
  weight: 600,
} as const;

/**
 * Font of an axis tick label.
 */
export const AXIS_TICK_FONT = {
  family: 'Barlow Condensed, sans-serif',
  size: 13,
} as const;

/**
 * Builds an axis title, hidden when the caller has nothing to name the axis with.
 *
 * @param theme resolved chart palette
 * @param text already-translated axis name, possibly empty
 * @returns the scale title options
 */
export function axisTitleOptions(
  theme: ChartTheme,
  text: string,
): { display: boolean; text: string; color: string; font: typeof AXIS_TITLE_FONT } {
  return { display: text.length > 0, text, color: theme.tick, font: AXIS_TITLE_FONT };
}

/**
 * Resolves the color of one series.
 *
 * @param index zero-based slot, assigned by the entity's own stable rank rather than by its
 *     position in the current selection, so filtering one series out never repaints the others
 * @returns the slot's color
 */
export function resolveSeriesColor(index: number): string {
  const variable = SERIES_COLOR_VARIABLES[index % SERIES_COLOR_COUNT];
  return token(variable, '#c48235');
}

/**
 * Whether the reader asked for reduced motion.
 *
 * @returns `true` when animations must be suppressed
 */
export function prefersReducedMotion(): boolean {
  return window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;
}

/**
 * Builds the tooltip styling shared by every chart, in the direction's notched, square-cornered
 * idiom rather than Chart.js' rounded default.
 *
 * @param theme resolved chart palette
 * @returns tooltip options to spread into a chart configuration
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
