package ai.swasthyavaani.api.store;

/** Outcome of an idempotent upsert keyed on {@code visitId} ({@code data-model.md} §7). */
public enum UpsertOutcome {
  /** First time this {@code visitId} was seen — stored. */
  CREATED,
  /** A strictly newer version (by {@code updatedAt}) replaced the stored one (last-writer-wins). */
  UPDATED,
  /** Same or older version re-sent — no change; the earlier store stands (idempotent success). */
  DUPLICATE_IGNORED
}
