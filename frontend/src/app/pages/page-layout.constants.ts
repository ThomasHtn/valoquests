/**
 * Host classes applied to every routed page nested under the application shell.
 *
 * The `page-stack` utility (see `styles.css`) is a non-scrolling column of exactly two items: the
 * context bar, then a `<div class="page-body">` wrapping everything else. `page-body` is where the
 * page's blocks stack as flex items separated by a single gap, and where it actually scrolls —
 * kept out of the same scroll container as the bar so the scrollbar never runs behind it. It also
 * owns the gutter and the width cap the shell deliberately no longer applies, so wrapping a page's
 * content in it is all that's needed to be laid out.
 *
 * **A page applying this class must render `<app-page-header>` as its first child, followed by a
 * single `<div class="page-body">` wrapping everything else.** The bar is not decoration: below
 * `lg` it carries the burger that opens the navigation drawer, since the sidebar contributes no bar
 * of its own there. A page without it is a phone with no way back into the navigation.
 */
export const PAGE_LAYOUT_CLASS = 'page-stack';
