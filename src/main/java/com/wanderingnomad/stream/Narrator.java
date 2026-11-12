package com.wanderingnomad.stream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Narrator captions: a lower third that tells the run's moments in the third person, in one of
 * three tones. Each line is a template filled with the circle, the place and the moment, and no
 * line comes back within {@link #QUIET} played minutes.
 */
public final class Narrator
{
    public enum Tone
    {
        SAGA,
        DRY,
        HUSHED;

        /** A tone from a word chat might type, or null. */
        public static Tone of(String word)
        {
            if (word == null) return null;
            switch (word.trim().toLowerCase(Locale.ROOT))
            {
                case "saga":
                case "epic":
                case "legend":
                case "heroic":
                    return SAGA;
                case "dry":
                case "wry":
                case "deadpan":
                case "funny":
                    return DRY;
                case "hushed":
                case "eerie":
                case "spooky":
                case "dark":
                case "creepy":
                    return HUSHED;
                default:
                    return null;
            }
        }
    }

    /** Twenty played hours. */
    public static final int QUIET = 1200;

    /** Templates by kind, then tone: {who} the circle, {place}, {mood}, {way}, {other}, {secs}, {storm}. */
    private static final Map<String, String[][]> LINES = new HashMap<>();

    static
    {
        put(Beat.OUT,
            new String[]{"The nomad stepped beyond {who}'s edge in {place}, and the world grew cold.",
                "In {place}, the nomad strayed outside {who}, and {who} quickened, restless.",
                "Outside the circle at last, the nomad stood alone in {place}."},
            new String[]{"The nomad has left the circle. The circle has noticed.", "{place}: the nomad, outside, again.",
                "The nomad steps outside. {who} sighs and speeds up."},
            new String[]{"Outside the circle, {place} is very quiet.", "The edge slid past the nomad in {place}. Something watched.",
                "Beyond {who}'s edge, the air in {place} tastes of iron."});
        put(Beat.CROSS,
            new String[]{"{who} opened its arms, and the nomad passed into a new circle over {place}.",
                "Across the touching edges the nomad went, into the keeping of {who}."},
            new String[]{"New circle, same nomad. {place} is now somebody else's problem.", "The nomad changed circles in {place}. Paperwork pending."},
            new String[]{"Where the circles touched, over {place}, the nomad slipped through.", "The nomad crossed over. {who} closed behind."});
        put(Beat.FORK,
            new String[]{"{who} hesitated over {place}, as if it could not choose between the {way} and the {other}.",
                "A tremor ran through {who}: it would soon be two, and only one would live."},
            new String[]{"{who} is about to become two circles. Nobody asked for this.", "Fork incoming. The nomad pretends to have a plan."},
            new String[]{"{who} began to tear, softly, along a line through {place}.", "Somewhere in {who}, a seam is opening."});
        put(Beat.PARTED,
            new String[]{"The {way} half lived. The other faded over {place}, unmourned.",
                "And so the circle chose its heir, and {who} went on to the {way}."},
            new String[]{"One half lives, one half doesn't. The nomad picked correctly, by definition.",
                "The {way} half won. The other half has been let go."},
            new String[]{"The other half thinned to nothing over {place}. No one saw it go.", "{who} is one again. The other half is somewhere else now."});
        put(Beat.STORM,
            new String[]{"The sky over {place} broke, and {who} was driven before the {storm}.",
                "The {storm} came for {who}, and the nomad held on."},
            new String[]{"A {storm}. Of course there's a {storm}.", "Weather report for {place}: {storm}, and the circle hates it."},
            new String[]{"The {storm} rolled over {place} without a sound.", "Under the {storm}, {who} moved like something hunted."});
        put(Beat.NEAR,
            new String[]{"The edge brushed the nomad's heel in {place}, {secs} seconds from ruin.",
                "{secs} seconds. That was all that stood between the nomad and the dark."},
            new String[]{"{secs} seconds to spare. The nomad will claim this was planned.", "Close call in {place}. Nobody panic. The nomad already did."},
            new String[]{"The edge came within a breath in {place}. It will come again.", "For {secs} seconds, the edge and the nomad shared a tile."});
        put(Beat.CLAIM,
            new String[]{"At {place}, the nomad laid a hand on the waystone, and it answered.", "The waystone gave up its gift to the nomad in {place}."},
            new String[]{"The nomad touched a rock and got a prize. Classic.", "Waystone claimed in {place}. Into the pocket it goes."},
            new String[]{"The stone in {place} went cold under the nomad's hand.", "Something passed from the waystone to the nomad. It will be spent."});
        put(Beat.RARE,
            new String[]{"A rare stone rose at {place}, older than the circle itself.", "The earth of {place} split, and a stone of legend stood up."},
            new String[]{"A shiny stone appeared. The nomad pretends not to want it.", "Rare stone in {place}. Chat, start the countdown."},
            new String[]{"Something rose at {place} that should have stayed buried.", "A stone stands in {place} that wasn't there a moment ago."});
        put(Beat.COMET,
            new String[]{"A comet crossed {who}'s sky, and the nomad caught it.", "The nomad stood in the comet's path over {place}, and it gave a gift."},
            new String[]{"The nomad caught a comet. Normal day.", "Comet caught. The sky would like it back."},
            new String[]{"The comet's light passed through the nomad and kept going.", "Over {place}, the comet touched the ground, once."});
        put(Beat.CASTAWAY,
            new String[]{"The sea cast {who} ashore at {place}, far from where it meant to land.", "A storm at sea, and {who} washed up in {place}, defiant."},
            new String[]{"{who} missed its stop. Now it lives in {place}.", "The sea has rerouted the circle. No refunds."},
            new String[]{"The sea gave {who} back at {place}, but not where it was promised.", "Something at sea turned {who} aside."});
        put(Beat.SAIL,
            new String[]{"{who} took to the sea from {place}, and the nomad had to follow.", "The tide called, and {who} answered from {place}."},
            new String[]{"{who} has booked a boat. The nomad has not.", "The circle is going on holiday. The nomad is not invited, yet."},
            new String[]{"{who} slid off the land at {place} and onto the water.", "The circle is at sea now. The land is very still."});
        put(Beat.MOOD,
            new String[]{"Over {place}, {who}'s heart turned {mood}.", "{who} grew {mood}, and the road ahead changed with it."},
            new String[]{"{who} is {mood} now. It does this.", "Mood update: {mood}. Nobody asked."},
            new String[]{"Something changed in {who} over {place}. It is {mood} now.", "{who} has gone {mood}, and won't say why."});
        put(Beat.DEATH,
            new String[]{"In {place}, the nomad fell. {who} drifted on.", "The nomad fell in {place}, and rose again to follow."},
            new String[]{"The nomad died in {place}. {who} did not stop to check.", "Death in {place}. {who} has not been informed."},
            new String[]{"The nomad fell in {place}. {who} kept moving, as it always does.", "For a moment in {place}, there was no nomad at all."});
        put(Beat.ACT,
            new String[]{"And so a new act began, in {place}.", "The story turned a page in {place}."},
            new String[]{"New act. Same circle. Higher stakes, allegedly.", "Act break. Stretch your legs."},
            new String[]{"Something ended in {place}, and something else began.", "A new act opened, quietly, in {place}."});
    }

    private static void put(String kind, String[] saga, String[] dry, String[] hushed)
    {
        LINES.put(kind, new String[][]{saga, dry, hushed});
    }

    /** Played minute each line was last told, by "tone/kind/index". */
    public Map<String, Integer> used = new HashMap<>();

    public Narrator ready()
    {
        if (used == null) used = new HashMap<>();
        return this;
    }

    /** Whether there are lines for a kind of moment at all. */
    public static boolean tells(String kind)
    {
        return LINES.containsKey(kind);
    }

    /**
     * A caption for a moment, or null when every line for it was told within {@link #QUIET}
     * played minutes. {@code vars} fills the template; a missing place reads "the wilds".
     */
    public String line(Tone tone, String kind, Map<String, String> vars, int minute)
    {
        String[][] byTone = LINES.get(kind);
        if (byTone == null) return null;
        String[] lines = byTone[tone.ordinal()];
        List<Integer> free = new ArrayList<>();
        for (int i = 0; i < lines.length; i++)
        {
            Integer last = used.get(key(tone, kind, i));
            if (last == null || minute - last >= QUIET) free.add(i);
        }
        if (free.isEmpty()) return null;
        int i = free.get(Math.floorMod(Foretold.mix(minute * 131L + kind.hashCode()), free.size()));
        used.put(key(tone, kind, i), minute);
        return fill(lines[i], vars);
    }

    static String fill(String template, Map<String, String> vars)
    {
        String out = template;
        for (String name : new String[]{"who", "place", "mood", "way", "other", "secs", "storm"})
        {
            String v = vars.get(name);
            if (v == null) v = "place".equals(name) ? "the wilds" : "who".equals(name) ? "the circle" : "storm".equals(name) ? "storm" : "";
            out = out.replace("{" + name + "}", v);
        }
        // A sentence that opens with a circle's name keeps its capital; one that opens with "the" gets one.
        return out.isEmpty() ? out : Character.toUpperCase(out.charAt(0)) + out.substring(1);
    }

    private static String key(Tone tone, String kind, int i)
    {
        return tone.name() + "/" + kind + "/" + i;
    }
}
