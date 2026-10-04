import { TitleKey } from './campaign-title.model';
import { TITLE_VISUALS } from './campaign-title-visual.constants';
import { TitleVisual } from './campaign-title-visual.model';

/**
 * Icon and colour of a title.
 */
export function resolveTitleVisual(title: TitleKey): TitleVisual {
  return TITLE_VISUALS[title];
}
