package com.wanderingnomad.sim;

/** What the plugin saves per account: the run, and the last surface tile you stood on. */
public final class SavedRun
{
    public NomadSim sim;
    public Tile surface;
    /** Delve (a house rule): minutes left counting as inside while underground, and minutes until it works again. */
    public int delveLeft;
    public int delveWait;

    public SavedRun()
    {
    }

    public SavedRun(NomadSim sim, Tile surface)
    {
        this.sim = sim;
        this.surface = surface;
    }
}
