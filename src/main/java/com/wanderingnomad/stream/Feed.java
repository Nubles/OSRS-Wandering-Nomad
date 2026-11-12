package com.wanderingnomad.stream;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.Geography;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Radar;
import com.wanderingnomad.sim.Region;
import com.wanderingnomad.sim.Tile;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stream feed: tiny text files for OBS text sources (mood, kingdom, edge time, chapter and more),
 * written to a folder on your own computer. Nothing is sent anywhere.
 */
public final class Feed
{
    private Feed()
    {
    }

    /** Each file's name and what it says now. */
    public static Map<String, String> files(NomadSim sim, Tile at, Radar radar, boolean outside, Session s, String caption, String name)
    {
        Map<String, String> out = new LinkedHashMap<>();
        Circle c = sim.bound();
        out.put("mood.txt", c.moodName());
        Region r = at == null ? null : Geography.regionAt(at.x, at.y);
        out.put("kingdom.txt", r == null ? "At sea" : r.name);
        out.put("circle.txt", (c.isMain() && name != null ? name : c.name()) + ": " + Math.round(c.radius()) + " tiles");
        String edge;
        if (outside) edge = "OUTSIDE";
        else if (radar != null && radar.edgeIn > 0) edge = "Edge in ~" + radar.edgeIn + " min";
        else edge = "Edge clear";
        out.put("edge.txt", edge);
        Beat latest = s == null ? null : s.latest();
        out.put("chapter.txt", latest == null ? "Cold open" : latest.title);
        out.put("drama.txt", s == null ? "" : "Drama " + Drama.of(s).total);
        out.put("prophecy.txt", s == null || s.prophecy == null ? "" : s.prophecy.line);
        out.put("stake.txt", s == null || s.stake == null ? "" : s.stake.headline);
        out.put("caption.txt", caption == null ? "" : caption);
        out.put("close-calls.txt", s == null ? "" : "Close calls: " + s.count(Beat.NEAR));
        out.put("fork.txt", sim.forkWarning() > 0 ? "Fork in " + sim.forkWarning() + " min" : sim.twin() != null ? "Forked!" : "");
        return out;
    }
}
