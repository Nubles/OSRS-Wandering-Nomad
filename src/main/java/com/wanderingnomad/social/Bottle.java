package com.wanderingnomad.social;

import com.wanderingnomad.sim.Geography;
import com.wanderingnomad.sim.Landmarks;
import java.util.ArrayList;
import java.util.List;

/**
 * Bottle post: at a coast you throw a note as a code. Whoever pastes it sees the bottle wash up
 * at a coastal landmark worked out from the bottle itself, and reads it once their own circle
 * gets them there.
 */
public final class Bottle
{
    public static final int NOTE_MAX = 140;
    public static final int NAME_MAX = 12;
    /** How close you must stand to read a washed-up bottle. */
    public static final int REACH = 5;

    public String id;
    public String from;
    public String note;
    public long thrown;
    /** Where it washes up, worked out when it's pasted. */
    public String shore;
    public int x;
    public int y;

    /** A new bottle. {@code id} only needs to be unlikely to repeat, such as the time and name mixed. */
    public static Bottle thrown(String from, String note, long now)
    {
        Bottle b = new Bottle();
        b.from = Codes.clean(from, NAME_MAX);
        b.note = Codes.clean(note, NOTE_MAX);
        b.thrown = now;
        b.id = Long.toHexString(Lodestar.mix(now ^ ((long) b.from.hashCode() << 32) ^ b.note.hashCode()));
        return b;
    }

    /** Fills in where a pasted bottle washes up; false if it's not a real bottle. */
    public boolean land()
    {
        if (id == null || id.isEmpty() || note == null || note.isEmpty()) return false;
        from = Codes.clean(from, NAME_MAX);
        note = Codes.clean(note, NOTE_MAX);
        List<Landmarks.Landmark> shores = shores();
        Landmarks.Landmark l = shores.get((int) Math.floorMod(Lodestar.mix(id.hashCode() * 31L + 7), (long) shores.size()));
        shore = l.name;
        x = l.x;
        y = l.y;
        return true;
    }

    /** Landmarks with open water close by, outside the Wilderness. */
    static List<Landmarks.Landmark> shores()
    {
        List<Landmarks.Landmark> known = shores;
        if (known != null) return known;
        List<Landmarks.Landmark> out = new ArrayList<>();
        for (Landmarks.Landmark l : Lodestar.safe()) if (coastal(l.x, l.y)) out.add(l);
        shores = java.util.Collections.unmodifiableList(out);
        return shores;
    }

    private static volatile List<Landmarks.Landmark> shores;

    /** Whether open water lies within about 12 tiles. */
    public static boolean coastal(double x, double y)
    {
        for (int r = 3; r <= 12; r += 3)
        {
            for (int k = 0; k < 8; k++)
            {
                double a = k * Math.PI / 4;
                if (!Geography.onLand(x + Math.cos(a) * r, y + Math.sin(a) * r)) return true;
            }
        }
        return false;
    }

    public boolean reached(int atX, int atY)
    {
        return Math.abs(atX - x) <= REACH && Math.abs(atY - y) <= REACH;
    }
}
