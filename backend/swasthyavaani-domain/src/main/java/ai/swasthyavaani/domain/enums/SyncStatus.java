package ai.swasthyavaani.domain.enums;

/** Sync state of a record between device and backend. Mirrors {@code data-model.md} §4. */
public enum SyncStatus {
  /** Held in the local queue, not yet synced. */
  PENDING,
  /** Durably stored in the backend registry. */
  SYNCED,
  /** Sync attempted and failed; eligible for retry. */
  FAILED,
  /** Replaced by a newer version (last-writer-wins by {@code updatedAt}). */
  SUPERSEDED
}
