import { useOnlineStatus } from './hooks/useOnlineStatus';

/**
 * Phase 0 app shell. A deliberately minimal, installable, offline-aware shell that later phases
 * fill in: voice capture (P1), readback/edit (P2), the offline queue + sync (P3), OCR (P4).
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
        <p className="lead">
          Voice-first, offline-capable field documentation for frontline health workers.
        </p>
        <p className="muted">
          Phase&nbsp;0 scaffold. Voice capture, structured extraction, spoken readback, and
          sync-later arrive in the phases tracked in <code>docs/roadmap.md</code>.
        </p>

        <button className="cta" type="button" disabled aria-disabled="true">
          🎙️ Record a visit (coming in Phase&nbsp;1)
        </button>
      </section>

      <footer className="shell__footer muted">
        Proof-of-concept · not a medical device · data stays in India (ap-south-1)
      </footer>
    </main>
  );
}
