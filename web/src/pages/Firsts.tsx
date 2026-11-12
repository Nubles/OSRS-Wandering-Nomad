import { useEffect, useState } from 'react';

interface Milestone {
  id: string;
  group: string;
  what: string;
  how: string;
}

interface Claim {
  id: string;
  who: string;
  /** The date it was done, as the player said, YYYY-MM-DD. */
  when: string;
  proof?: string;
}

interface Firsts {
  note: string;
  milestones: Milestone[];
  claimed: Claim[];
}

const REPO = 'https://github.com/Nubles/OSRS-Wandering-Nomad';

/** A prefilled GitHub issue to claim a first; the mode's author checks it and adds it to firsts.json. */
function claimLink(m: Milestone): string {
  const title = `First: ${m.what}`;
  const body = [
    `I think I'm the first Nomad to reach **${m.what}** (${m.how}).`,
    '',
    'Name to show:',
    'Date it happened:',
    'Proof (a screenshot or clip of the plugin, and a run code if you like):',
  ].join('\n');
  return `${REPO}/issues/new?${new URLSearchParams({ title, body, labels: 'hall of firsts' })}`;
}

/** The Hall of firsts: the first player to reach each trial, rank and finish line, recorded for good. */
export default function FirstsPage() {
  const [data, setData] = useState<Firsts | null>(null);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => {
    fetch(`${import.meta.env.BASE_URL}firsts.json`)
      .then((r) => (r.ok ? r.json() : Promise.reject(new Error(`firsts.json: ${r.status}`))))
      .then(setData, (e: Error) => setError(e.message));
  }, []);

  const groups = data ? [...new Set(data.milestones.map((m) => m.group))] : [];
  return (
    <div className="page">
      <section className="osrs-win window">
        <h1 className="win-head osrs-title">Hall of firsts</h1>
        <div className="win-body">
          <p>
            The first Nomad to reach each rank, Legend trial and finish line, written here for good. Firsts are self-reported: claim one
            with proof and it's checked by hand before it's added.
          </p>
          {error && <div className="osrs-chat notice notice-error">{error}</div>}
          {!data && !error && <p className="muted">Opening the hall…</p>}
          {groups.map((g) => (
            <section key={g}>
              <h2 className="osrs-title">{g}</h2>
              <table className="firsts-table">
                <thead>
                  <tr>
                    <th>Milestone</th>
                    <th>First</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {data!.milestones
                    .filter((m) => m.group === g)
                    .map((m) => {
                      const c = data!.claimed.find((x) => x.id === m.id);
                      return (
                        <tr key={m.id}>
                          <td>
                            <span className="value">{m.what}</span>
                            <br />
                            <span className="muted small">{m.how}</span>
                          </td>
                          <td>
                            {c ? (
                              <>
                                <strong>{c.who}</strong> <span className="muted small">{c.when}</span>
                                {c.proof && (
                                  <>
                                    {' '}
                                    <a href={c.proof} target="_blank" rel="noreferrer">
                                      proof
                                    </a>
                                  </>
                                )}
                              </>
                            ) : (
                              <span className="muted">Unclaimed</span>
                            )}
                          </td>
                          <td>
                            {!c && (
                              <a className="btn btn-small" href={claimLink(m)} target="_blank" rel="noreferrer">
                                Claim it
                              </a>
                            )}
                          </td>
                        </tr>
                      );
                    })}
                </tbody>
              </table>
            </section>
          ))}
        </div>
      </section>
    </div>
  );
}
