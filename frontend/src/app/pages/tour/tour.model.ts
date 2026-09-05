/**
 * Identifier of a guided-tour step.
 *
 * Doubles as the step's translation namespace (`tour.steps.<id>.*`) and as the discriminant the
 * template switches on to render the matching illustration.
 */
export type TourStepId = 'intro' | 'base' | 'week' | 'resources' | 'challenges' | 'ranking';
