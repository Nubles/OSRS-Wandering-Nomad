package com.wanderingnomad;

/**
 * The run's tallies for the sidebar: tiles walked, crossings made, waystones claimed and how many
 * times you stepped outside. Plain fields so they save as JSON with the run's other notes.
 */
final class RunStats
{
    /** The most tiles one game tick can count, so a teleport isn't counted as a walk. */
    private static final int STEP_LIMIT = 2;

    int tilesWalked;
    int crossings;
    int claimed;
    int timesOutside;

    /** Counts the walk from one surface tile to the next, by the larger of the two axes. */
    void walked(int dx, int dy)
    {
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps <= STEP_LIMIT) tilesWalked += steps;
    }

    void count(Moments.Event event)
    {
        switch (event)
        {
            case CROSSED:
                crossings++;
                break;
            case CLAIMED:
                claimed++;
                break;
            case LEFT:
                timesOutside++;
                break;
            default:
                break;
        }
    }
}
