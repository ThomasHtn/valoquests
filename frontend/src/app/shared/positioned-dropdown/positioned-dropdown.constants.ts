/**
 * Gap in px between the trigger and the panel.
 */
export const DROPDOWN_GAP_PX = 8;

/**
 * Height cap in px of the panel's list, the `menu-scroll` 24rem.
 */
export const DROPDOWN_LIST_MAX_PX = 384;

/**
 * Room in px needed below the trigger before opening upwards: the list cap plus the gap.
 */
export const DROPDOWN_ROOM_PX = DROPDOWN_LIST_MAX_PX + DROPDOWN_GAP_PX;
