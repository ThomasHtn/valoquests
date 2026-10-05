import { Component, input } from '@angular/core';
import { LucideLoaderCircle } from '@lucide/angular';

import { SpinnerSize } from './spinner.model';

/**
 * Turning loader icon for a pending action; decorative, the caller words the wait.
 */
@Component({
  selector: 'app-spinner',
  imports: [LucideLoaderCircle],
  templateUrl: './spinner.html',
  styleUrl: './spinner.scss',
  host: {
    class: 'spinner',
    '[class.spinner--small]': "size() === 'sm'",
    'aria-hidden': 'true',
  },
})
export class Spinner {
  /**
   * `md` inside a button, `sm` beside a caption.
   */
  public readonly size = input<SpinnerSize>('md');
}
