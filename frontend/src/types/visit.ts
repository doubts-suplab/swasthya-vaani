// Mirrors the backend VisitRecord / VisitDraftResponse contract (docs/data-model.md).
// Kept intentionally in one place so the whole UI shares one typed shape.

export type VisitType =
  | 'ANC'
  | 'PNC'
  | 'IMMUNIZATION'
  | 'CHILD_GROWTH'
  | 'GENERAL'
  | 'OTHER';

export type BeneficiaryCategory =
  | 'PREGNANT_WOMAN'
  | 'LACTATING_MOTHER'
  | 'INFANT'
  | 'CHILD_UNDER_5'
  | 'ADULT'
  | 'OTHER';

export type ConfirmationStatus = 'DRAFT' | 'CONFIRMED' | 'EDITED';
export type SyncStatus = 'PENDING' | 'SYNCED' | 'FAILED' | 'SUPERSEDED';

export interface Beneficiary {
  beneficiaryRef?: string | null;
  name?: string | null;
  category?: BeneficiaryCategory | null;
  gender?: 'FEMALE' | 'MALE' | 'OTHER' | null;
  ageYears?: number | null;
}

export interface BloodPressure {
  systolic?: number | null;
  diastolic?: number | null;
}

export interface Observations {
  weightKg?: number | null;
  temperatureC?: number | null;
  bloodPressure?: BloodPressure | null;
  gestationWeeks?: number | null;
  reportedSymptoms?: string[] | null;
  notes?: string | null;
}

export interface Referral {
  referred?: boolean;
  facility?: string | null;
  urgency?: 'ROUTINE' | 'URGENT' | 'EMERGENCY' | null;
  reason?: string | null;
}

export interface Actions {
  medicinesHandedOver?: string[] | null;
  referral?: Referral | null;
  nextVisitDate?: string | null;
}

export interface Provenance {
  sourceLanguage?: string | null;
  sttModel?: string | null;
  extractionModel?: string | null;
  extractionConfidence?: number | null;
  warnings?: string[] | null;
}

export interface VisitRecord {
  visitId: string;
  schemaVersion: number;
  workerId: string;
  deviceId: string;
  visitType: VisitType;
  visitTimestamp: string;
  location?: { villageName?: string | null; state?: string | null } | null;
  beneficiary?: Beneficiary | null;
  observations?: Observations | null;
  actions?: Actions | null;
  provenance?: Provenance | null;
  confirmationStatus: ConfirmationStatus;
  syncStatus: SyncStatus;
  createdOffline: boolean;
  createdAt: string;
  updatedAt: string;
  syncedAt?: string | null;
}

export interface VisitDraftResponse {
  visit: VisitRecord;
  transcript: string;
  extractionValid: boolean;
  validationMessages: string[];
}

export interface ReadbackResponse {
  spokenText: string;
  officialText: string;
  targetLanguage: string;
  audioBase64: string;
  audioContentType: string;
  cached: boolean;
}
