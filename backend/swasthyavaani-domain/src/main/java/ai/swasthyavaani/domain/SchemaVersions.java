package ai.swasthyavaani.domain;

/**
 * Visit-record schema versioning ({@code data-model.md} §9). Bump {@link #CURRENT} on any breaking
 * change to the record shape; the backend accepts the current version and one prior.
 */
public final class SchemaVersions {

  /** Current visit-record schema version. */
  public static final int CURRENT = 1;

  /** Oldest schema version the backend still accepts. */
  public static final int MIN_SUPPORTED = 1;

  private SchemaVersions() {}
}
