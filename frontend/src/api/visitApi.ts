import type { VisitDraftResponse } from '../types/visit';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export interface TranscribeOptions {
  workerId?: string;
  deviceId?: string;
  languageCode?: string;
}

/**
 * Upload a visit recording to the backend and get back the extracted DRAFT record.
 * Phase 1 online path — offline queueing arrives in Phase 3.
 */
export async function transcribeVisit(
  audio: Blob,
  filename: string,
  options: TranscribeOptions = {},
): Promise<VisitDraftResponse> {
  const form = new FormData();
  form.append('file', audio, filename);
  if (options.workerId) form.append('workerId', options.workerId);
  if (options.deviceId) form.append('deviceId', options.deviceId);
  if (options.languageCode) form.append('languageCode', options.languageCode);

  const response = await fetch(`${API_BASE_URL}/api/v1/visits/transcribe`, {
    method: 'POST',
    body: form,
  });

  if (!response.ok) {
    let detail = `Request failed (${response.status})`;
    try {
      const problem = (await response.json()) as { detail?: string };
      if (problem.detail) detail = problem.detail;
    } catch {
      // reason: response body may be empty or non-JSON on some errors
    }
    throw new Error(detail);
  }

  return (await response.json()) as VisitDraftResponse;
}
