import { useState } from 'react';
import { RuleDemo } from '../components/RuleDemo';
import type { Geography } from '../engine/geography';
import { GROWTH, LAWS } from './laws';

/** The rulebook, each law with a tiny live demo to poke (the living rulebook). */
export function Rules({ geo }: { geo: Geography | null }) {
  const [open, setOpen] = useState<string | null>(null);
  return (
    <article className="page prose">
      <div className="osrs-win window">
      <div className="win-body">
      <p className="osrs-title">The rulebook</p>
      <h1>Nomad UIM</h1>
      <p className="lede">Ultimate Ironman, but you can only play inside circles that drift across Gielinor.</p>

      <h2>The goal</h2>
      <p>
        Earn the Infernal cape. The Inferno and the Fight Caves only count while you are in the Main circle and Main covers the volcano on
        Karamja, so the hardest fight in the game waits on the world coming to you.
      </p>

      <h2>The circles</h2>
      <p>
        You start in the <strong>Main circle</strong> at Lumbridge. It is the only circle that roams the whole world, the only one that
        boards boats, and the only one that opens the Inferno. Each of the eleven kingdoms (Kandarin, Fremennik, Asgarnia, Misthalin, the
        Kharidian Desert, Morytania, the Wilderness, Karamja, Kourend &amp; Kebos, Tirannwn and Varlamore) has its own circle that never
        leaves its borders.
      </p>

      <h2>The laws</h2>
      <dl className="law-list">
        {LAWS.map((l) => (
          <div key={l.name}>
            <dt>{l.name}</dt>
            <dd>
              <p>
                <strong>{l.short}</strong>
              </p>
              <p>{l.long}</p>
              {geo && (
                <button type="button" className="btn btn-small" aria-expanded={open === l.name} onClick={() => setOpen(open === l.name ? null : l.name)}>
                  {open === l.name ? 'Hide the demo' : 'Show me'}
                </button>
              )}
              {geo && open === l.name && <RuleDemo key={l.name} geo={geo} law={l.name} />}
            </dd>
          </div>
        ))}
      </dl>

      <h2>Growing your world</h2>
      {GROWTH.map((g) => (
        <p key={g}>{g}</p>
      ))}

      <h2>Variants</h2>
      <ul>
        <li>
          <strong>Nomad UIM:</strong> the standard mode.
        </li>
        <li>
          <strong>Nomad HCUIM:</strong> one life. The run ends on your first death.
        </li>
        <li>On either, you may never reclaim a death pile.</li>
      </ul>

      <h2>Waystones</h2>
      <p>
        Milestones like total level, quest points and first boss kills summon a waystone somewhere in the world. Reach it, inside its
        circle, to claim a one-use boon, such as a bigger radius, a burst of speed or a nudge of the wheel. You hold three at most.
      </p>
      </div>
      </div>
    </article>
  );
}
