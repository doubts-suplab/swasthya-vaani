package ai.swasthyavaani.api.store;

import ai.swasthyavaani.domain.model.VisitRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Maps a {@link VisitRecord} to/from a DynamoDB single-table item ({@code data-model.md} §6). Keys
 * are derived per the access patterns; the full record is stored as a JSON {@code body} attribute
 * so the schema can evolve without an item-shape migration. {@code updatedAt} is a top-level
 * attribute to drive the last-writer-wins conditional write. Pure and unit-tested — no AWS client
 * needed.
 */
public final class VisitItem {

  public static final String PK = "PK";
  public static final String SK = "SK";
  public static final String GSI2_PK = "GSI2PK";
  public static final String GSI2_SK = "GSI2SK";
  public static final String UPDATED_AT = "updatedAt";
  public static final String BODY = "body";

  private final ObjectMapper objectMapper;

  public VisitItem(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public Map<String, AttributeValue> toItem(VisitRecord r) {
    var item = new HashMap<String, AttributeValue>();
    item.put(PK, s("WORKER#" + r.workerId()));
    item.put(SK, s("VISIT#" + r.visitTimestamp() + "#" + r.visitId()));
    item.put(GSI2_PK, s("VISIT#" + r.visitId()));
    item.put(GSI2_SK, s("VISIT#" + r.visitId()));
    if (r.beneficiary() != null && r.beneficiary().beneficiaryRef() != null) {
      item.put("GSI1PK", s("BEN#" + r.beneficiary().beneficiaryRef()));
      item.put("GSI1SK", s("VISIT#" + r.visitTimestamp()));
    }
    if (r.syncStatus() != null) {
      item.put("GSI3PK", s("SYNC#" + r.syncStatus()));
      item.put("GSI3SK", s("VISIT#" + r.visitTimestamp()));
    }
    if (r.updatedAt() != null) {
      item.put(UPDATED_AT, s(r.updatedAt().toString()));
    }
    item.put(BODY, s(writeBody(r)));
    return item;
  }

  public VisitRecord fromItem(Map<String, AttributeValue> item) {
    var body = item.get(BODY);
    if (body == null || body.s() == null) {
      throw new IllegalStateException("DynamoDB item missing 'body' attribute");
    }
    try {
      return objectMapper.readValue(body.s(), VisitRecord.class);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to deserialize stored visit record", e);
    }
  }

  private String writeBody(VisitRecord r) {
    try {
      return objectMapper.writeValueAsString(r);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to serialize visit record", e);
    }
  }

  private static AttributeValue s(String value) {
    return AttributeValue.fromS(value);
  }
}
