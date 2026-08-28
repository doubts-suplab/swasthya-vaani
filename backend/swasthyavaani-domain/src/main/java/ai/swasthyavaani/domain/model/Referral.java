package ai.swasthyavaani.domain.model;

import ai.swasthyavaani.domain.enums.ReferralUrgency;
import com.fasterxml.jackson.annotation.JsonInclude;

/** A referral the worker made. {@code urgency} is worker-flagged, not clinical. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Referral(boolean referred, String facility, ReferralUrgency urgency, String reason) {}
