package com.wanderingnomad.wild;

import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Region;
import com.wanderingnomad.sim.Geography;
import com.wanderingnomad.sim.Rng;
import com.wanderingnomad.sim.Tile;
import java.util.ArrayList;
import java.util.List;

/**
 * The wildcards: seventeen odd rules, each a setting and off by default, kept per run as one JSON
 * value. Plugin-side only: they act on the circles through the fields a run already saves, so the
 * website needs nothing new. The plugin feeds this its events and tells what comes back in an
 * {@link Out}.
 */
public final class Wild
{
    /** Which wildcards are on, from the settings; set before each call. */
    public static final class Flags
    {
        public boolean quantum;
        public boolean drop;
        public boolean porter;
        public boolean meteors;
        public boolean redeploy;
        public boolean bonfire;
        public boolean inspiration;
        public boolean eggs;
        public boolean roamer;
        public boolean visa;
        public boolean fog;
        public boolean chaos;
        public boolean chart;
        public boolean tides;
        public boolean wayguess;
        public boolean pasture;
        public boolean favour;

        public boolean any()
        {
            return quantum || drop || porter || meteors || redeploy || bonfire || inspiration || eggs || roamer || visa || fog || chaos
                || chart || tides || wayguess || pasture || favour;
        }
    }

    public transient Flags on = new Flags();

    Quantum quantum = new Quantum();
    Drop drop = new Drop();
    Porter porter = new Porter();
    Meteors meteors = new Meteors();
    Carrier carrier = new Carrier();
    Embers embers = new Embers();
    Inspiration inspiration = new Inspiration();
    Eggs eggs = new Eggs();
    Roamer roamer = new Roamer();
    Visa visa = new Visa();
    Fog fog = new Fog();
    Chaos chaos = new Chaos();
    Chart chart = new Chart();
    Tides tides = new Tides();
    Wayguess wayguess = new Wayguess();
    Pasture pasture = new Pasture();
    Favour favour = new Favour();

    /** Fills in parts a run saved by an older plugin lacks. Call after loading from JSON. */
    public Wild ready()
    {
        if (on == null) on = new Flags();
        if (quantum == null) quantum = new Quantum();
        if (drop == null) drop = new Drop();
        if (porter == null) porter = new Porter();
        if (meteors == null) meteors = new Meteors();
        if (carrier == null) carrier = new Carrier();
        if (embers == null) embers = new Embers();
        if (inspiration == null) inspiration = new Inspiration();
        if (eggs == null) eggs = new Eggs();
        if (roamer == null) roamer = new Roamer();
        if (visa == null) visa = new Visa();
        if (fog == null) fog = new Fog();
        if (chaos == null) chaos = new Chaos();
        if (chart == null) chart = new Chart();
        if (tides == null) tides = new Tides();
        if (wayguess == null) wayguess = new Wayguess();
        if (pasture == null) pasture = new Pasture();
        if (favour == null) favour = new Favour();
        quantum.ready();
        eggs.ready();
        pasture.ready();
        return this;
    }

    public Quantum quantum()
    {
        return quantum;
    }

    public Drop drop()
    {
        return drop;
    }

    public Porter porter()
    {
        return porter;
    }

    public Meteors meteors()
    {
        return meteors;
    }

    public Carrier carrier()
    {
        return carrier;
    }

    public Embers embers()
    {
        return embers;
    }

    public Inspiration inspiration()
    {
        return inspiration;
    }

    public Eggs eggs()
    {
        return eggs;
    }

    public Roamer roamer()
    {
        return roamer;
    }

    public Visa visa()
    {
        return visa;
    }

    public Fog fog()
    {
        return fog;
    }

    public Chaos chaos()
    {
        return chaos;
    }

    public Chart chart()
    {
        return chart;
    }

    public Wayguess wayguess()
    {
        return wayguess;
    }

    public Pasture pasture()
    {
        return pasture;
    }

    public Favour favour()
    {
        return favour;
    }

    /** The Drop: starts it on a fresh run, returning where Main will be born; null if it's off. */
    public Tile beginDrop(int seed)
    {
        return on.drop ? drop.begin(seed) : null;
    }

    /** Whether the drop is still closing: the circles wait, and the plugin plays only {@link #dropMinute}. */
    public boolean dropping()
    {
        return drop.on();
    }

    /** A played minute while the drop closes. True on the minute it lands. */
    public boolean dropMinute(Out out)
    {
        return drop.minute(out);
    }

    /** Whether a wildcard counts you as inside where you stand: the drop, the carrier or the embers. */
    public boolean shelters(Tile at)
    {
        return (drop.on() && drop.contains(at)) || (on.redeploy && carrier.contains(at)) || (on.bonfire && embers.contains(at));
    }

    /** Whether the fog hides the map, Radar and Forecast. */
    public boolean fogged()
    {
        return on.fog && fog.on();
    }

    /**
     * Once a played minute, after the circles moved. {@code inside}: you count as inside;
     * {@code comebacks}: the run's comeback count; {@code held}: whether your Cargo is still on you;
     * {@code today}: the UTC day.
     */
    public void minute(NomadSim sim, Tile you, boolean inside, int comebacks, boolean held, long today, Out out)
    {
        if (on.quantum) quantum.minute(sim, out);
        if (on.porter) porter.minute(sim.seed(), sim.minutes(), held, inside, kingdom(you), out);
        if (on.meteors) meteors.minute(sim, out);
        if (on.redeploy) carrier.minute(sim, out);
        if (on.bonfire) embers.minute(sim, out);
        if (on.inspiration) inspiration.minute(comebacks, out);
        if (on.eggs) eggs.minute(sim, out);
        if (on.roamer)
        {
            roamer.minute(sim, out);
            roamer.touch(sim, you, out);
        }
        if (on.visa) visa.minute(sim, out);
        if (on.fog) fog.minute(sim, out);
        if (on.chaos) chaos.minute(sim, you, inside, out);
        if (on.chart) chart.minute(sim, out);
        if (on.tides) tides.minute(sim, out);
        if (on.wayguess) wayguess.minute(sim, today, out);
        if (on.pasture) pasture.minute(sim, out);
        if (on.favour) favour.minute(sim.minutes(), out);
    }

    /**
     * Each game tick: the tile you stand on, whether you count as inside, and whether each quantum
     * stone is on screen (by index into the quantum stones; null to skip them).
     */
    public void tick(NomadSim sim, Tile you, boolean inside, boolean[] seen, Out out)
    {
        if (on.eggs) eggs.step(you, inside);
        if (on.redeploy) carrier.tick(sim, you, out);
        if (on.roamer) roamer.touch(sim, you, out);
        if (on.quantum && seen != null) quantum.tick(sim, seen, new Rng(sim.seed() ^ sim.minutes() ^ 0x9A4));
    }

    /** You died where you stood. */
    public void died(Tile at)
    {
        if (on.redeploy) carrier.died(at);
    }

    /** A fire caught where you stand. */
    public void fire(NomadSim sim, Tile at, boolean inside, Out out)
    {
        if (on.bonfire) embers.light(at, inside, sim.minutes(), out);
    }

    /** You took an item off the ground. */
    public void pickup(NomadSim sim, int item, String name, Tile at, Out out)
    {
        if (on.porter) porter.take(item, name, kingdom(at), sim.minutes(), out);
    }

    /** A trial was finished. */
    public void trial(Out out)
    {
        if (on.eggs) eggs.trial(out);
    }

    /** XP in a skilling skill where you stand. */
    public void skilled(NomadSim sim, Tile at)
    {
        if (on.pasture) pasture.skill(at, sim.minutes());
    }

    /** Prayer XP gained at an altar while inside your circle. */
    public void offered(NomadSim sim, int xp, Out out)
    {
        if (on.favour) favour.offer(xp, sim.minutes(), out);
    }

    /**
     * Spends Inspiration on {@link Inspiration#MOOD} or {@link Inspiration#FORK} (the omen is the
     * plugin's to reroll). Returns the log line, or null if nothing was spent.
     */
    public String inspire(NomadSim sim, String what)
    {
        if (!on.inspiration || !inspiration.held()) return null;
        String line = null;
        if (Inspiration.MOOD.equals(what))
        {
            if (sim.rerollMood(sim.boundIndex())) line = "Inspired: your circle forgets its mood and picks a new one.";
        }
        else if (Inspiration.FORK.equals(what))
        {
            String turned = sim.rerollFork();
            if (turned != null) line = "Inspired: " + turned;
        }
        if (line != null) inspiration.spend();
        return line;
    }

    /** Spends Inspiration on the omen, once the plugin has rerolled it. */
    public boolean spendInspiration()
    {
        return on.inspiration && inspiration.spend();
    }

    /** You picked a route on the Chart. */
    public boolean chart(NomadSim sim, int index, Out out)
    {
        return on.chart && chart.choose(sim, index, out);
    }

    /** Your Wayguess for tomorrow. */
    public void wayguess(String region, long today, Out out)
    {
        if (on.wayguess) wayguess.guess(region, today, out);
    }

    /** The kingdom a tile lies in, by region id, or null at sea or unknown. */
    static String kingdom(Tile at)
    {
        if (at == null) return null;
        Region r = Geography.regionAt(at.x, at.y);
        return r == null ? null : r.id;
    }

    /** One line per wildcard with something going on, for the sidebar. */
    public List<String> lines(NomadSim sim)
    {
        List<String> out = new ArrayList<>();
        int now = sim.minutes();
        if (drop.on()) out.add("The Drop: Main lands in " + drop.left() + " min");
        if (on.fog && fog.warning() > 0) out.add("Fog rolls in: " + fog.warning() + " min");
        if (on.fog && fog.on()) out.add("Fog bank: map dark for " + fog.left() + " min");
        if (on.inspiration) out.add(inspiration.held() ? "Inspiration: held, spend it below" : "Inspiration: earn one with a comeback");
        if (on.porter)
        {
            String p = porter.line(now);
            if (p != null) out.add(p);
        }
        if (on.eggs && eggs.held()) out.add("Egg: " + String.format("%,d", eggs.steps()) + "/" + String.format("%,d", Eggs.STEPS) + " new tiles");
        if (on.redeploy && carrier.on()) out.add("Carrier: walking you home, " + carrier.left() + " min left");
        if (on.bonfire && embers.on())
        {
            int left = embers.left(now);
            out.add(left < 0 ? "Embers: lit, your circle still covers them" : "Embers: legal for " + left + " min");
        }
        if (on.roamer && roamer.on())
        {
            out.add(roamer.window() > 0 ? "Roamer in Main: " + roamer.window() + " min to step in"
                : "Roamer: " + (int) Math.round(Math.hypot(roamer.x() - sim.main().x(), roamer.y() - sim.main().y())) + " tiles from Main"
                + (roamer.escapes() > 0 ? ", " + roamer.escapes() + " escapes" : ""));
        }
        if (on.visa)
        {
            int left = visa.left(sim);
            if (left > 0) out.add("Visa: " + Visa.time(left) + " left in " + sim.bound().name());
            else if (left == 0) out.add("Visa overstayed: deportation under way");
        }
        if (on.chaos && chaos.rift() != null) out.add("Chaos rift: " + chaos.open() + " min to step on it");
        if (on.chaos && chaos.handicap() != null) out.add("Chaos: " + chaos.handicap() + ", " + chaos.left() + " min" + (chaos.clean() ? "" : " (boon lost)"));
        if (on.chart)
        {
            if (chart.pickLeft() > 0) out.add("The Chart: pick a route, " + chart.pickLeft() + " min");
            String r = chart.route();
            if (r != null) out.add(r);
        }
        if (on.tides) out.add("Tide: " + (Tides.rising(sim.seed(), now) ? "rising" : "falling") + ", turns in " + Tides.turns(sim.seed(), now) + " min");
        if (on.meteors)
        {
            int n = meteors.next(now);
            if (n >= 0 && n <= 60) out.add("Meteors: the sky is restless");
        }
        if (on.wayguess) out.add(wayguess.guess() != null ? "Wayguess: " + Wayguess.name(wayguess.guess()) + " tomorrow"
            : "Wayguess: guess tomorrow's kingdom below");
        if (on.favour) out.add("Altar favour: " + favour.favour() + "/" + Favour.FULL
            + (favour.rest(now) > 0 && favour.favour() >= Favour.FULL ? ", waits " + favour.rest(now) / 60 + "h" : ""));
        if (on.quantum && !quantum.stones().isEmpty()) out.add("Quantum stones: " + quantum.stones().size() + ", keep them in view");
        return out;
    }
}
