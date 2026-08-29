import { syncVisits } from '../api/visitApi';
import { getPendingVisits, markSynced, putVisit } from './visitQueue';

const CHANGE_EVENT = 'sv:queue-changed';

export interface DrainResult {
  attempted: number;
  synced: number;
  failed: number;
}

/**
 * Drain the offline queue: send every PENDING/FAILED visit to the backend and mark the ones it
 * accepted as SYNCED. Idempotent end-to-end (backend upserts on visitId), so a partial/failed drain
 * can be retried safely — the core of "no data lost, no duplicates" (CLAUDE.md §7.3).
 */
export async function drainQueue(): Promise<DrainResult> {
  const pending = await getPendingVisits();
  if (pending.length === 0) return { attempted: 0, synced: 0, failed: 0 };

  try {
    const results = await syncVisits(pending);
    const syncedIds = new Set(results.map((r) => r.visitId));
    await Promise.all(pending.filter((v) => syncedIds.has(v.visitId)).map((v) => markSynced(v.visitId)));
    return { attempted: pending.length, synced: syncedIds.size, failed: pending.length - syncedIds.size };
  } catch {
    // Offline or backend unreachable: leave the queue intact for the next attempt.
    return { attempted: pending.length, synced: 0, failed: pending.length };
  }
}

/** Enqueue a confirmed visit and notify listeners (which triggers a drain when online). */
export async function enqueueVisit(record: Parameters<typeof putVisit>[0]): Promise<void> {
  await putVisit(record);
  notifyQueueChanged();
}

export function notifyQueueChanged(): void {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new Event(CHANGE_EVENT));
  }
}

export function onQueueChanged(listener: () => void): () => void {
  window.addEventListener(CHANGE_EVENT, listener);
  return () => window.removeEventListener(CHANGE_EVENT, listener);
}
