import { useCallback, useRef, useState } from 'react';

export type RecorderStatus = 'idle' | 'recording' | 'recorded' | 'unsupported';

export interface AudioRecording {
  blob: Blob;
  url: string;
  mimeType: string;
}

/**
 * Thin wrapper over the MediaRecorder API for capturing a single visit recording. Kept UI-free so
 * it can be swapped for the realtime streaming capture in Phase 3 without touching components.
 */
export function useAudioRecorder() {
  const supported =
    typeof window !== 'undefined' &&
    typeof navigator !== 'undefined' &&
    !!navigator.mediaDevices &&
    typeof window.MediaRecorder !== 'undefined';

  const [status, setStatus] = useState<RecorderStatus>(supported ? 'idle' : 'unsupported');
  const [recording, setRecording] = useState<AudioRecording | null>(null);
  const [error, setError] = useState<string | null>(null);

  const recorderRef = useRef<MediaRecorder | null>(null);
  const chunksRef = useRef<Blob[]>([]);

  const start = useCallback(async () => {
    if (!supported) return;
    setError(null);
    setRecording(null);
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const mimeType = MediaRecorder.isTypeSupported('audio/webm') ? 'audio/webm' : '';
      const recorder = new MediaRecorder(stream, mimeType ? { mimeType } : undefined);
      chunksRef.current = [];

      recorder.ondataavailable = (e) => {
        if (e.data.size > 0) chunksRef.current.push(e.data);
      };
      recorder.onstop = () => {
        const type = recorder.mimeType || 'audio/webm';
        const blob = new Blob(chunksRef.current, { type });
        setRecording({ blob, url: URL.createObjectURL(blob), mimeType: type });
        setStatus('recorded');
        stream.getTracks().forEach((t) => t.stop());
      };

      recorderRef.current = recorder;
      recorder.start();
      setStatus('recording');
    } catch {
      setError('Microphone permission denied or unavailable.');
      setStatus('idle');
    }
  }, [supported]);

  const stop = useCallback(() => {
    recorderRef.current?.stop();
  }, []);

  const reset = useCallback(() => {
    if (recording) URL.revokeObjectURL(recording.url);
    setRecording(null);
    setError(null);
    setStatus(supported ? 'idle' : 'unsupported');
  }, [recording, supported]);

  return { status, recording, error, start, stop, reset, supported };
}
