package ai.swasthyavaani.api.visit;

import ai.swasthyavaani.domain.model.VisitRecord;

/**
 * Worker's confirmation of a (possibly edited) record.
 *
 * @param visit the record as the worker approved it (with any edits already applied client-side)
 * @param edited true if the worker changed any field before approving → status becomes {@code
 *     EDITED}; false → {@code CONFIRMED}
 */
public record ConfirmRequest(VisitRecord visit, boolean edited) {}
