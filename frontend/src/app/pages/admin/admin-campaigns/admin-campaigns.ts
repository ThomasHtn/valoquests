import { Component } from '@angular/core';

import { WorkInProgress } from '@shared/work-in-progress/work-in-progress';
import { PAGE_LAYOUT_CLASS } from '../../page-layout.constants';

/**
 * Placeholder for the gameplay v2 screen, delivered by lot 11 of the redesign.
 */
@Component({
  selector: 'app-admin-campaigns',
  imports: [WorkInProgress],
  template: '<app-work-in-progress headingKey="admin.campaigns.title" [lot]="11" />',
  host: { class: PAGE_LAYOUT_CLASS },
})
export class AdminCampaigns {}
