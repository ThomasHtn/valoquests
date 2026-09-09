/**
 * Role a button plays in the interface, each mapped to one fixed color treatment below.
 *
 * `accent` and `danger-outline` read as the same weight as `secondary` but tinted, for an action
 * that is not the primary call to action yet still deserves more emphasis than a neutral one (a
 * retry after an error, restoring an archived row) or carries risk without being the dialog's own
 * destructive confirmation (removing a row, as opposed to the dialog that follows it).
 */
export type ButtonVariant =
  'primary' | 'secondary' | 'ghost' | 'accent' | 'danger' | 'danger-outline';
