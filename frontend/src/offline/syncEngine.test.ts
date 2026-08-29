import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { SyncResult, VisitRecord } from '../types/visit';
import { clearQueue, countPending, putVisit } from './visitQueue';
import { drainQueue } from './syncEngine';
import { syncVisits } from '../api/visitApi';

vi.mock('../api/visitApi', () => ({ syncVisits: vi.fn() }));

function record(visitId: string): VisitRecord {
  const now = '2026-08-28T09:14:00+05:30';
  return {
    visitId,
    schemaVersion: 1,
    workerId: 'ASHA-WB-1',
    deviceId: 'dev',
    visitType: 'ANC',
    visitTimestamp: now,
    confirmationStatus: 'CONFIRMED',
    syncStatus: 'PENDING',
    createdOffline: true,
    createdAt: now,
    updatedAt: now,
  };
}

describe('drainQueue', () => {
  beforeEach(async () => {
    await clearQueue();
    vi.clearAllMocks();
  });
  afterEach(() => vi.restoreAllMocks());

  it('syncs pending visits and marks them synced', async () => {
    await putVisit(record('v-1'));
    await putVisit(record('v-2'));
    const results: SyncResult[] = [
      { visitId: 'v-1', outcome: 'CREATED', syncStatus: 'SYNCED' },
      { visitId: 'v-2', outcome: 'CREATED', syncStatus: 'SYNCED' },
    ];
    vi.mocked(syncVisits).mockResolvedValue(results);

    const result = await drainQueue();

    expect(result).toEqual({ attempted: 2, synced: 2, failed: 0 });
    expect(await countPending()).toBe(0);
  });

  it('keeps the queue intact when the backend is unreachable', async () => {
    await putVisit(record('v-1'));
    vi.mocked(syncVisits).mockRejectedValue(new Error('offline'));

    const result = await drainQueue();

    expect(result.failed).toBe(1);
    expect(await countPending()).toBe(1); // not lost — retried next time
  });

  it('is a no-op when nothing is pending', async () => {
    const result = await drainQueue();
    expect(result).toEqual({ attempted: 0, synced: 0, failed: 0 });
    expect(syncVisits).not.toHaveBeenCalled();
  });
});
