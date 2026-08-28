package ai.swasthyavaani.sarvam.model;

/**
 * Saaras v3 output modes ({@code sarvam-integration.md} §3). {@code CODEMIX} matches how ASHA
 * workers speak (mixed script); {@code TRANSCRIBE} keeps the original language.
 */
public enum SttMode {
  TRANSCRIBE("transcribe"),
  TRANSLATE("translate"),
  VERBATIM("verbatim"),
  TRANSLIT("translit"),
  CODEMIX("codemix");

  private final String wire;

  SttMode(String wire) {
    this.wire = wire;
  }

  /** The value the Sarvam API expects on the wire. */
  public String wire() {
    return wire;
  }
}
