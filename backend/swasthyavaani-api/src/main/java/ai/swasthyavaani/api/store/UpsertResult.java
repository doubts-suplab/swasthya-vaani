package ai.swasthyavaani.api.store;

import ai.swasthyavaani.domain.model.VisitRecord;

/**
 * Result of an idempotent upsert: the record now in the store and what happened.
 *
 * @param stored the authoritative stored record after the upsert
 * @param outcome whether it was created, updated, or an ignored duplicate
 */
public record UpsertResult(VisitRecord stored, UpsertOutcome outcome) {}
