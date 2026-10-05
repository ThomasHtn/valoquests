/**
 * Title icon key, mapped to its Lucide icon by the title badge.
 */
export type TitleIcon = 'crown' | 'wrench' | 'wheat' | 'flame' | 'target';

/**
 * Icon and colour of a title.
 */
export interface TitleVisual {
  /**
   * Icon of the title.
   */
  readonly icon: TitleIcon;

  /**
   * CSS colour of the title (`var(--color-accent-cyan)`), bound as `--tone`.
   */
  readonly tone: string;
}
