package com.wanderingnomad.stream;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Auto chapters: the episode's big moments, timed from when you started recording (or the
 * episode began), as YouTube chapters, a marker list for an editor, and an EDL of markers.
 */
public final class Chapters
{
    /** About this many chapters an episode, the first being the cold open. */
    public static final int MAX = 12;
    /** Chapters at least this far apart, which YouTube asks for (10 s) with room to spare. */
    static final long APART_MS = 30_000;

    public static final class Chapter
    {
        public final long offset;
        public final String title;

        Chapter(long offset, String title)
        {
            this.offset = offset;
            this.title = title;
        }
    }

    private Chapters()
    {
    }

    /** When the chapters count from: recording, or the episode's start. */
    public static long zero(Session s)
    {
        return s.recordingAt > 0 ? s.recordingAt : s.startedAt;
    }

    /** The chapters: "Cold open" at 0:00, then the heaviest beats since, at least 30 seconds apart. */
    public static List<Chapter> of(Session s)
    {
        long zero = zero(s);
        List<Beat> beats = new ArrayList<>();
        for (Beat b : s.beats) if (b.at >= zero + APART_MS && b.weight >= 2) beats.add(b);
        beats.sort(Comparator.comparingInt((Beat b) -> -b.weight).thenComparingLong(b -> b.at));
        List<Beat> picked = new ArrayList<>();
        for (Beat b : beats)
        {
            if (picked.size() >= MAX - 1) break;
            boolean clear = true;
            for (Beat p : picked) if (Math.abs(p.at - b.at) < APART_MS) clear = false;
            if (clear) picked.add(b);
        }
        picked.sort(Comparator.comparingLong(b -> b.at));
        List<Chapter> out = new ArrayList<>();
        out.add(new Chapter(0, "Cold open"));
        int marks = 0;
        for (Beat b : picked) out.add(new Chapter(b.at - zero, Beat.MARK.equals(b.kind) ? "Marked moment " + ++marks : b.title));
        return out;
    }

    /** YouTube's chapter list, one "0:00 Title" per line. */
    public static String youtube(List<Chapter> chapters)
    {
        StringBuilder out = new StringBuilder();
        for (Chapter c : chapters) out.append(time(c.offset)).append(' ').append(c.title).append('\n');
        return out.toString();
    }

    /** A plain marker list for an editor: timecode, tab, name. */
    public static String markers(List<Chapter> chapters, int fps)
    {
        StringBuilder out = new StringBuilder("Timecode\tName\n");
        for (Chapter c : chapters) out.append(timecode(c.offset, fps)).append('\t').append(c.title).append('\n');
        return out.toString();
    }

    /** The chapters as an EDL of markers, which DaVinci Resolve and others import onto a timeline. */
    public static String edl(List<Chapter> chapters, int fps)
    {
        StringBuilder out = new StringBuilder("TITLE: Nomad UIM episode\nFCM: NON-DROP FRAME\n\n");
        int n = 1;
        for (Chapter c : chapters)
        {
            String in = timecode(c.offset, fps);
            String outTc = timecode(c.offset + 1000 / Math.max(1, fps), fps);
            out.append(String.format("%03d  001      V     C        %s %s %s %s%n", n++, in, outTc, in, outTc));
            out.append(" |C:ResolveColorBlue |M:").append(c.title.replace('|', '/')).append(" |D:1\n\n");
        }
        return out.toString();
    }

    /** "1:02:03" or "2:03". */
    public static String time(long ms)
    {
        long s = Math.max(0, ms / 1000);
        long h = s / 3600;
        long m = s / 60 % 60;
        long sec = s % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, sec) : String.format("%d:%02d", m, sec);
    }

    static String timecode(long ms, int fps)
    {
        long s = Math.max(0, ms / 1000);
        long frames = Math.max(0, ms % 1000) * fps / 1000;
        return String.format("%02d:%02d:%02d:%02d", s / 3600, s / 60 % 60, s % 60, frames);
    }
}
