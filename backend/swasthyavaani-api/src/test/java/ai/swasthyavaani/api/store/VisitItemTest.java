package ai.swasthyavaani.api.store;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ai.swasthyavaani.api.visit.VisitTestData;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class VisitItemTest {

  // Mirrors the app's ObjectMapper config: jsr310, ISO dates, IST offset preserved (not normalised
  // to UTC) — matching spring.jackson in application.yml.
  private final VisitItem mapper =
      new VisitItem(
          JsonMapper.builder()
              .findAndAddModules()
              .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
              .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
              .build());

  @Test
  void derivesSingleTableKeys() {
    var record = VisitTestData.draftWith("v-1", OffsetDateTime.parse("2026-08-28T09:14:00+05:30"));
    var item = mapper.toItem(record);

    assertEquals("WORKER#ASHA-WB-1", item.get(VisitItem.PK).s());
    assertEquals("VISIT#v-1", item.get(VisitItem.GSI2_PK).s());
    assertEquals("2026-08-28T09:14+05:30", item.get(VisitItem.UPDATED_AT).s());
    org.junit.jupiter.api.Assertions.assertTrue(item.get(VisitItem.SK).s().endsWith("#v-1"));
  }

  @Test
  void roundTripsTheRecordThroughTheBody() {
    var record = VisitTestData.draftWith("v-2", OffsetDateTime.parse("2026-08-28T10:00:00+05:30"));
    var restored = mapper.fromItem(mapper.toItem(record));
    assertEquals(record, restored);
  }
}
