package com.wanderingnomad.sim;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Coordinate tide: reads a coordinate clue's text into the tile it asks you to dig on. */
public final class Clues
{
    /** "00 degrees 05 minutes north 01 degree 13 minutes east", after tags and line breaks are gone. */
    private static final Pattern COORDINATES = Pattern.compile(
        "(\\d+) degrees? (\\d+) minutes? (north|south),? (\\d+) degrees? (\\d+) minutes? (east|west)", Pattern.CASE_INSENSITIVE);

    private Clues()
    {
    }

    /**
     * The dig spot a coordinate clue names, or null for any other clue. Each degree is 32 tiles and
     * each minute 32/60 of one, from the middle of the Observatory, as the game counts them.
     */
    public static Tile digSpot(String text)
    {
        if (text == null) return null;
        Matcher m = COORDINATES.matcher(text.replaceAll("(?i)<br\\s*/?>", " ").replaceAll("<[^>]*>", "").replaceAll("\\s+", " "));
        if (!m.find()) return null;
        int degY = Integer.parseInt(m.group(1));
        int minY = Integer.parseInt(m.group(2));
        int degX = Integer.parseInt(m.group(4));
        int minX = Integer.parseInt(m.group(5));
        int sy = "south".equalsIgnoreCase(m.group(3)) ? -1 : 1;
        int sx = "west".equalsIgnoreCase(m.group(6)) ? -1 : 1;
        int x = 2440 + sx * (degX * 32 + (int) Math.round(minX / 1.875));
        int y = 3161 + sy * (degY * 32 + (int) Math.round(minY / 1.875));
        return new Tile(x, y);
    }
}
