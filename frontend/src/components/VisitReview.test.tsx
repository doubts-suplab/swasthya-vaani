import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { VisitDraftResponse } from '../types/visit';
import { VisitReview } from './VisitReview';
import { confirmVisit, readbackVisit } from '../api/visitApi';

vi.mock('../api/visitApi', () => ({
  readbackVisit: vi.fn(),
  confirmVisit: vi.fn(),
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

  it('confirms an unedited record with edited=false', async () => {
    vi.mocked(confirmVisit).mockResolvedValue({ ...draft.visit, confirmationStatus: 'CONFIRMED' });
    const onConfirmed = vi.fn();
    render(<VisitReview draft={draft} onConfirmed={onConfirmed} />);

    fireEvent.click(screen.getByRole('button', { name: 'Confirm' }));

    await waitFor(() => expect(onConfirmed).toHaveBeenCalledOnce());
    expect(confirmVisit).toHaveBeenCalledWith(draft.visit, false);
  });

  it('marks the record edited when a field changes', async () => {
    vi.mocked(confirmVisit).mockResolvedValue({ ...draft.visit, confirmationStatus: 'EDITED' });
    render(<VisitReview draft={draft} onConfirmed={vi.fn()} />);

    const nameInput = screen.getByDisplayValue('Rekha Das');
    fireEvent.change(nameInput, { target: { value: 'Rekha Devi' } });

    fireEvent.click(screen.getByRole('button', { name: 'Save edits & confirm' }));

    await waitFor(() => expect(confirmVisit).toHaveBeenCalledOnce());
    const [visitArg, editedArg] = vi.mocked(confirmVisit).mock.calls[0];
    expect(visitArg.beneficiary?.name).toBe('Rekha Devi');
    expect(editedArg).toBe(true);
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
