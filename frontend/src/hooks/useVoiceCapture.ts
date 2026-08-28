import { Capacitor } from '@capacitor/core';
import { useCallback, useRef, useState } from 'react';

export type RecorderStatus = 'idle' | 'recording' | 'recorded' | 'unsupported';

export interface AudioRecording {
  blob: Blob;
  url: string;
  mimeType: string;
}

/**
 * Unified voice capture for one visit recording. On a native Android build it uses the Capacitor
 * voice-recorder plugin (reliable capture + real permission prompt on low-end devices); on the web
 * it uses MediaRecorder. The rest of the app is identical across both — one codebase, two runtimes.
 */
export function useVoiceCapture() {
  const native = Capacitor.isNativePlatform();
  const webSupported =
    typeof window !== 'undefined' &&
    typeof navigator !== 'undefined' &&
    !!navigator.mediaDevices &&
    typeof window.MediaRecorder !== 'undefined';
  const supported = native || webSupported;

  const [status, setStatus] = useState<RecorderStatus>(supported ? 'idle' : 'unsupported');
  const [recording, setRecording] = useState<AudioRecording | null>(null);
  const [error, setError] = useState<string | null>(null);

  const recorderRef = useRef<MediaRecorder | null>(null);
  const chunksRef = useRef<Blob[]>([]);

  const start = useCallback(async () => {
    if (!supported) return;
    setError(null);
    setRecording(null);

    if (native) {
      try {
        const { VoiceRecorder } = await import('capacitor-voice-recorder');
        const permission = await VoiceRecorder.requestAudioRecordingPermission();
        if (!permission.value) {
          setError('Microphone permission denied.');
          return;
        }
        await VoiceRecorder.startRecording();
        setStatus('recording');
      } catch {
        setError('Could not start recording.');
        setStatus('idle');
      }
      return;
    }

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
  }, [native, supported]);

  const stop = useCallback(async () => {
    if (native) {
      try {
        const { VoiceRecorder } = await import('capacitor-voice-recorder');
        const result = await VoiceRecorder.stopRecording();
        const { recordDataBase64, mimeType } = result.value;
        if (!recordDataBase64) {
          setError('No audio was captured.');
          setStatus('idle');
          return;
        }
        const type = mimeType || 'audio/aac';
        const blob = base64ToBlob(recordDataBase64, type);
        setRecording({ blob, url: URL.createObjectURL(blob), mimeType: type });
        setStatus('recorded');
      } catch {
        setError('Could not finish recording.');
        setStatus('idle');
      }
      return;
    }
    recorderRef.current?.stop();
  }, [native]);

  const reset = useCallback(() => {
    if (recording) URL.revokeObjectURL(recording.url);
    setRecording(null);
    setError(null);
    setStatus(supported ? 'idle' : 'unsupported');
  }, [recording, supported]);

  return { status, recording, error, start, stop, reset, supported, native };
}

function base64ToBlob(base64: string, mimeType: string): Blob {
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i += 1) bytes[i] = binary.charCodeAt(i);
  return new Blob([bytes], { type: mimeType });
}
