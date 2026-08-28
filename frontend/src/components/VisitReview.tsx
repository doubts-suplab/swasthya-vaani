import { useMemo, useState } from 'react';
import { base64ToAudioUrl, confirmVisit, readbackVisit } from '../api/visitApi';
import type {
  Actions,
  Beneficiary,
  BeneficiaryCategory,
  Observations,
  VisitDraftResponse,
  VisitRecord,
} from '../types/visit';

const CATEGORIES: BeneficiaryCategory[] = [
  'PREGNANT_WOMAN',
  'LACTATING_MOTHER',
  'INFANT',
  'CHILD_UNDER_5',
  'ADULT',
  'OTHER',
];

/**
 * Phase 2 review loop: edit any field, hear the record read back (Bulbul TTS), then confirm.
 * Confirming sends DRAFT → CONFIRMED (unchanged) or EDITED (changed).
 */
export function VisitReview({
  draft,
  onConfirmed,
}: {
  draft: VisitDraftResponse;
  onConfirmed: (record: VisitRecord) => void;
}) {
  const original = useMemo(() => JSON.stringify(draft.visit), [draft.visit]);
  const [visit, setVisit] = useState<VisitRecord>(draft.visit);
  const [audioUrl, setAudioUrl] = useState<string | null>(null);
  const [spoken, setSpoken] = useState<string | null>(null);
  const [busy, setBusy] = useState<null | 'readback' | 'confirm'>(null);
  const [error, setError] = useState<string | null>(null);

  const edited = JSON.stringify(visit) !== original;

  function patchBeneficiary(patch: Partial<Beneficiary>) {
    setVisit((v) => ({ ...v, beneficiary: { ...v.beneficiary, ...patch } }));
  }
  function patchObservations(patch: Partial<Observations>) {
    setVisit((v) => ({ ...v, observations: { ...v.observations, ...patch } }));
  }
  function patchActions(patch: Partial<Actions>) {
    setVisit((v) => ({ ...v, actions: { ...v.actions, ...patch } }));
  }

  async function playReadback() {
    setBusy('readback');
    setError(null);
    try {
      const rb = await readbackVisit(visit, 'bn-IN');
      setSpoken(rb.spokenText);
      setAudioUrl(base64ToAudioUrl(rb.audioBase64, rb.audioContentType));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Readback failed.');
    } finally {
      setBusy(null);
    }
  }

  async function confirm() {
    setBusy('confirm');
    setError(null);
    try {
      onConfirmed(await confirmVisit(visit, edited));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Confirmation failed.');
    } finally {
      setBusy(null);
    }
  }

  const b = visit.beneficiary ?? {};
  const o = visit.observations ?? {};
  const a = visit.actions ?? {};

  return (
    <div className="record">
      <div className="record__statusrow">
        <span className="badge badge--offline">{visit.confirmationStatus}</span>
        <span className="muted">{visit.visitType}</span>
        {edited && <span className="badge badge--online">edited</span>}
      </div>

      {!draft.extractionValid && (
        <div className="alert" role="alert">
          <strong>Please review.</strong> Some details couldn’t be auto-filled — check them
          against the transcript.
        </div>
      )}
      {error && (
        <div className="alert" role="alert">
          {error}
        </div>
      )}

      <fieldset className="record__section">
        <legend className="record__sectiontitle">Beneficiary</legend>
        <TextField label="Name" value={b.name} onChange={(v) => patchBeneficiary({ name: v })} />
        <SelectField
          label="Category"
          value={b.category ?? ''}
          options={CATEGORIES}
          onChange={(v) => patchBeneficiary({ category: (v || null) as BeneficiaryCategory | null })}
        />
        <NumberField
          label="Age (years)"
          value={b.ageYears}
          onChange={(v) => patchBeneficiary({ ageYears: v })}
        />
      </fieldset>

      <fieldset className="record__section">
        <legend className="record__sectiontitle">Observations</legend>
        <NumberField
          label="Weight (kg)"
          value={o.weightKg}
          onChange={(v) => patchObservations({ weightKg: v })}
        />
        <NumberField
          label="Temperature (°C)"
          value={o.temperatureC}
          onChange={(v) => patchObservations({ temperatureC: v })}
        />
        <NumberField
          label="Gestation (weeks)"
          value={o.gestationWeeks}
          onChange={(v) => patchObservations({ gestationWeeks: v })}
        />
        <TextField
          label="Symptoms (comma-separated)"
          value={o.reportedSymptoms?.join(', ')}
          onChange={(v) => patchObservations({ reportedSymptoms: splitList(v) })}
        />
        <TextField label="Notes" value={o.notes} onChange={(v) => patchObservations({ notes: v })} />
      </fieldset>

      <fieldset className="record__section">
        <legend className="record__sectiontitle">Actions</legend>
        <TextField
          label="Medicines (comma-separated)"
          value={a.medicinesHandedOver?.join(', ')}
          onChange={(v) => patchActions({ medicinesHandedOver: splitList(v) })}
        />
        <TextField
          label="Next visit (YYYY-MM-DD)"
          value={a.nextVisitDate}
          onChange={(v) => patchActions({ nextVisitDate: v || null })}
        />
      </fieldset>

      <details className="transcript" open={!draft.extractionValid}>
        <summary>Transcript</summary>
        <p lang={visit.provenance?.sourceLanguage ?? undefined}>{draft.transcript}</p>
      </details>

      <div className="capture__actions">
        <button className="cta cta--enabled" type="button" onClick={playReadback} disabled={busy !== null}>
          {busy === 'readback' ? 'Preparing…' : '🔊 Play readback'}
        </button>
        <button className="cta cta--enabled" type="button" onClick={confirm} disabled={busy !== null}>
          {busy === 'confirm' ? 'Saving…' : edited ? 'Save edits & confirm' : 'Confirm'}
        </button>
      </div>

      {audioUrl && (
        <div className="capture__review">
          {/* eslint-disable-next-line jsx-a11y/media-has-caption -- reason: spoken readback, text shown below */}
          <audio controls autoPlay src={audioUrl} />
          {spoken && <p className="muted" lang={visit.provenance?.sourceLanguage ?? undefined}>{spoken}</p>}
        </div>
      )}
    </div>
  );
}

function splitList(value: string): string[] {
  return value
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean);
}

function TextField({
  label,
  value,
  onChange,
}: {
  label: string;
  value?: string | null;
  onChange: (v: string) => void;
}) {
  return (
    <label className="editfield">
      <span>{label}</span>
      <input type="text" value={value ?? ''} onChange={(e) => onChange(e.target.value)} />
    </label>
  );
}

function NumberField({
  label,
  value,
  onChange,
}: {
  label: string;
  value?: number | null;
  onChange: (v: number | null) => void;
}) {
  return (
    <label className="editfield">
      <span>{label}</span>
      <input
        type="number"
        inputMode="decimal"
        value={value ?? ''}
        onChange={(e) => onChange(e.target.value === '' ? null : Number(e.target.value))}
      />
    </label>
  );
}

function SelectField({
  label,
  value,
  options,
  onChange,
}: {
  label: string;
  value: string;
  options: string[];
  onChange: (v: string) => void;
}) {
  return (
    <label className="editfield">
      <span>{label}</span>
      <select value={value} onChange={(e) => onChange(e.target.value)}>
        <option value="">—</option>
        {options.map((opt) => (
          <option key={opt} value={opt}>
            {opt}
          </option>
        ))}
      </select>
    </label>
  );
}
