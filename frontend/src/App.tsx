import { RecordVisit } from './components/RecordVisit';
import { useSyncQueue } from './offline/useSyncQueue';

/**
 * App shell. Online capture (P1), readback/edit (P2), and the offline queue + reconcile-on-reconnect
 * (P3) are wired in. The header reflects live connectivity and the pending-sync count.
 */
export function App() {
  const { online, pending, syncing } = useSyncQueue();

  const status = !online
    ? `Offline${pending ? ` — ${pending} queued` : ''}`
    : syncing
      ? 'Syncing…'
      : pending
        ? `${pending} pending`
        : 'Online';

  return (
    <main className="shell">
      <header className="shell__header">
        <h1 className="shell__title">SwasthyaVaani</h1>
        <span
          className={`badge ${online && !pending ? 'badge--online' : 'badge--offline'}`}
          role="status"
          aria-live="polite"
        >
          {status}
        </span>
      </header>

      <section className="shell__body">
        <p className="lead">Narrate a home visit; the record writes itself.</p>
        <RecordVisit />
      </section>

      <footer className="shell__footer muted">
        Proof-of-concept · not a medical device · data stays in India (ap-south-1)
      </footer>
    </main>
  );
}
