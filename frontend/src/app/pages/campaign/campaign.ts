import { Component } from '@angular/core';

import { WorkInProgress } from '@shared/work-in-progress/work-in-progress';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';

/**
 * Placeholder for the gameplay v2 screen, delivered by lot 8 of the redesign.
 */
@Component({
  selector: 'app-campaign',
  imports: [WorkInProgress],
  template: '<app-work-in-progress headingKey="campaign.title" [lot]="8" />',
  host: { class: PAGE_LAYOUT_CLASS },
})
export class Campaign {}
