import type { VisitDraftResponse } from '../types/visit';

/** Renders an extracted DRAFT visit record for the worker to review. Read-only in Phase 1; the
 * edit/confirm loop arrives in Phase 2. */
export function VisitRecordView({ draft }: { draft: VisitDraftResponse }) {
  const { visit, transcript, extractionValid, validationMessages } = draft;
  const b = visit.beneficiary;
  const o = visit.observations;
  const a = visit.actions;

  return (
    <div className="record">
      <div className="record__statusrow">
        <span className="badge badge--offline">{visit.confirmationStatus}</span>
        <span className="badge badge--online">{visit.syncStatus}</span>
        <span className="muted">{visit.visitType}</span>
      </div>

      {!extractionValid && (
        <div className="alert" role="alert">
          <strong>Please review.</strong> The details couldn’t be auto-filled reliably —
          check them against the transcript below.
          {validationMessages.length > 0 && (
            <ul>
              {validationMessages.map((m) => (
                <li key={m}>{m}</li>
              ))}
            </ul>
          )}
        </div>
      )}

      <Section title="Beneficiary">
        <Field label="Name" value={b?.name} />
        <Field label="Category" value={b?.category} />
        <Field label="Age (years)" value={b?.ageYears} />
        <Field label="Gender" value={b?.gender} />
      </Section>

      <Section title="Observations">
        <Field label="Weight (kg)" value={o?.weightKg} />
        <Field label="Temperature (°C)" value={o?.temperatureC} />
        <Field
          label="Blood pressure"
          value={
            o?.bloodPressure?.systolic != null
              ? `${o.bloodPressure.systolic}/${o.bloodPressure.diastolic ?? '—'}`
              : undefined
          }
        />
        <Field label="Gestation (weeks)" value={o?.gestationWeeks} />
        <Field label="Symptoms" value={o?.reportedSymptoms?.join(', ')} />
        <Field label="Notes" value={o?.notes} />
      </Section>

      <Section title="Actions">
        <Field label="Medicines handed over" value={a?.medicinesHandedOver?.join(', ')} />
        <Field
          label="Referral"
          value={
            a?.referral?.referred
              ? `${a.referral.facility ?? 'facility?'} (${a.referral.urgency ?? 'ROUTINE'})`
              : undefined
          }
        />
        <Field label="Next visit" value={a?.nextVisitDate} />
      </Section>

      <details className="transcript">
        <summary>Transcript</summary>
        <p lang={visit.provenance?.sourceLanguage ?? undefined}>{transcript}</p>
      </details>

      {visit.provenance?.warnings && visit.provenance.warnings.length > 0 && (
        <details className="transcript">
          <summary>Extraction notes ({visit.provenance.warnings.length})</summary>
          <ul>
            {visit.provenance.warnings.map((w) => (
              <li key={w}>{w}</li>
            ))}
          </ul>
        </details>
      )}
    </div>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="record__section">
      <h3 className="record__sectiontitle">{title}</h3>
      <dl className="record__fields">{children}</dl>
    </section>
  );
}

function Field({ label, value }: { label: string; value?: string | number | null }) {
  const isEmpty = value === null || value === undefined || value === '';
  return (
    <div className="field">
      <dt>{label}</dt>
      <dd className={isEmpty ? 'field__empty' : undefined}>{isEmpty ? 'not recorded' : value}</dd>
    </div>
  );
}
