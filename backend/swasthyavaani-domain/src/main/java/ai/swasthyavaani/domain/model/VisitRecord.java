package ai.swasthyavaani.domain.model;

import ai.swasthyavaani.domain.SchemaVersions;
import ai.swasthyavaani.domain.enums.ConfirmationStatus;
import ai.swasthyavaani.domain.enums.SyncStatus;
import ai.swasthyavaani.domain.enums.VisitType;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;

/**
 * The structured output of the extraction step and the unit that syncs to the backend — the
 * contract that ties Sarvam extraction, the client offline queue, and DynamoDB together.
 *
 * <p>The client-generated {@code visitId} (UUID v4) is the idempotency key: created on the device
 * the moment recording starts, it survives offline and makes sync exactly-once. See {@code
 * data-model.md}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VisitRecord(
    String visitId,
    int schemaVersion,
    String workerId,
    String deviceId,
    VisitType visitType,
    OffsetDateTime visitTimestamp,
    Location location,
    Beneficiary beneficiary,
    Observations observations,
    Actions actions,
    Provenance provenance,
    ConfirmationStatus confirmationStatus,
    SyncStatus syncStatus,
    boolean createdOffline,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    OffsetDateTime syncedAt) {

  public VisitRecord {
    if (schemaVersion == 0) {
      schemaVersion = SchemaVersions.CURRENT;
    }
  }
}
