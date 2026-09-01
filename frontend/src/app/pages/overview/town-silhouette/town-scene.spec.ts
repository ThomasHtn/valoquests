import { describe, expect, it } from 'vitest';

import { buildTownScene, hasWaterAt, reflectionsFor, starsFor } from './town-scene';

/** Step of every named tier, plus the numbered citadels a strong run reaches past them. */
const NAMED_STEPS = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11];
const CITADEL_STEPS = [12, 13, 14, 15, 16, 17];

const litCells = (scene: ReturnType<typeof buildTownScene>): number =>
  scene.buildings
    .flatMap((building) => building.shapes)
    .filter((shape) => shape.kind === 'rect' && shape.fill === '#ffc477').length;

/** Lamps actually burning on the street, counted by the halo each one throws. */
const litGlow = (scene: ReturnType<typeof buildTownScene>): number =>
  scene.furniture.filter((shape) => shape.kind === 'ellipse' && shape.fill === '#ffc477').length;

describe('buildTownScene', () => {
  it('builds something at every step, named or numbered', () => {
    for (const step of [...NAMED_STEPS, ...CITADEL_STEPS]) {
      const scene = buildTownScene(step);

      expect(scene.buildings.length, `step ${step}`).toBeGreaterThan(0);
      expect(scene.terraces.length, `step ${step}`).toBeGreaterThan(0);
      expect(scene.foreground.length, `step ${step}`).toBeGreaterThan(0);
    }
  });

  it('retires the first houses once the waterfront is rebuilt over them', () => {
    const camp = buildTownScene(0).buildings.length;
    const borough = buildTownScene(3).buildings.length;
    const town = buildTownScene(4).buildings.length;

    // the camp's four structures stand through the borough; the town drops all four and puts up one
    // mid-rise in their place
    expect(camp).toBe(4);
    expect(borough).toBeGreaterThan(camp);
    expect(town).toBe(borough - 4 + 1);
  });

  it('keeps the frontage centred on what it actually contains', () => {
    for (const step of NAMED_STEPS) {
      const scene = buildTownScene(step);
      const left = Math.min(...scene.buildings.map((building) => building.x));
      const right = Math.max(...scene.buildings.map((building) => building.x + building.w));

      expect((left + right) / 2 + scene.frontageOffset, `step ${step}`).toBeCloseTo(360, 5);
    }
  });

  it('digs the water out only once the colony has grown past its camp', () => {
    expect(hasWaterAt(0)).toBe(false);
    expect(hasWaterAt(1)).toBe(true);
    expect(hasWaterAt(11)).toBe(true);
  });

  it('reflects every building on the water, and nothing without one', () => {
    const city = buildTownScene(5);

    expect(reflectionsFor(city.buildings).length).toBeGreaterThan(city.buildings.length);
    expect(reflectionsFor([])).toHaveLength(0);
  });

  it('lights more windows as the population fills what the food can feed', () => {
    expect(litCells(buildTownScene(8, 1))).toBeGreaterThan(litCells(buildTownScene(8, 0)));
  });

  it('clamps a population outside its nominal range instead of inverting the scene', () => {
    expect(buildTownScene(5, 4).buildings).toEqual(buildTownScene(5, 1).buildings);
  });

  /*
   * The quay is full at the twelfth step, so the numbered citadels a strong run reaches have to keep
   * changing the picture some other way — otherwise the last third of the campaign is spent looking
   * at one drawing. They raise the city on the horizon instead.
   */
  it('keeps the frontage as it is past the last placed step', () => {
    expect(buildTownScene(17).buildings).toEqual(buildTownScene(11).buildings);
  });

  it('keeps raising the city behind it, one numbered citadel at a time', () => {
    const heights = [11, 12, 14, 17].map((step) =>
      buildTownScene(step)
        .backdrop.filter((shape) => shape.kind === 'rect')
        .reduce((highest, shape) => Math.min(highest, shape.y), Number.POSITIVE_INFINITY),
    );

    expect(heights[1]).toBeLessThan(heights[0]);
    expect(heights[2]).toBeLessThan(heights[1]);
    expect(heights[3]).toBeLessThan(heights[2]);
  });

  it('stops raising it once the horizon is closed, rather than off the top of the frame', () => {
    expect(buildTownScene(40).backdrop).toEqual(buildTownScene(17).backdrop);
  });

  /*
   * Turnout is the scene's only daily reading: the step moves about once a week and the lit windows
   * move by a percent a day, so without this the drawing is the same drawing from one Monday to the
   * next. It has to change the picture, and it has to leave the street standing when nobody came.
   */
  it('lights more of the street the more of the squad played today', () => {
    expect(litGlow(buildTownScene(8, 0.6, 1))).toBeGreaterThan(litGlow(buildTownScene(8, 0.6, 0)));
  });

  it('keeps the street itself on an evening nobody played', () => {
    const dead = buildTownScene(8, 0.6, 0);

    expect(litGlow(dead)).toBe(0);
    expect(dead.furniture.length).toBeGreaterThan(0);
  });

  it('clamps a turnout outside its nominal range instead of emptying the street', () => {
    expect(buildTownScene(8, 0.6, 4).furniture).toEqual(buildTownScene(8, 0.6, 1).furniture);
    expect(buildTownScene(8, 0.6, -1).furniture).toEqual(buildTownScene(8, 0.6, 0).furniture);
  });

  /*
   * The site is what makes the town grow a little every day rather than a street at a time: it is
   * the quarter the colony is currently paying for, standing under scaffolding, and the page reveals
   * it in proportion to the materials banked.
   */
  it('puts the next quarter on site', () => {
    expect(buildTownScene(6).construction.length).toBeGreaterThan(0);
  });

  it('leaves the site unlit, so a half-built quarter is not a finished one', () => {
    const windows = buildTownScene(6).construction.filter(
      (shape) => shape.kind === 'rect' && shape.fill === '#ffc477',
    );

    expect(windows).toHaveLength(0);
  });

  /* The quay is full at the last placed step, so there is nothing left to put on site. */
  it('closes the site once the waterfront is finished', () => {
    expect(buildTownScene(11).construction).toHaveLength(0);
    expect(buildTownScene(17).construction).toHaveLength(0);
  });

  /*
   * The camp is four huts and one plot being built on. Centring on the huts alone left the site
   * hanging off the right-hand side of the frame, which is the one thing it must not do — it is what
   * the reader is being asked to watch.
   */
  it('centres the frontage on the site as well as on what is built', () => {
    const camp = buildTownScene(0);
    const rightmost = 176 + 56;

    expect(camp.frontageOffset).toBeCloseTo(360 - (22 + rightmost) / 2, 5);
  });
});

describe('starsFor', () => {
  it('keeps the night starry at every morale, and only thins it out', () => {
    const worst = starsFor(0);
    const best = starsFor(1);

    expect(worst.length).toBeGreaterThan(0);
    expect(best.length).toBeGreaterThan(worst.length);
  });

  it('puts the same stars out one by one rather than reshuffling the sky', () => {
    const fewer = starsFor(0.2);
    const more = starsFor(0.8);

    expect(more.slice(0, fewer.length)).toEqual(fewer);
  });

  it('clamps a morale outside its nominal range', () => {
    expect(starsFor(-1)).toEqual(starsFor(0));
    expect(starsFor(9)).toEqual(starsFor(1));
  });
});
