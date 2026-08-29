import { beforeEach, describe, expect, it } from 'vitest';
import type { VisitRecord } from '../types/visit';
import { clearQueue, countPending, getPendingVisits, markSynced, putVisit } from './visitQueue';

function record(visitId: string, syncStatus: VisitRecord['syncStatus'] = 'PENDING'): VisitRecord {
  const now = '2026-08-28T09:14:00+05:30';
  return {
    visitId,
    schemaVersion: 1,
    workerId: 'ASHA-WB-1',
    deviceId: 'dev',
    visitType: 'ANC',
    visitTimestamp: now,
    confirmationStatus: 'CONFIRMED',
    syncStatus,
    createdOffline: true,
    createdAt: now,
    updatedAt: now,
  };
}

describe('visitQueue (IndexedDB)', () => {
  beforeEach(async () => {
    await clearQueue();
  });

  it('enqueues and reports pending visits', async () => {
    await putVisit(record('v-1'));
    await putVisit(record('v-2'));

    expect(await countPending()).toBe(2);
    expect((await getPendingVisits()).map((v) => v.visitId).sort()).toEqual(['v-1', 'v-2']);
  });

  it('marking synced removes it from pending', async () => {
    await putVisit(record('v-1'));
    await markSynced('v-1');

    expect(await countPending()).toBe(0);
  });

  it('re-putting the same visitId does not duplicate', async () => {
    await putVisit(record('v-1'));
    await putVisit(record('v-1'));

    expect(await countPending()).toBe(1);
  });
});
