import type { VisitRecord } from '../types/visit';

/**
 * Apply the confirm/edit transition on the device (mirrors the backend `VisitConfirmationService`).
 * Offline-first: the worker can confirm with no connectivity; the record is queued and the backend
 * reconciles it idempotently later. Marks the record PENDING so the sync engine picks it up.
 */
export function applyConfirmation(record: VisitRecord, edited: boolean): VisitRecord {
  return {
    ...record,
    confirmationStatus: edited ? 'EDITED' : 'CONFIRMED',
    syncStatus: 'PENDING',
    updatedAt: new Date().toISOString(),
  };
}
