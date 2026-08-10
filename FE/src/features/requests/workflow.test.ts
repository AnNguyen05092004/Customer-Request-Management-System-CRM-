import { describe, expect, it } from 'vitest';
import { allowedNextStatuses } from './workflow';

describe('request workflow transitions', () => {
  it('exposes only transitions accepted by the backend state machine', () => {
    expect(allowedNextStatuses('PENDING')).toEqual(['IN_PROGRESS']);
    expect(allowedNextStatuses('IN_PROGRESS')).toEqual(['DONE']);
    expect(allowedNextStatuses('DONE')).toEqual([]);
  });
});
