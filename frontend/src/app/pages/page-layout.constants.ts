/**
 * Host classes applied to every routed page.
 *
 * Pages stack their blocks — header, widgets, tables, pagination — as flex items separated by a
 * single gap, rather than each block carrying its own bottom margin. Declared once here so the
 * vertical rhythm cannot drift between pages, and so adding a block to a page never requires
 * remembering the spacing convention.
 */
export const PAGE_LAYOUT_CLASS = 'flex flex-col gap-6';
