import { useCallback, useEffect, useState } from 'react';
import { countPending } from './visitQueue';
import { drainQueue, onQueueChanged } from './syncEngine';

/**
 * Watches the offline queue and reconciles it whenever connectivity returns or the queue changes.
 * Exposes the pending count and a live online flag for the UI. Drains on mount, on the browser
 * `online` event, and on any local enqueue.
 */
export function useSyncQueue() {
  const [pending, setPending] = useState(0);
  const [syncing, setSyncing] = useState(false);
  const [online, setOnline] = useState(typeof navigator !== 'undefined' ? navigator.onLine : true);

  const refreshCount = useCallback(async () => {
    setPending(await countPending());
  }, []);

  const flush = useCallback(async () => {
    if (typeof navigator !== 'undefined' && !navigator.onLine) {
      await refreshCount();
      return;
    }
    setSyncing(true);
    try {
      await drainQueue();
    } finally {
      setSyncing(false);
      await refreshCount();
    }
  }, [refreshCount]);

  useEffect(() => {
    void flush();

    const goOnline = () => {
      setOnline(true);
      void flush();
    };
    const goOffline = () => setOnline(false);

    window.addEventListener('online', goOnline);
    window.addEventListener('offline', goOffline);
    const unsubscribe = onQueueChanged(() => void flush());

    return () => {
      window.removeEventListener('online', goOnline);
      window.removeEventListener('offline', goOffline);
      unsubscribe();
    };
  }, [flush]);

  return { pending, syncing, online, flush };
}
