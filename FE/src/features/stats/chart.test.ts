import { describe, expect, it } from 'vitest';
import { buildCategoryGradient, safePercentage } from './chart';

describe('dashboard chart calculations', () => {
  it('builds deterministic category segments and handles empty data', () => {
    expect(buildCategoryGradient([
      { category: 'BUG', count: 2 },
      { category: 'FEATURE', count: 1 },
      { category: 'INQUIRY', count: 1 },
    ])).toBe('conic-gradient(#de350b 0.00% 50.00%, #0052cc 50.00% 75.00%, #6554c0 75.00% 100.00%)');
    expect(buildCategoryGradient([])).toBe('#e8ebf0');
  });

  it('keeps bar widths within a valid percentage range', () => {
    expect(safePercentage(3, 6)).toBe(50);
    expect(safePercentage(0, 0)).toBe(0);
    expect(safePercentage(8, 4)).toBe(100);
  });
});
