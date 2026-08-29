import { openDB, type DBSchema, type IDBPDatabase } from 'idb';
import type { VisitRecord } from '../types/visit';

const DB_NAME = 'swasthyavaani';
const DB_VERSION = 1;
const STORE = 'visits';

interface SwasthyaVaaniDB extends DBSchema {
  visits: {
    key: string; // visitId
    value: VisitRecord;
    indexes: { bySyncStatus: string };
  };
}

let dbPromise: Promise<IDBPDatabase<SwasthyaVaaniDB>> | null = null;

function db(): Promise<IDBPDatabase<SwasthyaVaaniDB>> {
  if (!dbPromise) {
    dbPromise = openDB<SwasthyaVaaniDB>(DB_NAME, DB_VERSION, {
      upgrade(database) {
        const store = database.createObjectStore(STORE, { keyPath: 'visitId' });
        store.createIndex('bySyncStatus', 'syncStatus');
      },
    });
  }
  return dbPromise;
}

/** Persist (or replace) a visit in the local queue, keyed by its visitId. */
export async function putVisit(record: VisitRecord): Promise<void> {
  await (await db()).put(STORE, record);
}

/** Every visit held locally, regardless of sync status. */
export async function getAllVisits(): Promise<VisitRecord[]> {
  return (await db()).getAll(STORE);
}

/** Visits still waiting to sync (or that failed). */
export async function getPendingVisits(): Promise<VisitRecord[]> {
  const all = await getAllVisits();
  return all.filter((v) => v.syncStatus === 'PENDING' || v.syncStatus === 'FAILED');
}

/** Count of not-yet-synced visits — drives the "N pending" indicator. */
export async function countPending(): Promise<number> {
  return (await getPendingVisits()).length;
}

/** Mark a queued visit as synced (called after the backend confirms the upsert). */
export async function markSynced(visitId: string): Promise<void> {
  const database = await db();
  const existing = await database.get(STORE, visitId);
  if (existing) {
    await database.put(STORE, { ...existing, syncStatus: 'SYNCED', syncedAt: new Date().toISOString() });
  }
}

/** Test/util: clear the local queue. */
export async function clearQueue(): Promise<void> {
  await (await db()).clear(STORE);
}
