package ai.swasthyavaani.domain.enums;

/** Where a record sits in the confirm/edit loop. Mirrors {@code data-model.md} §4. */
public enum ConfirmationStatus {
  /** Extracted, not yet confirmed by the worker. */
  DRAFT,
  /** Worker approved the record as-is. */
  CONFIRMED,
  /** Worker changed at least one field before approving. */
  EDITED
}
