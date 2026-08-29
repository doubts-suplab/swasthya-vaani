package ai.swasthyavaani.api.sync;

import ai.swasthyavaani.api.store.UpsertOutcome;
import ai.swasthyavaani.domain.enums.SyncStatus;

/**
 * Per-record outcome of a sync, returned to the client so it can mark its local queue entry.
 *
 * @param visitId the record's idempotency key
 * @param outcome created / updated / duplicate-ignored
 * @param syncStatus the record's sync status after the operation (always {@code SYNCED} on success)
 */
public record SyncResult(String visitId, UpsertOutcome outcome, SyncStatus syncStatus) {}
