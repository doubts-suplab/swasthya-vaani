import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import type { VisitDraftResponse } from '../types/visit';
import { VisitRecordView } from './VisitRecordView';

const draft: VisitDraftResponse = {
  transcript: 'baby-r weight thik aache, kintu fever holo kal theke',
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
    observations: { weightKg: 52.5, reportedSymptoms: ['fever since yesterday'] },
    actions: { medicinesHandedOver: ['IFA tablets'] },
    provenance: { sttModel: 'saaras:v3', extractionModel: 'sarvam-m', warnings: [] },
    confirmationStatus: 'DRAFT',
    syncStatus: 'PENDING',
    createdOffline: false,
    createdAt: '2026-08-28T09:14:00+05:30',
    updatedAt: '2026-08-28T09:14:00+05:30',
  },
};

describe('VisitRecordView', () => {
  it('renders extracted fields', () => {
    render(<VisitRecordView draft={draft} />);
    expect(screen.getByText('Rekha Das')).toBeInTheDocument();
    expect(screen.getByText('52.5')).toBeInTheDocument();
    expect(screen.getByText('IFA tablets')).toBeInTheDocument();
    expect(screen.getByText('DRAFT')).toBeInTheDocument();
  });

  it('shows a review alert when extraction is invalid', () => {
    render(
      <VisitRecordView
        draft={{ ...draft, extractionValid: false, validationMessages: ['ageYears too large'] }}
      />,
    );
    expect(screen.getByRole('alert')).toHaveTextContent('Please review');
    expect(screen.getByText('ageYears too large')).toBeInTheDocument();
  });

  it('marks missing fields as not recorded', () => {
    render(
      <VisitRecordView
        draft={{ ...draft, visit: { ...draft.visit, observations: { notes: null } } }}
      />,
    );
    expect(screen.getAllByText('not recorded').length).toBeGreaterThan(0);
  });
});
