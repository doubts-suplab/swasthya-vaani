import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import type { VisitDraftResponse } from '../types/visit';
import { RecordVisit } from './RecordVisit';
import { ingestPhoto } from '../api/visitApi';

// Voice capture unavailable (as in a plain test/browser without a mic) -> photo path is offered.
vi.mock('../hooks/useVoiceCapture', () => ({
  useVoiceCapture: () => ({
    status: 'unsupported',
    recording: null,
    error: null,
    start: vi.fn(),
    stop: vi.fn(),
    reset: vi.fn(),
    supported: false,
    native: false,
  }),
}));

vi.mock('../api/visitApi', () => ({
  transcribeVisit: vi.fn(),
  ingestPhoto: vi.fn(),
}));

const draft: VisitDraftResponse = {
  transcript: 'Rekha Das, weight 52.5 kg',
  extractionValid: true,
  validationMessages: [],
  visit: {
    visitId: 'ocr-1',
    schemaVersion: 1,
    workerId: 'ASHA-WB-1',
    deviceId: 'dev',
    visitType: 'ANC',
    visitTimestamp: '2026-08-28T09:14:00+05:30',
    beneficiary: { name: 'Rekha Das', ageYears: 24 },
    observations: { weightKg: 52.5 },
    actions: {},
    provenance: { extractionModel: 'sarvam-m', warnings: ['Ingested via OCR'] },
    confirmationStatus: 'DRAFT',
    syncStatus: 'PENDING',
    createdOffline: false,
    createdAt: '2026-08-28T09:14:00+05:30',
    updatedAt: '2026-08-28T09:14:00+05:30',
  },
};

describe('RecordVisit — photo ingest', () => {
  it('offers a photo option and ingests it into the review flow', async () => {
    vi.mocked(ingestPhoto).mockResolvedValue(draft);
    const { container } = render(<RecordVisit />);

    expect(screen.getByRole('button', { name: /Photograph a paper record/ })).toBeInTheDocument();

    const input = container.querySelector('input[type="file"]') as HTMLInputElement;
    const file = new File(['img'], 'record.jpg', { type: 'image/jpeg' });
    fireEvent.change(input, { target: { files: [file] } });

    // After OCR ingest, the extracted record is shown for review.
    expect(await screen.findByDisplayValue('Rekha Das')).toBeInTheDocument();
    expect(ingestPhoto).toHaveBeenCalledOnce();
  });
});
