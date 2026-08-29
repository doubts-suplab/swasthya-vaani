import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { VisitDraftResponse } from '../types/visit';
import { VisitReview } from './VisitReview';
import { readbackVisit } from '../api/visitApi';

vi.mock('../api/visitApi', () => ({
  readbackVisit: vi.fn(),
  base64ToAudioUrl: vi.fn(() => 'blob:mock-audio'),
}));

const draft: VisitDraftResponse = {
  transcript: 'baby-r weight thik aache',
  extractionValid: true,
  validationMessages: [],
  visit: {
    visitId: 'v-1',
    schemaVersion: 1,
    workerId: 'ASHA-WB-1',
    deviceId: 'dev',
    visitType: 'ANC',
    visitTimestamp: '2026-08-28T09:14:00+05:30',
    beneficiary: { name: 'Rekha Das', category: 'PREGNANT_WOMAN', ageYears: 24 },
    observations: { weightKg: 52.5, reportedSymptoms: ['fever'] },
    actions: { medicinesHandedOver: ['IFA tablets'] },
    provenance: { sttModel: 'saaras:v3', extractionModel: 'sarvam-m' },
    confirmationStatus: 'DRAFT',
    syncStatus: 'PENDING',
    createdOffline: false,
    createdAt: '2026-08-28T09:14:00+05:30',
    updatedAt: '2026-08-28T09:14:00+05:30',
  },
};

describe('VisitReview', () => {
  beforeEach(() => vi.clearAllMocks());

  it('confirms an unedited record as CONFIRMED (on-device)', async () => {
    const onConfirmed = vi.fn();
    render(<VisitReview draft={draft} onConfirmed={onConfirmed} />);

    fireEvent.click(screen.getByRole('button', { name: 'Confirm' }));

    await waitFor(() => expect(onConfirmed).toHaveBeenCalledOnce());
    expect(onConfirmed.mock.calls[0][0].confirmationStatus).toBe('CONFIRMED');
  });

  it('marks the record EDITED when a field changes', async () => {
    const onConfirmed = vi.fn();
    render(<VisitReview draft={draft} onConfirmed={onConfirmed} />);

    fireEvent.change(screen.getByDisplayValue('Rekha Das'), { target: { value: 'Rekha Devi' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save edits & confirm' }));

    await waitFor(() => expect(onConfirmed).toHaveBeenCalledOnce());
    const confirmedArg = onConfirmed.mock.calls[0][0];
    expect(confirmedArg.confirmationStatus).toBe('EDITED');
    expect(confirmedArg.beneficiary?.name).toBe('Rekha Devi');
  });

  it('plays a readback and shows the spoken text', async () => {
    vi.mocked(readbackVisit).mockResolvedValue({
      spokenText: 'বাংলা টেক্সট',
      officialText: 'বাংলা টেক্সট',
      targetLanguage: 'bn-IN',
      audioBase64: 'd2F2',
      audioContentType: 'audio/wav',
      cached: false,
    });
    render(<VisitReview draft={draft} onConfirmed={vi.fn()} />);

    fireEvent.click(screen.getByRole('button', { name: '🔊 Play readback' }));

    expect(await screen.findByText('বাংলা টেক্সট')).toBeInTheDocument();
    expect(readbackVisit).toHaveBeenCalledWith(draft.visit, 'bn-IN');
  });
});
