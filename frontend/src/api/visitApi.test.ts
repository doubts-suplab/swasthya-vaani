import { afterEach, describe, expect, it, vi } from 'vitest';
import { ingestPhoto, transcribeVisit } from './visitApi';
import type { VisitDraftResponse } from '../types/visit';

const sampleResponse: VisitDraftResponse = {
  transcript: 'test transcript',
  extractionValid: true,
  validationMessages: [],
  visit: {
    visitId: 'v-1',
    schemaVersion: 1,
    workerId: 'w',
    deviceId: 'd',
    visitType: 'GENERAL',
    visitTimestamp: '2026-08-28T09:14:00+05:30',
    confirmationStatus: 'DRAFT',
    syncStatus: 'PENDING',
    createdOffline: false,
    createdAt: '2026-08-28T09:14:00+05:30',
    updatedAt: '2026-08-28T09:14:00+05:30',
  },
};

describe('transcribeVisit', () => {
  afterEach(() => vi.restoreAllMocks());

  it('posts multipart audio and returns the parsed draft', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(sampleResponse), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);

    const blob = new Blob(['audio'], { type: 'audio/webm' });
    const result = await transcribeVisit(blob, 'visit.webm', { languageCode: 'bn-IN' });

    expect(result.visit.visitId).toBe('v-1');
    expect(fetchMock).toHaveBeenCalledOnce();
    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/v1/visits/transcribe');
    expect(init.method).toBe('POST');
    expect(init.body).toBeInstanceOf(FormData);
  });

  it('posts a photo to the OCR ingest endpoint', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(sampleResponse), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);

    const image = new Blob(['img'], { type: 'image/jpeg' });
    const result = await ingestPhoto(image, 'record.jpg', { languageCode: 'bn-IN' });

    expect(result.visit.visitId).toBe('v-1');
    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/v1/visits/ingest-photo');
    expect(init.body).toBeInstanceOf(FormData);
  });

  it('throws with the server problem detail on error', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ detail: 'The speech service is temporarily unavailable.' }), {
        status: 502,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);

    await expect(transcribeVisit(new Blob(['x']), 'v.webm')).rejects.toThrow(
      'temporarily unavailable',
    );
  });
});
