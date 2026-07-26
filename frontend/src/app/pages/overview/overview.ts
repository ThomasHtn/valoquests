import { ChangeDetectionStrategy, Component } from '@angular/core';

import { TranslatePipe } from '../../core/i18n/translate-pipe';

/**
 * Overview page.
 *
 * Landing page shown at the application root route.
 */
@Component({
  selector: 'app-overview',
  imports: [TranslatePipe],
  templateUrl: './overview.html',
  styleUrl: './overview.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Overview {}
