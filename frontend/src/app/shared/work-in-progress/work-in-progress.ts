import { Component, input } from '@angular/core';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { PageHeader } from '@layout/page-header/page-header';

/**
 * Placeholder body of a page whose gameplay v2 screen is not built yet.
 *
 * Temporary by design: each routed page that renders it is replaced by its real screen in a later
 * lot of the redesign, and this component goes with the last of them. It keeps the route, the
 * navigation entry and the context bar alive meanwhile, so a phone still has its way back into
 * the navigation.
 */
@Component({
  selector: 'app-work-in-progress',
  imports: [TranslatePipe, PageHeader],
  templateUrl: './work-in-progress.html',
})
export class WorkInProgress {
  /**
   * Translation key of the page's heading.
   */
  public readonly headingKey = input.required<string>();

  /**
   * Lot of the redesign that delivers the real screen.
   */
  public readonly lot = input.required<number>();
}
