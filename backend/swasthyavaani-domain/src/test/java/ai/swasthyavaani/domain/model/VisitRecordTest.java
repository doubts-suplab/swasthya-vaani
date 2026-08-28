package ai.swasthyavaani.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ai.swasthyavaani.domain.SchemaVersions;
import ai.swasthyavaani.domain.enums.ConfirmationStatus;
import ai.swasthyavaani.domain.enums.SyncStatus;
import ai.swasthyavaani.domain.enums.VisitType;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class VisitRecordTest {

  @Test
  void defaultsSchemaVersionWhenUnset() {
    var now = OffsetDateTime.now();
    var record =
        new VisitRecord(
            "8f2c1a44-6b0e-4c9a-9f1d-2e7a5b3c1d90",
            0, // unset -> should default to CURRENT
            "ASHA-WB-24PGS-00417",
            "dev-3a9f",
            VisitType.ANC,
            now,
            null,
            null,
            null,
            null,
            null,
            ConfirmationStatus.DRAFT,
            SyncStatus.PENDING,
            true,
            now,
            now,
            null);

    assertEquals(SchemaVersions.CURRENT, record.schemaVersion());
  }

  @Test
  void keepsExplicitSchemaVersion() {
    var now = OffsetDateTime.now();
    var record =
        new VisitRecord(
            "id",
            1,
            "w",
            "d",
            VisitType.GENERAL,
            now,
            null,
            null,
            null,
            null,
            null,
            ConfirmationStatus.DRAFT,
            SyncStatus.PENDING,
            false,
            now,
            now,
            null);

    assertEquals(1, record.schemaVersion());
  }
}
