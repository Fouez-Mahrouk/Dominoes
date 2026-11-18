export interface HandLayoutMetrics {
  tileWidth: number;
  tileHeight: number;
  gap: number;
  scale: number;
}

/**
 * Computes proportional tile dimensions so all tiles fit cleanly inside availableWidth
 * without overlap or horizontal scrolling, snapping back to base size as tiles are played.
 */
export function computeHandLayout(
  tileCount: number,
  availableWidth: number,
  baseWidth: number,
  baseHeight: number,
  baseGap: number
): HandLayoutMetrics {
  if (tileCount <= 0 || availableWidth <= 0) {
    return {
      tileWidth: baseWidth,
      tileHeight: baseHeight,
      gap: baseGap,
      scale: 1,
    };
  }

  const totalNormalWidth =
    tileCount * baseWidth + Math.max(0, tileCount - 1) * baseGap;

  if (totalNormalWidth <= availableWidth) {
    return {
      tileWidth: baseWidth,
      tileHeight: baseHeight,
      gap: baseGap,
      scale: 1,
    };
  }

  const scale = availableWidth / totalNormalWidth;
  return {
    tileWidth: Math.max(10, baseWidth * scale),
    tileHeight: Math.max(16, baseHeight * scale),
    gap: Math.max(1.5, baseGap * scale),
    scale,
  };
}
