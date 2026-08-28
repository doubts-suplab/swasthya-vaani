import { RecordVisit } from './components/RecordVisit';
import { useOnlineStatus } from './hooks/useOnlineStatus';

/**
 * App shell. Phase 1 wires in the online capture flow (record → STT → extraction → review).
 * Readback/edit (P2), the offline queue + sync (P3), and OCR (P4) build on top.
 */
export function App() {
  const online = useOnlineStatus();

  return (
    <main className="shell">
      <header className="shell__header">
        <h1 className="shell__title">SwasthyaVaani</h1>
        <span
          className={`badge ${online ? 'badge--online' : 'badge--offline'}`}
          role="status"
          aria-live="polite"
        >
          {online ? 'Online' : 'Offline — visits will be queued'}
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
