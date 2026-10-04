/**
 * Title icon, matched by a `@switch` since each Lucide icon is its own directive.
 */
export type TitleIcon = 'crown' | 'wrench' | 'wheat' | 'flame' | 'target';

/**
 * Icon and colour of a title; full literal classes so Tailwind's scanner finds them.
 */
export interface TitleVisual {
  /**
   * Icon of the title.
   */
  readonly icon: TitleIcon;

  /**
   * Tailwind text colour class.
   */
  readonly colorClass: string;
}
