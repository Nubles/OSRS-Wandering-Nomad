import bounds from './mapBounds.json';

/** Where map.jpg sits in world tiles: 1 pixel per tile, with y growing northward. */
export const MAP = bounds as { x0: number; x1: number; yTop: number; yBottom: number };
