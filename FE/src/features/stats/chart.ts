import type { Category } from '../../types/api';

export const categoryColors: Record<Category, string> = {
  BUG: '#de350b',
  FEATURE: '#0052cc',
  INQUIRY: '#6554c0',
};

export function buildCategoryGradient(values: Array<{ category: Category; count: number }>): string {
  const total = values.reduce((sum, item) => sum + item.count, 0);
  if (total <= 0) return '#e8ebf0';

  let offset = 0;
  const stops = values
    .filter((item) => item.count > 0)
    .map((item) => {
      const start = offset;
      offset += (item.count / total) * 100;
      return `${categoryColors[item.category]} ${start.toFixed(2)}% ${offset.toFixed(2)}%`;
    });
  return `conic-gradient(${stops.join(', ')})`;
}

export function safePercentage(value: number, maximum: number): number {
  if (maximum <= 0 || value <= 0) return 0;
  return Math.min(100, (value / maximum) * 100);
}
