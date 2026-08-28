package ai.swasthyavaani.domain.enums;

/**
 * Worker-flagged urgency of a referral. Not a clinical judgement — the worker's words only ({@code
 * CLAUDE.md} §7.4). Mirrors {@code data-model.md} §4.
 */
public enum ReferralUrgency {
  ROUTINE,
  URGENT,
  EMERGENCY
}
