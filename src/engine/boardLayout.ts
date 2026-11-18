import { BoardBranches, BranchDir, GameMode, OpenEnd, PlacedTile } from '../types/domino';
import { findSpinnerTile, getOpenEnds } from './dominoEngine';

export const TILE_LONG = 84;
export const TILE_SHORT = 42;
export const TILE_GAP = 3;

export type Heading = 'E' | 'W' | 'N' | 'S';

export interface PositionedTile {
  placed: PlacedTile;
  x: number;
  y: number;
  width: number;
  height: number;
  orientation: 'horizontal' | 'vertical';
  /** Top pip if vertical, Left pip if horizontal */
  firstPip: number;
  /** Bottom pip if vertical, Right pip if horizontal */
  secondPip: number;
}

export interface PositionedDropZone {
  end: OpenEnd;
  x: number;
  y: number;
  width: number;
  height: number;
  heading: Heading;
}

export interface BoardLayoutResult {
  tiles: PositionedTile[];
  dropZones: PositionedDropZone[];
  hasSpinner: boolean;
  bounds: {
    minX: number;
    maxX: number;
    minY: number;
    maxY: number;
    width: number;
    height: number;
    halfSpanX: number;
    halfSpanY: number;
  };
}

function tileDimensions(heading: Heading, isDouble: boolean): {
  width: number;
  height: number;
  orientation: 'horizontal' | 'vertical';
} {
  const isHorizontalTravel = heading === 'E' || heading === 'W';
  const orientation = isDouble
    ? isHorizontalTravel
      ? 'vertical'
      : 'horizontal'
    : isHorizontalTravel
    ? 'horizontal'
    : 'vertical';

  return {
    width: orientation === 'horizontal' ? TILE_LONG : TILE_SHORT,
    height: orientation === 'horizontal' ? TILE_SHORT : TILE_LONG,
    orientation,
  };
}

function getPipsForHeading(
  placed: PlacedTile,
  heading: Heading,
  orientation: 'horizontal' | 'vertical'
): { firstPip: number; secondPip: number } {
  if (placed.isDouble) {
    return { firstPip: placed.tile.a, secondPip: placed.tile.b };
  }
  if (orientation === 'horizontal') {
    if (heading === 'E') {
      return { firstPip: placed.innerPip, secondPip: placed.outerPip };
    }
    return { firstPip: placed.outerPip, secondPip: placed.innerPip };
  } else {
    if (heading === 'S') {
      return { firstPip: placed.innerPip, secondPip: placed.outerPip };
    }
    return { firstPip: placed.outerPip, secondPip: placed.innerPip };
  }
}

interface CursorState {
  x: number;
  y: number;
  heading: Heading;
  lastTile: PositionedTile;
}

function advanceCursor(
  cursor: CursorState,
  branch: BranchDir,
  nextTile: PlacedTile | null
): { cx: number; cy: number; heading: Heading } {
  const { lastTile } = cursor;
  let heading = cursor.heading;

  // Decide if we should turn at the end of a segment (never turn right off a double)
  const canTurn = !lastTile.placed.isDouble;

  if (canTurn) {
    if (branch === 'right') {
      if (heading === 'E' && cursor.x >= 210) {
        heading = 'N';
      } else if (heading === 'N' && cursor.y <= -165) {
        heading = 'E';
      }
    } else if (branch === 'left') {
      if (heading === 'W' && cursor.x <= -210) {
        heading = 'S';
      } else if (heading === 'S' && cursor.y >= 165) {
        heading = 'W';
      }
    } else if (branch === 'up') {
      if (heading === 'N' && cursor.y <= -155) {
        heading = 'W';
      } else if (heading === 'W' && cursor.x <= -340) {
        heading = 'N';
      }
    } else if (branch === 'down') {
      if (heading === 'S' && cursor.y >= 155) {
        heading = 'E';
      } else if (heading === 'E' && cursor.x >= 340) {
        heading = 'S';
      }
    }
  }

  const isDouble = nextTile ? nextTile.isDouble : false;
  const { width, height } = tileDimensions(heading, isDouble);

  // If heading changed (90 degree corner), align flush on the outer half of lastTile
  if (heading !== cursor.heading) {
    const prevHeading = cursor.heading;
    let anchorX = lastTile.x;
    let anchorY = lastTile.y;

    if (prevHeading === 'E' && heading === 'N') {
      anchorX = lastTile.x + lastTile.width / 2 - TILE_SHORT / 2;
      anchorY = lastTile.y - lastTile.height / 2 - TILE_GAP - height / 2;
    } else if (prevHeading === 'N' && heading === 'E') {
      anchorX = lastTile.x + lastTile.width / 2 + TILE_GAP + width / 2;
      anchorY = lastTile.y - lastTile.height / 2 + TILE_SHORT / 2;
    } else if (prevHeading === 'W' && heading === 'S') {
      anchorX = lastTile.x - lastTile.width / 2 + TILE_SHORT / 2;
      anchorY = lastTile.y + lastTile.height / 2 + TILE_GAP + height / 2;
    } else if (prevHeading === 'S' && heading === 'W') {
      anchorX = lastTile.x - lastTile.width / 2 - TILE_GAP - width / 2;
      anchorY = lastTile.y + lastTile.height / 2 - TILE_SHORT / 2;
    } else if (prevHeading === 'N' && heading === 'W') {
      anchorX = lastTile.x - lastTile.width / 2 - TILE_GAP - width / 2;
      anchorY = lastTile.y - lastTile.height / 2 + TILE_SHORT / 2;
    } else if (prevHeading === 'W' && heading === 'N') {
      anchorX = lastTile.x - lastTile.width / 2 + TILE_SHORT / 2;
      anchorY = lastTile.y - lastTile.height / 2 - TILE_GAP - height / 2;
    } else if (prevHeading === 'S' && heading === 'E') {
      anchorX = lastTile.x + lastTile.width / 2 + TILE_GAP + width / 2;
      anchorY = lastTile.y + lastTile.height / 2 - TILE_SHORT / 2;
    } else if (prevHeading === 'E' && heading === 'S') {
      anchorX = lastTile.x + lastTile.width / 2 - TILE_SHORT / 2;
      anchorY = lastTile.y + lastTile.height / 2 + TILE_GAP + height / 2;
    }
    return { cx: anchorX, cy: anchorY, heading };
  }

  // Straight continuation
  let cx = lastTile.x;
  let cy = lastTile.y;
  if (heading === 'E') {
    cx = lastTile.x + lastTile.width / 2 + TILE_GAP + width / 2;
  } else if (heading === 'W') {
    cx = lastTile.x - lastTile.width / 2 - TILE_GAP - width / 2;
  } else if (heading === 'N') {
    cy = lastTile.y - lastTile.height / 2 - TILE_GAP - height / 2;
  } else if (heading === 'S') {
    cy = lastTile.y + lastTile.height / 2 + TILE_GAP + height / 2;
  }

  return { cx, cy, heading };
}

export function computeBoardLayout(
  board: BoardBranches,
  mode: GameMode
): BoardLayoutResult {
  const positioned: PositionedTile[] = [];
  const dropZones: PositionedDropZone[] = [];

  if (!board.root) {
    return {
      tiles: [],
      dropZones: [],
      hasSpinner: false,
      bounds: {
        minX: -120,
        maxX: 120,
        minY: -90,
        maxY: 90,
        width: 240,
        height: 180,
        halfSpanX: 120,
        halfSpanY: 90,
      },
    };
  }

  const rootDims = tileDimensions('E', board.root.isDouble);
  const rootPips = getPipsForHeading(board.root, 'E', rootDims.orientation);
  const rootPos: PositionedTile = {
    placed: board.root,
    x: 0,
    y: 0,
    width: rootDims.width,
    height: rootDims.height,
    orientation: rootDims.orientation,
    firstPip: rootPips.firstPip,
    secondPip: rootPips.secondPip,
  };
  positioned.push(rootPos);

  const posById = new Map<string, PositionedTile>();
  posById.set(board.root.tile.id, rootPos);

  const armCursors: Partial<Record<BranchDir, CursorState>> = {};

  // Layout right and left arms first
  (['right', 'left'] as const).forEach((branch) => {
    const initialHeading: Heading = branch === 'right' ? 'E' : 'W';
    let cursor: CursorState = {
      x: rootPos.x,
      y: rootPos.y,
      heading: initialHeading,
      lastTile: rootPos,
    };

    for (const placed of board[branch]) {
      const { cx, cy, heading } = advanceCursor(cursor, branch, placed);
      const dims = tileDimensions(heading, placed.isDouble);
      const pips = getPipsForHeading(placed, heading, dims.orientation);
      const pos: PositionedTile = {
        placed,
        x: cx,
        y: cy,
        width: dims.width,
        height: dims.height,
        orientation: dims.orientation,
        firstPip: pips.firstPip,
        secondPip: pips.secondPip,
      };
      positioned.push(pos);
      posById.set(placed.tile.id, pos);
      cursor = {
        x: cx,
        y: cy,
        heading,
        lastTile: pos,
      };
    }

    armCursors[branch] = cursor;
  });

  // Layout spinner up & down arms if spinner exists
  const spinnerPlaced = findSpinnerTile(board);
  const spinnerPos = spinnerPlaced ? posById.get(spinnerPlaced.tile.id) ?? rootPos : rootPos;

  (['up', 'down'] as const).forEach((branch) => {
    const initialHeading: Heading = branch === 'up' ? 'N' : 'S';
    let cursor: CursorState = {
      x: spinnerPos.x,
      y: spinnerPos.y,
      heading: initialHeading,
      lastTile: spinnerPos,
    };

    for (const placed of board[branch]) {
      const { cx, cy, heading } = advanceCursor(cursor, branch, placed);
      const dims = tileDimensions(heading, placed.isDouble);
      const pips = getPipsForHeading(placed, heading, dims.orientation);
      const pos: PositionedTile = {
        placed,
        x: cx,
        y: cy,
        width: dims.width,
        height: dims.height,
        orientation: dims.orientation,
        firstPip: pips.firstPip,
        secondPip: pips.secondPip,
      };
      positioned.push(pos);
      posById.set(placed.tile.id, pos);
      cursor = {
        x: cx,
        y: cy,
        heading,
        lastTile: pos,
      };
    }

    armCursors[branch] = cursor;
  });

  // Compute drop zones for every currently open end
  const openEnds = getOpenEnds(board, mode);
  for (const end of openEnds) {
    const cursor = armCursors[end.branch];
    if (!cursor) continue;
    const { cx, cy, heading } = advanceCursor(cursor, end.branch, null);
    const dims = tileDimensions(heading, false);
    dropZones.push({
      end,
      x: cx,
      y: cy,
      width: dims.width,
      height: dims.height,
      heading,
    });
  }

  // Shift entire board so the spinner tile (if active) is always pinned at (0, 0)
  const hasSpinner = Boolean(spinnerPlaced && posById.has(spinnerPlaced.tile.id));
  const anchorX = hasSpinner ? spinnerPos.x : 0;
  const anchorY = hasSpinner ? spinnerPos.y : 0;

  if (anchorX !== 0 || anchorY !== 0) {
    for (const t of positioned) {
      t.x -= anchorX;
      t.y -= anchorY;
    }
    for (const dz of dropZones) {
      dz.x -= anchorX;
      dz.y -= anchorY;
    }
  }

  // Compute bounding box including drop zones
  let minX = Infinity;
  let maxX = -Infinity;
  let minY = Infinity;
  let maxY = -Infinity;

  for (const t of positioned) {
    minX = Math.min(minX, t.x - t.width / 2);
    maxX = Math.max(maxX, t.x + t.width / 2);
    minY = Math.min(minY, t.y - t.height / 2);
    maxY = Math.max(maxY, t.y + t.height / 2);
  }
  for (const dz of dropZones) {
    minX = Math.min(minX, dz.x - dz.width / 2);
    maxX = Math.max(maxX, dz.x + dz.width / 2);
    minY = Math.min(minY, dz.y - dz.height / 2);
    maxY = Math.max(maxY, dz.y + dz.height / 2);
  }

  const padding = 32;
  minX -= padding;
  maxX += padding;
  minY -= padding;
  maxY += padding;

  const halfSpanX = Math.max(120, Math.abs(minX), Math.abs(maxX));
  const halfSpanY = Math.max(90, Math.abs(minY), Math.abs(maxY));

  return {
    tiles: positioned,
    dropZones,
    hasSpinner,
    bounds: {
      minX,
      maxX,
      minY,
      maxY,
      width: Math.max(240, maxX - minX),
      height: Math.max(180, maxY - minY),
      halfSpanX,
      halfSpanY,
    },
  };
}
