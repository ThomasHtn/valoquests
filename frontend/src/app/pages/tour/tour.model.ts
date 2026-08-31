/**
 * Identifier of a guided-tour step.
 *
 * Doubles as the step's translation namespace (`tour.steps.<id>.*`) and as the discriminant the
 * template switches on to render the matching live component.
 */
export type TourStepId = 'intro' | 'colony' | 'week' | 'ladder' | 'ranking' | 'players';
