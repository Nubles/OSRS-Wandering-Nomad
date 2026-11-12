package com.wanderingnomad.journey;

/**
 * One goal of the run: reach {@link #target} on a {@link #metric}. A trial on the account's own
 * totals (total level, quest points, diary tiers, 99s) is {@link #baseline}: it only counts if the
 * run started below the target, so an old account can't collect it just by logging in.
 */
public final class Trial
{
    public final String id;
    public final String chapter;
    public final String name;
    public final String detail;
    public final String metric;
    public final int target;
    public final Tier tier;
    public final boolean baseline;
    /** A secret trial's riddle: until it's done the sidebar shows "???" and this instead of its name and detail. Null if not secret. */
    public final String riddle;

    Trial(String id, String chapter, String name, String detail, String metric, int target, Tier tier, boolean baseline)
    {
        this(id, chapter, name, detail, metric, target, tier, baseline, null);
    }

    Trial(String id, String chapter, String name, String detail, String metric, int target, Tier tier, boolean baseline, String riddle)
    {
        this.riddle = riddle;
        this.id = id;
        this.chapter = chapter;
        this.name = name;
        this.detail = detail;
        this.metric = metric;
        this.target = target;
        this.tier = tier;
        this.baseline = baseline;
    }
}
