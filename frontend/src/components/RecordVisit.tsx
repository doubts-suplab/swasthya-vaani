import { useRef, useState } from 'react';
import { ingestPhoto, transcribeVisit } from '../api/visitApi';
import { useVoiceCapture } from '../hooks/useVoiceCapture';
import { enqueueVisit } from '../offline/syncEngine';
import type { VisitDraftResponse, VisitRecord } from '../types/visit';
import { VisitRecordView } from './VisitRecordView';
import { VisitReview } from './VisitReview';

type Phase = 'capture' | 'processing' | 'review' | 'confirmed';

/** Map an audio MIME type to a filename extension the backend/Sarvam accept. */
function extensionFor(mimeType: string): string {
  if (mimeType.includes('webm')) return 'webm';
  if (mimeType.includes('aac')) return 'aac';
  if (mimeType.includes('mp4') || mimeType.includes('m4a')) return 'm4a';
  if (mimeType.includes('ogg')) return 'ogg';
  return 'wav';
}

/** Capture a visit by voice (P1/P2) or by photographing a paper record (P4), then review. */
export function RecordVisit() {
  const recorder = useVoiceCapture();
  const [phase, setPhase] = useState<Phase>('capture');
  const [draft, setDraft] = useState<VisitDraftResponse | null>(null);
  const [confirmed, setConfirmed] = useState<VisitRecord | null>(null);
  const [error, setError] = useState<string | null>(null);
  const photoInput = useRef<HTMLInputElement>(null);

  async function run(work: () => Promise<VisitDraftResponse>) {
    setPhase('processing');
    setError(null);
    try {
      setDraft(await work());
      setPhase('review');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Something went wrong.');
      setPhase('capture');
    }
  }

  function submitRecording() {
    if (!recorder.recording) return;
    const ext = extensionFor(recorder.recording.mimeType);
    void run(() =>
      transcribeVisit(recorder.recording!.blob, `visit.${ext}`, { languageCode: 'bn-IN' }),
    );
  }

  function submitPhoto(file: File) {
    void run(() => ingestPhoto(file, file.name || 'record.jpg', { languageCode: 'bn-IN' }));
  }

  function startOver() {
    recorder.reset();
    setDraft(null);
    setConfirmed(null);
    setError(null);
    if (photoInput.current) photoInput.current.value = '';
    setPhase('capture');
  }

  if (phase === 'review' && draft) {
    return (
      <VisitReview
        draft={draft}
        onConfirmed={async (record) => {
          await enqueueVisit(record); // save locally + trigger sync (works offline)
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
          ✓ Visit {confirmed.confirmationStatus.toLowerCase()} — saved locally and queued to sync.
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

      {recorder.supported && recorder.status !== 'recording' && !recorder.recording && (
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
            <button className="cta cta--enabled" type="button" onClick={submitRecording}>
              Transcribe visit
            </button>
            <button className="linkbtn" type="button" onClick={startOver}>
              Discard
            </button>
          </div>
        </div>
      )}

      {phase === 'capture' && !recorder.recording && recorder.status !== 'recording' && (
        <>
          {recorder.supported && <div className="capture__or">or</div>}
          <button
            className="cta cta--enabled"
            type="button"
            onClick={() => photoInput.current?.click()}
          >
            📷 Photograph a paper record
          </button>
          <input
            ref={photoInput}
            type="file"
            accept="image/*"
            capture="environment"
            hidden
            onChange={(e) => {
              const file = e.target.files?.[0];
              if (file) submitPhoto(file);
            }}
          />
          {!recorder.supported && (
            <p className="muted">Voice recording isn’t available here — use a photo instead.</p>
          )}
        </>
      )}

      {phase === 'processing' && <p className="muted">Reading the visit and extracting details…</p>}
    </div>
  );
}
