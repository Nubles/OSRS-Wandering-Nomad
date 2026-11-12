import { useState } from 'react';
import { lastRun } from '../story';

interface Props {
  /** Where a loaded code goes, such as "#/saga/". */
  base: string;
  label?: string;
  error?: string | null;
}

/** Paste a run code, or reuse the last one this browser loaded. */
export function RunCodeBox({ base, label = "Paste a run code from the plugin's sidebar (Copy run code):", error }: Props) {
  const [code, setCode] = useState('');
  const last = lastRun();
  return (
    <div className="run-code-box">
      {error && <div className="osrs-chat notice notice-error" role="alert">{error}</div>}
      <label className="small muted" htmlFor={`code-${base}`}>
        {label}
      </label>
      <div className="row">
        <input id={`code-${base}`} className="input grow" value={code} placeholder="N1.…" onChange={(e) => setCode(e.target.value)} />
        <button type="button" className="btn" disabled={!code.trim()} onClick={() => (location.hash = base + code.trim())}>
          Load
        </button>
        {last && (
          <button type="button" className="btn" onClick={() => (location.hash = base + last)}>
            Use my last run
          </button>
        )}
      </div>
    </div>
  );
}
