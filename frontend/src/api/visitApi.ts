import type {
  ReadbackResponse,
  SyncResult,
  VisitDraftResponse,
  VisitRecord,
} from '../types/visit';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

async function parseError(response: Response): Promise<never> {
  let detail = `Request failed (${response.status})`;
  try {
    const problem = (await response.json()) as { detail?: string };
    if (problem.detail) detail = problem.detail;
  } catch {
    // reason: error body may be empty or non-JSON
  }
  throw new Error(detail);
}

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

  if (!response.ok) return parseError(response);
  return (await response.json()) as VisitDraftResponse;
}

/** Ingest a photographed paper record (OCR) and get back the extracted DRAFT record. */
export async function ingestPhoto(
  image: Blob,
  filename: string,
  options: TranscribeOptions = {},
): Promise<VisitDraftResponse> {
  const form = new FormData();
  form.append('file', image, filename);
  if (options.workerId) form.append('workerId', options.workerId);
  if (options.deviceId) form.append('deviceId', options.deviceId);
  if (options.languageCode) form.append('languageCode', options.languageCode);

  const response = await fetch(`${API_BASE_URL}/api/v1/visits/ingest-photo`, {
    method: 'POST',
    body: form,
  });
  if (!response.ok) return parseError(response);
  return (await response.json()) as VisitDraftResponse;
}

/** Request a spoken confirmation (Bulbul TTS) of the record in the target language. */
export async function readbackVisit(
  visit: VisitRecord,
  targetLanguage = 'bn-IN',
): Promise<ReadbackResponse> {
  const url = `${API_BASE_URL}/api/v1/visits/readback?targetLanguage=${encodeURIComponent(
    targetLanguage,
  )}`;
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(visit),
  });
  if (!response.ok) return parseError(response);
  return (await response.json()) as ReadbackResponse;
}

/** Confirm a (possibly edited) record. `edited` drives DRAFT → CONFIRMED | EDITED. */
export async function confirmVisit(visit: VisitRecord, edited: boolean): Promise<VisitRecord> {
  const response = await fetch(`${API_BASE_URL}/api/v1/visits/confirm`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ visit, edited }),
  });
  if (!response.ok) return parseError(response);
  return (await response.json()) as VisitRecord;
}

/** Reconcile a batch of queued visits with the backend. Idempotent per visitId. */
export async function syncVisits(records: VisitRecord[]): Promise<SyncResult[]> {
  const response = await fetch(`${API_BASE_URL}/api/v1/visits/sync`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(records),
  });
  if (!response.ok) return parseError(response);
  return (await response.json()) as SyncResult[];
}

/** Decode base64 audio (as returned by the readback endpoint) into a playable object URL. */
export function base64ToAudioUrl(base64: string, contentType = 'audio/wav'): string {
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i += 1) bytes[i] = binary.charCodeAt(i);
  return URL.createObjectURL(new Blob([bytes], { type: contentType }));
}
