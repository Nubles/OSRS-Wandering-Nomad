package com.wanderingnomad;

import java.util.ArrayList;
import java.util.List;

/**
 * The milestones that summon waystones, as the design doc lists them: every 100 total level and
 * every 25 quest points summon a common boon's waystone, and the first kill of any boss a rare
 * one. The marks are how many of each the run has already paid out, saved per account; a run
 * starts them at the account's current totals, so only progress made during the run counts.
 */
final class Milestones
{
    static final int LEVELS = 100;
    static final int QUEST_POINTS = 25;
    /** The total level whose reaching is a legendary feat. */
    static final int LEGEND_TOTAL = 1500;

    /** -1 until the account's totals are first seen. */
    int levelMark = -1;
    int questMark = -1;
    /** 99s and Elite diaries the account had when the run first saw it; -1 until then. */
    int nines = -1;
    int elites = -1;
    /** Which legendary feats have summoned their waystone this run (or were done before it began). */
    boolean ninePaid;
    boolean elitePaid;
    boolean totalPaid;

    /**
     * The legendary feats the account's new state reaches, each summoning a legendary waystone
     * once a run: its first new 99, its first new Elite diary, and reaching 1,500 total level.
     * The first look only takes stock, so progress from before the run never counts.
     */
    List<String> legendary(int maxed, int eliteDiaries, int totalLevel)
    {
        List<String> out = new ArrayList<>();
        if (totalLevel <= 0) return out;
        if (nines < 0 || elites < 0)
        {
            nines = maxed;
            elites = eliteDiaries;
            totalPaid = totalLevel >= LEGEND_TOTAL;
            return out;
        }
        if (!ninePaid && maxed > nines)
        {
            ninePaid = true;
            out.add("Your first 99 of the run");
        }
        if (!elitePaid && eliteDiaries > elites)
        {
            elitePaid = true;
            out.add("Your first Elite diary of the run");
        }
        if (!totalPaid && totalLevel >= LEGEND_TOTAL)
        {
            totalPaid = true;
            out.add("Reaching 1,500 total level");
        }
        return out;
    }

    /** How many common waystones the new totals earn, updating the marks. */
    int earned(int totalLevel, int questPoints)
    {
        if (totalLevel <= 0) return 0;
        int level = totalLevel / LEVELS;
        int quest = Math.max(0, questPoints) / QUEST_POINTS;
        if (levelMark < 0 || questMark < 0)
        {
            levelMark = level;
            questMark = quest;
            return 0;
        }
        int earned = Math.max(0, level - levelMark) + Math.max(0, quest - questMark);
        levelMark = Math.max(levelMark, level);
        questMark = Math.max(questMark, quest);
        return earned;
    }
}
