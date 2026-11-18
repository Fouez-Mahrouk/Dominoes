import React from 'react';
import { DominoTile } from '../types/domino';

// Standard 3x3 grid cell coordinates (row 0..2, col 0..2) for pips 0..6
const PIP_POSITIONS_VERTICAL: Record<number, [number, number][]> = {
  0: [],
  1: [[1, 1]],
  2: [
    [0, 2],
    [2, 0],
  ],
  3: [
    [0, 2],
    [1, 1],
    [2, 0],
  ],
  4: [
    [0, 0],
    [0, 2],
    [2, 0],
    [2, 2],
  ],
  5: [
    [0, 0],
    [0, 2],
    [1, 1],
    [2, 0],
    [2, 2],
  ],
  6: [
    [0, 0],
    [0, 2],
    [1, 0],
    [1, 2],
    [2, 0],
    [2, 2],
  ],
};

// When tile is horizontal, 6s run horizontally (2 rows of 3)
const PIP_POSITIONS_HORIZONTAL: Record<number, [number, number][]> = {
  0: [],
  1: [[1, 1]],
  2: [
    [0, 2],
    [2, 0],
  ],
  3: [
    [0, 2],
    [1, 1],
    [2, 0],
  ],
  4: [
    [0, 0],
    [0, 2],
    [2, 0],
    [2, 2],
  ],
  5: [
    [0, 0],
    [0, 2],
    [1, 1],
    [2, 0],
    [2, 2],
  ],
  6: [
    [0, 0],
    [0, 1],
    [0, 2],
    [2, 0],
    [2, 1],
    [2, 2],
  ],
};

interface PipFaceProps {
  value: number;
  horizontal: boolean;
  pipSizeClass: string;
  pipStyle?: React.CSSProperties;
}

const PipFace: React.FC<PipFaceProps> = ({
  value,
  horizontal,
  pipSizeClass,
  pipStyle,
}) => {
  const coords = horizontal
    ? PIP_POSITIONS_HORIZONTAL[value] ?? []
    : PIP_POSITIONS_VERTICAL[value] ?? [];

  return (
    <div className="relative grid grid-cols-3 grid-rows-3 w-full h-full p-[15%] place-items-center">
      {coords.map(([r, c], idx) => (
        <span
          key={idx}
          style={{ ...pipStyle, gridRowStart: r + 1, gridColumnStart: c + 1 }}
          className={`${pipSizeClass} rounded-full transition-colors bg-[#FAF8F4] shadow-[inset_0_1px_1px_rgba(0,0,0,0.45)] dark:bg-[#171B22] dark:shadow-[inset_0_1px_1px_rgba(255,255,255,0.4)]`}
        />
      ))}
    </div>
  );
};

export interface DominoTileViewProps {
  tile: DominoTile;
  orientation?: 'vertical' | 'horizontal';
  /** First pip (top when vertical, left when horizontal) */
  firstPip?: number;
  /** Second pip (bottom when vertical, right when horizontal) */
  secondPip?: number;
  /** Exact pixel width/height for board layout, or omit for preset size */
  width?: number;
  height?: number;
  size?: 'xs' | 'sm' | 'md' | 'hand';
  isSelected?: boolean;
  isPlayable?: boolean;
  isDimmed?: boolean;
  isSpinner?: boolean;
  isNew?: boolean;
  className?: string;
  onPointerDown?: (e: React.PointerEvent<HTMLDivElement>) => void;
  onPointerMove?: (e: React.PointerEvent<HTMLDivElement>) => void;
  onPointerUp?: (e: React.PointerEvent<HTMLDivElement>) => void;
  onPointerCancel?: (e: React.PointerEvent<HTMLDivElement>) => void;
  onClick?: (e: React.MouseEvent<HTMLDivElement>) => void;
}

const SIZE_CLASSES: Record<'xs' | 'sm' | 'md' | 'hand', { vert: string; horiz: string; pip: string }> = {
  xs: { vert: 'w-7 h-14', horiz: 'w-14 h-7', pip: 'w-1.5 h-1.5' },
  sm: { vert: 'w-9 h-[72px]', horiz: 'w-[72px] h-9', pip: 'w-2 h-2' },
  md: { vert: 'w-11 h-[88px]', horiz: 'w-[88px] h-11', pip: 'w-2 h-2' },
  hand: { vert: 'w-[46px] h-[92px] sm:w-[52px] sm:h-[104px]', horiz: 'w-[92px] h-[46px]', pip: 'w-2 h-2 sm:w-2.5 sm:h-2.5' },
};

export const DominoTileView: React.FC<DominoTileViewProps> = ({
  tile,
  orientation = 'vertical',
  firstPip,
  secondPip,
  width,
  height,
  size = 'md',
  isSelected = false,
  isPlayable = false,
  isDimmed = false,
  isSpinner = false,
  isNew = false,
  className = '',
  onPointerDown,
  onPointerMove,
  onPointerUp,
  onPointerCancel,
  onClick,
}) => {
  const isHorizontal = orientation === 'horizontal';
  const topOrLeft = firstPip ?? tile.a;
  const bottomOrRight = secondPip ?? tile.b;

  const sizeInfo = SIZE_CLASSES[size];
  const minorAxis = width && height ? Math.min(width, height) : null;
  const pipStyle = minorAxis
    ? {
        width: `${Math.max(2.5, Math.round(minorAxis * 0.18 * 10) / 10)}px`,
        height: `${Math.max(2.5, Math.round(minorAxis * 0.18 * 10) / 10)}px`,
      }
    : undefined;
  const pipSizeClass = minorAxis ? '' : sizeInfo.pip;

  const inlineDimensions =
    width && height
      ? {
          width: `${width}px`,
          height: `${height}px`,
          borderRadius: `${Math.max(5, Math.round(Math.min(width, height) * 0.21))}px`,
        }
      : undefined;

  const dimensionClass = inlineDimensions
    ? ''
    : isHorizontal
    ? sizeInfo.horiz
    : sizeInfo.vert;

  return (
    <div
      role={onClick || onPointerDown ? 'button' : undefined}
      tabIndex={onClick || onPointerDown ? 0 : undefined}
      aria-label={`Domino ${topOrLeft} and ${bottomOrRight}`}
      onPointerDown={onPointerDown}
      onPointerMove={onPointerMove}
      onPointerUp={onPointerUp}
      onPointerCancel={onPointerCancel}
      onClick={onClick}
      onKeyDown={(e) => {
        if ((e.key === 'Enter' || e.key === ' ') && onClick) {
          e.preventDefault();
          onClick(e as unknown as React.MouseEvent<HTMLDivElement>);
        }
      }}
      style={inlineDimensions}
      className={[
        'relative select-none rounded-[10px] flex shrink-0 transition-[transform,opacity] duration-150',
        isHorizontal ? 'flex-row' : 'flex-col',
        dimensionClass,
        // Light Mode: Dark Charcoal Tile | Dark Mode: Light Ivory Tile
        'bg-gradient-to-b from-[#272C35] to-[#1A1E25] text-[#FAF8F4]',
        'border border-[#383F4B] shadow-[0_3px_0_#0D1015,0_6px_14px_rgba(15,23,42,0.22)]',
        'dark:from-[#FCF9F3] dark:to-[#ECE5D7] dark:text-[#171B22]',
        'dark:border-[#DDD3BE] dark:shadow-[0_3px_0_#BFB49D,0_6px_16px_rgba(0,0,0,0.45)]',
        // Interactive states
        isSelected
          ? '-translate-y-2.5 scale-[1.06] ring-2 ring-[#0F766E] dark:ring-[#2DD4BF] z-20'
          : '',
        isPlayable && !isSelected
          ? 'cursor-grab active:cursor-grabbing hover:-translate-y-1'
          : '',
        isDimmed ? 'opacity-40 saturate-75 cursor-not-allowed' : '',
        isNew
          ? 'ring-2 ring-amber-500/80 dark:ring-amber-400/80'
          : '',
        'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-teal-600',
        className,
      ]
        .filter(Boolean)
        .join(' ')}
    >
      {/* Subtle top specular highlight */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-x-1 top-0.5 h-[1px] rounded-full bg-white/15 dark:bg-white/70"
      />

      {/* First Half */}
      <div className="flex-1 min-w-0 min-h-0">
        <PipFace
          value={topOrLeft}
          horizontal={isHorizontal}
          pipSizeClass={pipSizeClass}
          pipStyle={pipStyle}
        />
      </div>

      {/* Brass Center Divider Bar + Spinner Pin */}
      <div
        className={[
          'relative flex items-center justify-center shrink-0',
          isHorizontal ? 'w-[2px] my-1.5' : 'h-[2px] mx-1.5',
          'bg-[#B89D68]/75 dark:bg-[#9B8253]/65 rounded-full',
        ].join(' ')}
      >
        {isSpinner && (
          <span
            title="Spinner (4-way)"
            className="absolute w-2 h-2 rounded-full bg-amber-400 dark:bg-amber-500 ring-2 ring-[#1A1E25] dark:ring-[#F6F2EA]"
          />
        )}
      </div>

      {/* Second Half */}
      <div className="flex-1 min-w-0 min-h-0">
        <PipFace
          value={bottomOrRight}
          horizontal={isHorizontal}
          pipSizeClass={pipSizeClass}
          pipStyle={pipStyle}
        />
      </div>
    </div>
  );
};

export const DominoTileBackView: React.FC<{
  width: number;
  height: number;
}> = ({ width, height }) => {
  return (
    <div
      aria-hidden="true"
      style={{
        width: `${width}px`,
        height: `${height}px`,
        borderRadius: `${Math.max(3, Math.round(width * 0.24))}px`,
      }}
      className="relative shrink-0 flex items-center justify-center bg-gradient-to-b from-[#2A303A] to-[#1A1E25] border border-[#3B4250] shadow-[0_1.5px_0_#0D1015] dark:from-[#F4EFE5] dark:to-[#E2DAC9] dark:border-[#CFC5B1] dark:shadow-[0_1.5px_0_#BFB49D]"
    >
      <span className="w-[38%] h-[38%] rounded-full border border-[#B89D68]/60 dark:border-[#9B8253]/50" />
    </div>
  );
};
