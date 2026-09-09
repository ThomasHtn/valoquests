import { InlineMessageTone } from './inline-message.model';

/**
 * Rule colour and text colour of each tone.
 */
export const TONE_CLASS: Record<InlineMessageTone, string> = {
  info: 'border-brand-500/60 text-text-secondary',
  success: 'border-success/60 text-success',
  danger: 'border-danger/60 text-danger',
};
