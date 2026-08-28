import { useEffect, useState } from 'react';

/**
 * Tracks browser connectivity. The offline-first UI reflects this so a worker always knows whether
 * a visit will sync now or be queued (CLAUDE.md §7.3). The actual queue/reconcile logic lands in
 * Phase 3; this hook is the signal it will hang off.
 */
export function useOnlineStatus(): boolean {
  const [online, setOnline] = useState<boolean>(
    typeof navigator !== 'undefined' ? navigator.onLine : true,
  );

  useEffect(() => {
    const update = () => setOnline(navigator.onLine);
    window.addEventListener('online', update);
    window.addEventListener('offline', update);
    return () => {
      window.removeEventListener('online', update);
      window.removeEventListener('offline', update);
    };
  }, []);

  return online;
}
