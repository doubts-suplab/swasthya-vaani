import { useState } from 'react';
import { transcribeVisit } from '../api/visitApi';
import { useAudioRecorder } from '../hooks/useAudioRecorder';
import type { VisitDraftResponse, VisitRecord } from '../types/visit';
import { VisitRecordView } from './VisitRecordView';
import { VisitReview } from './VisitReview';

type Phase = 'capture' | 'processing' | 'review' | 'confirmed';

/** Phase 1 capture flow: record → upload → review the extracted draft. */
export function RecordVisit() {
  const recorder = useAudioRecorder();
  const [phase, setPhase] = useState<Phase>('capture');
  const [draft, setDraft] = useState<VisitDraftResponse | null>(null);
  const [confirmed, setConfirmed] = useState<VisitRecord | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function submit() {
    if (!recorder.recording) return;
    setPhase('processing');
    setError(null);
    try {
      const ext = recorder.recording.mimeType.includes('webm') ? 'webm' : 'wav';
      const result = await transcribeVisit(recorder.recording.blob, `visit.${ext}`, {
        languageCode: 'bn-IN',
      });
      setDraft(result);
      setPhase('review');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Something went wrong.');
      setPhase('capture');
    }
  }

  function startOver() {
    recorder.reset();
    setDraft(null);
    setConfirmed(null);
    setError(null);
    setPhase('capture');
  }

  if (recorder.status === 'unsupported') {
    return <p className="muted">Audio recording isn’t supported on this device/browser.</p>;
  }

  if (phase === 'review' && draft) {
    return (
      <VisitReview
        draft={draft}
        onConfirmed={(record) => {
          setConfirmed(record);
          setPhase('confirmed');
        }}
      />
    );
  }

  if (phase === 'confirmed' && draft && confirmed) {
    return (
      <div>
        <div className="alert alert--ok" role="status">
          ✓ Visit {confirmed.confirmationStatus.toLowerCase()} — queued to sync.
        </div>
        <VisitRecordView draft={{ ...draft, visit: confirmed }} />
        <button className="cta cta--enabled" type="button" onClick={startOver}>
          Record another visit
        </button>
      </div>
    );
  }

  return (
    <div className="capture">
      {error && (
        <div className="alert" role="alert">
          {error}
        </div>
      )}

      {recorder.status !== 'recording' && !recorder.recording && (
        <button className="cta cta--enabled" type="button" onClick={recorder.start}>
          🎙️ Start recording
        </button>
      )}

      {recorder.status === 'recording' && (
        <button className="cta cta--recording" type="button" onClick={recorder.stop}>
          ⏹ Stop recording
        </button>
      )}

      {recorder.recording && phase === 'capture' && (
        <div className="capture__review">
          <audio controls src={recorder.recording.url} />
          <div className="capture__actions">
            <button className="cta cta--enabled" type="button" onClick={submit}>
              Transcribe visit
            </button>
            <button className="linkbtn" type="button" onClick={startOver}>
              Discard
            </button>
          </div>
        </div>
      )}

      {phase === 'processing' && <p className="muted">Transcribing and extracting…</p>}
    </div>
  );
}
