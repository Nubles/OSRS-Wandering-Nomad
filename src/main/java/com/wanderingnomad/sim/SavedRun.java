package com.wanderingnomad.sim;

/** What the plugin saves per account: the run, and the last surface tile you stood on. */
public final class SavedRun
{
    public NomadSim sim;
    public Tile surface;
    /** Delve (a house rule): minutes left counting as inside while underground, and minutes until it works again. */
    public int delveLeft;
    public int delveWait;
    /** Second wind (Hardcore): the one death the run is allowed has happened. */
    public boolean fallen;
    /** Plot circles (a house rule): the farming patches that hold your circle, as x, y, minutes-left triples. */
    public int[] plots;
    /** The run was laid down: its circles stand still and its epilogue is written. */
    public boolean retired;
    /** The run's epithet so far, such as "the Seaborn", for rivals to read; null if none yet. */
    public String epithet;

    public SavedRun()
    {
    }

    public SavedRun(NomadSim sim, Tile surface)
    {
        this.sim = sim;
        this.surface = surface;
    }
}
