// The shapes of `POST /avatar/dress` in `api/openapi.yaml` (DressRequest, DressedAvatar). Integer mm throughout.

export type Measurements = {
  height: number;
  shoulders: number;
  chest: number;
  waist: number;
  hips: number;
  arm: number;
  inseam: number;
};

export type GarmentSize = Partial<Record<'chest' | 'waist' | 'hips' | 'length' | 'sleeve' | 'inseam', number>>;

export type DressRequest = { measurements: Measurements; items: Array<{ id: string; size?: GarmentSize }> };

export type FigureLandmarks = {
  head: number;
  neck: number;
  shoulders: number;
  chest: number;
  waist: number;
  hips: number;
  crotch: number;
  knee: number;
  ankle: number;
  floor: number;
};

export type Figure = {
  landmarks: FigureLandmarks;
  halfWidths: { shoulders: number; chest: number; waist: number; hips: number };
};

export type Shape = {
  top: number;
  bottom: number;
  topHalfWidth: number;
  bottomHalfWidth: number;
  /** Absent when the type has no sleeves or the size gives no sleeve. */
  sleeveEnd?: number;
  clamped: boolean;
};

export type FitPart = 'CHEST' | 'WAIST' | 'HIPS' | 'SLEEVE' | 'INSEAM';
export type FitKind = 'TOO_TIGHT' | 'TOO_LOOSE' | 'SLEEVE_SHORT' | 'SLEEVE_LONG' | 'LEG_SHORT' | 'LEG_LONG';
export type FitNote = { kind: FitKind | string; part: FitPart | string };

export type Layer = { itemId: string; type: string; shape?: Shape; fit: FitNote[] };

export type DressedAvatar = { figure: Figure; layers: Layer[] };
