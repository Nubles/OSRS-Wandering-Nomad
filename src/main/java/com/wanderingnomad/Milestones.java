package com.wanderingnomad;

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

    /** -1 until the account's totals are first seen. */
    int levelMark = -1;
    int questMark = -1;

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
