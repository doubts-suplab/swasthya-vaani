/**
 * Data-sovereignty guard (CLAUDE.md §7.1 / roadmap T7-F04). Every SwasthyaVaani stack MUST deploy
 * in India — `ap-south-1` (Mumbai). This is enforced at synth time so a misconfigured region fails
 * the build/CI rather than shipping PII out of the country.
 */
export const INDIA_REGION = 'ap-south-1';

export function assertInIndia(region: string | undefined): string {
  if (region !== INDIA_REGION) {
    throw new Error(
      `Data residency violation: region must be '${INDIA_REGION}' (India), got '${region ?? 'undefined'}'. ` +
        'All SwasthyaVaani processing and storage stays in India (CLAUDE.md §7.1).',
    );
  }
  return region;
}
