package ai.swasthyavaani.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Where the visit happened. {@code villageName} and {@code state} are required; block/district are
 * optional quasi-identifiers. See {@code data-model.md} §3.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Location(String villageName, String block, String district, String state) {}
