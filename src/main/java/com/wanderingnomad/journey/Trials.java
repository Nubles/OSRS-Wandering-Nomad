package com.wanderingnomad.journey;

import com.wanderingnomad.sim.Geography;
import com.wanderingnomad.sim.Landmarks;
import com.wanderingnomad.sim.Region;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Every trial of a run, in chapters. The mode's own trials count from the run's start; the
 * account's (levels, quests, diaries) only count when the run is what takes you past the target.
 * Every metric is read from the game or the circles: nothing here asks you to tick a box.
 */
public final class Trials
{
    // Metrics the plugin counts from the circles.
    public static final String WALKED = "walked";
    public static final String CLAIMED = "claimed";
    public static final String BOONS = "boons";
    public static final String CROSSINGS = "crossings";
    public static final String FORKS = "forks";
    public static final String VOYAGES = "voyages";
    public static final String PLAYED = "played";
    public static final String STREAK = "streak";
    /** Distinct sets: region circles you've been inside, and landmarks you've stood at. */
    public static final String REGIONS = "regions";
    public static final String LANDMARKS = "landmarks";

    // Metrics the plugin reads from the game.
    public static final String TOTAL_LEVEL = "total";
    public static final String QUEST_POINTS = "qp";
    public static final String DIARY_TIERS = "diary";
    public static final String MAXED = "maxed";
    public static final String BOSSES = "bosses";
    public static final String COMBAT_TASKS = "combat";
    public static final String GRANDMASTER_TASKS = "combat:grandmaster";
    public static final String PETS = "pets";
    public static final String COLLECTION = "collection";
    /** Every kill count line, repeats included, and omens met. */
    public static final String KILLS = "kills";
    public static final String OMENS = "omens";
    /** Distinct ports you've followed Main into. */
    public static final String HARBOURS = "harbours";
    /** The trials a run can swear as its oath: the long goals a whole run is built around. */
    public static final List<String> OATHS = Collections.unmodifiableList(java.util.Arrays.asList(
        "diary-all", "harbours-all", "landmarks-all", "total-max", "bosses-50", "regions-all"));

    /** The game's six clue tiers, as its "You have completed 1 easy Treasure Trail." line names them. */
    public static final String[] CLUE_TIERS = {"beginner", "easy", "medium", "hard", "elite", "master"};

    public static final String FIRST_STEPS = "First steps";
    public static final String LONG_ROAD = "The long road";
    public static final String FATE = "Forks and fate";
    public static final String KINGDOMS = "Kingdoms";
    public static final String ATLAS = "Atlas";
    public static final String SEAFARER = "Seafarer";
    public static final String DEEDS = "Deeds";
    public static final String TREASURE = "Treasure and glory";
    public static final List<String> CHAPTERS = Collections.unmodifiableList(java.util.Arrays.asList(
        FIRST_STEPS, LONG_ROAD, FATE, KINGDOMS, ATLAS, SEAFARER, DEEDS, TREASURE));

    private Trials()
    {
    }

    /** One region's diary tiers, as a metric. */
    public static String diaryOf(Region region)
    {
        return DIARY_TIERS + ":" + region.id;
    }

    public static String clue(String tier)
    {
        return "clue:" + tier;
    }

    /**
     * The whole list. {@code skills} is how many skills the game has, so the max total level stays
     * right as skills are added (24 with Sailing: 2,376).
     */
    public static List<Trial> all(int skills)
    {
        List<Trial> t = new ArrayList<>();
        int landmarks = Landmarks.ALL.size();
        int regions = Geography.REGIONS.size();
        int diaryTiers = 0;
        for (Region r : Geography.REGIONS) diaryTiers += r.maxTiers();
        int maxTotal = 99 * skills;

        t.add(trial("first-waystone", FIRST_STEPS, "Called by the stones", "Claim your first waystone.", CLAIMED, 1, Tier.BRONZE));
        t.add(trial("first-boon", FIRST_STEPS, "Spend it well", "Spend a boon from the sidebar.", BOONS, 1, Tier.BRONZE));
        t.add(trial("first-crossing", FIRST_STEPS, "Over the line", "Cross into another circle.", CROSSINGS, 1, Tier.BRONZE));
        t.add(trial("first-fork", FIRST_STEPS, "The road divides", "Be inside when a fork's halves part.", FORKS, 1, Tier.BRONZE));
        t.add(trial("first-landmarks", FIRST_STEPS, "Sightseer", "Stand at 5 landmarks.", LANDMARKS, 5, Tier.BRONZE));
        t.add(trial("first-hour", FIRST_STEPS, "Steady feet", "Play an hour without stepping outside.", STREAK, 60, Tier.BRONZE));

        t.add(trial("walk-10k", LONG_ROAD, "Worn boots", "Walk 10,000 tiles.", WALKED, 10_000, Tier.SILVER));
        t.add(trial("walk-100k", LONG_ROAD, "Pilgrim", "Walk 100,000 tiles.", WALKED, 100_000, Tier.GOLD));
        t.add(trial("walk-1m", LONG_ROAD, "A million steps", "Walk 1,000,000 tiles.", WALKED, 1_000_000, Tier.LEGEND));
        t.add(trial("played-24h", LONG_ROAD, "Day one", "Play 24 hours of the run.", PLAYED, 24 * 60, Tier.SILVER));
        t.add(trial("played-100h", LONG_ROAD, "Long haul", "Play 100 hours of the run.", PLAYED, 100 * 60, Tier.GOLD));
        t.add(trial("played-500h", LONG_ROAD, "Lifer", "Play 500 hours of the run.", PLAYED, 500 * 60, Tier.LEGEND));
        t.add(trial("streak-6h", LONG_ROAD, "Unbroken", "Play 6 hours in a row without stepping outside.", STREAK, 6 * 60, Tier.SILVER));
        t.add(trial("streak-24h", LONG_ROAD, "Never strayed", "Play 24 hours in a row without stepping outside.", STREAK, 24 * 60, Tier.GOLD));

        t.add(trial("forks-5", FATE, "Fork in the road", "Be inside when 5 forks part.", FORKS, 5, Tier.SILVER));
        t.add(trial("forks-25", FATE, "Fate's favourite", "Be inside when 25 forks part.", FORKS, 25, Tier.GOLD));
        t.add(trial("forks-100", FATE, "Splitter", "Be inside when 100 forks part.", FORKS, 100, Tier.LEGEND));
        t.add(trial("claimed-10", FATE, "Stonecaller", "Claim 10 waystones.", CLAIMED, 10, Tier.SILVER));
        t.add(trial("claimed-50", FATE, "Waystone keeper", "Claim 50 waystones.", CLAIMED, 50, Tier.GOLD));
        t.add(trial("boons-25", FATE, "Gifted", "Spend 25 boons.", BOONS, 25, Tier.SILVER));
        t.add(trial("omens-1", FATE, "Read the signs", "Meet an omen before its time runs out.", OMENS, 1, Tier.BRONZE));
        t.add(trial("omens-10", FATE, "Augur", "Meet 10 omens.", OMENS, 10, Tier.SILVER));
        t.add(trial("omens-50", FATE, "Oracle", "Meet 50 omens.", OMENS, 50, Tier.GOLD));
        t.add(trial("crossings-25", FATE, "Border hopper", "Cross between circles 25 times.", CROSSINGS, 25, Tier.SILVER));

        t.add(trial("regions-3", KINGDOMS, "Three crowns", "Be inside 3 different region circles.", REGIONS, 3, Tier.BRONZE));
        t.add(trial("regions-6", KINGDOMS, "Statesman", "Be inside 6 different region circles.", REGIONS, 6, Tier.SILVER));
        t.add(trial("regions-all", KINGDOMS, "Every banner", "Be inside all " + regions + " region circles.", REGIONS, regions, Tier.GOLD));
        for (Region r : Geography.REGIONS)
        {
            if (r.diaries.isEmpty()) continue;
            String which = r.diaries.size() == 1 ? "the " + diaryName(r.diaries.get(0)) + " Diary"
                : "the " + diaryName(r.diaries.get(0)) + " and " + diaryName(r.diaries.get(1)) + " Diaries";
            t.add(new Trial("bloom-" + r.id, KINGDOMS, r.name + " in full bloom",
                "Finish every tier of " + which + " during the run, growing its circle to full.",
                diaryOf(r), r.maxTiers(), Tier.GOLD, true));
        }

        t.add(trial("landmarks-25", ATLAS, "Wayfinder", "Stand at 25 landmarks.", LANDMARKS, 25, Tier.SILVER));
        t.add(trial("landmarks-50", ATLAS, "Surveyor", "Stand at 50 landmarks.", LANDMARKS, 50, Tier.GOLD));
        t.add(trial("landmarks-all", ATLAS, "Atlas complete", "Stand at all " + landmarks + " landmarks.", LANDMARKS, landmarks, Tier.LEGEND));

        t.add(trial("voyage-1", SEAFARER, "Cast off", "Follow Main across the sea: be back inside within an hour of it docking.", VOYAGES, 1, Tier.BRONZE));
        t.add(trial("voyage-10", SEAFARER, "Sea legs", "Follow Main across the sea 10 times.", VOYAGES, 10, Tier.SILVER));
        t.add(trial("harbours-all", SEAFARER, "Round the world", "Follow Main into all " + com.wanderingnomad.sim.Geography.PORTS.size() + " ports.",
            HARBOURS, com.wanderingnomad.sim.Geography.PORTS.size(), Tier.LEGEND));
        t.add(trial("voyage-50", SEAFARER, "Old salt", "Follow Main across the sea 50 times.", VOYAGES, 50, Tier.GOLD));

        t.add(account("total-500", "Apprentice", "Reach 500 total level.", TOTAL_LEVEL, 500, Tier.BRONZE));
        t.add(account("total-1000", "Journeyman", "Reach 1,000 total level.", TOTAL_LEVEL, 1000, Tier.SILVER));
        t.add(account("total-1500", "Adept", "Reach 1,500 total level.", TOTAL_LEVEL, 1500, Tier.GOLD));
        t.add(account("total-2000", "Master of the road", "Reach 2,000 total level.", TOTAL_LEVEL, 2000, Tier.GOLD));
        t.add(account("total-max", "Maxed nomad", "Reach " + String.format("%,d", maxTotal) + " total level: 99 in every skill.",
            TOTAL_LEVEL, maxTotal, Tier.LEGEND));
        t.add(account("maxed-1", "Ninety-nine", "Reach level 99 in a skill.", MAXED, 1, Tier.GOLD));
        t.add(account("qp-50", "Errand runner", "Reach 50 quest points.", QUEST_POINTS, 50, Tier.BRONZE));
        t.add(account("qp-100", "Adventurer", "Reach 100 quest points.", QUEST_POINTS, 100, Tier.SILVER));
        t.add(account("qp-200", "Hero of Gielinor", "Reach 200 quest points.", QUEST_POINTS, 200, Tier.GOLD));
        t.add(account("diary-12", "Diarist", "Finish 12 Achievement Diary tiers.", DIARY_TIERS, 12, Tier.SILVER));
        t.add(account("diary-24", "Chronicler", "Finish 24 Achievement Diary tiers.", DIARY_TIERS, 24, Tier.GOLD));
        t.add(account("diary-all", "Every page written", "Finish all " + diaryTiers + " Achievement Diary tiers, as the Achievement Diary cape asks.",
            DIARY_TIERS, diaryTiers, Tier.LEGEND));

        t.add(trial("bosses-10", TREASURE, "Giant slayer", "Get a kill count on 10 different bosses.", BOSSES, 10, Tier.SILVER));
        t.add(trial("bosses-25", TREASURE, "Monster hunter", "Get a kill count on 25 different bosses.", BOSSES, 25, Tier.GOLD));
        t.add(trial("bosses-50", TREASURE, "Bane of bosses", "Get a kill count on 50 different bosses.", BOSSES, 50, Tier.LEGEND));
        Tier[] clueTier = {Tier.BRONZE, Tier.BRONZE, Tier.SILVER, Tier.SILVER, Tier.GOLD, Tier.GOLD};
        for (int i = 0; i < CLUE_TIERS.length; i++)
        {
            String tier = CLUE_TIERS[i];
            String title = Character.toUpperCase(tier.charAt(0)) + tier.substring(1);
            t.add(trial("clue-" + tier, TREASURE, title + " treasure", "Complete " + (i == 1 || i == 4 ? "an " : "a ") + tier + " Treasure Trail.",
                clue(tier), 1, clueTier[i]));
        }
        t.add(trial("combat-1", TREASURE, "First task", "Complete a combat task.", COMBAT_TASKS, 1, Tier.BRONZE));
        t.add(trial("combat-50", TREASURE, "Veteran", "Complete 50 combat tasks.", COMBAT_TASKS, 50, Tier.SILVER));
        t.add(trial("combat-200", TREASURE, "Warlord", "Complete 200 combat tasks.", COMBAT_TASKS, 200, Tier.GOLD));
        t.add(trial("combat-gm", TREASURE, "Grandmaster", "Complete a grandmaster combat task.", GRANDMASTER_TASKS, 1, Tier.LEGEND));
        t.add(trial("pet", TREASURE, "A friend on the road", "Get a pet.", PETS, 1, Tier.GOLD));
        t.add(trial("collection-50", TREASURE, "Collector", "Add 50 new items to your collection log (needs the game's collection log chat message on).",
            COLLECTION, 50, Tier.SILVER));
        t.add(trial("collection-250", TREASURE, "Hoarder of nothing", "Add 250 new items to your collection log.", COLLECTION, 250, Tier.GOLD));
        return Collections.unmodifiableList(t);
    }

    /** "Lumbridge" to "Lumbridge & Draynor": the diaries' names as the game shows them. */
    static String diaryName(String diary)
    {
        switch (diary)
        {
            case "Lumbridge":
                return "Lumbridge & Draynor";
            case "Kourend":
                return "Kourend & Kebos";
            case "Western":
                return "Western Provinces";
            default:
                return diary;
        }
    }

    private static Trial trial(String id, String chapter, String name, String detail, String metric, int target, Tier tier)
    {
        return new Trial(id, chapter, name, detail, metric, target, tier, false);
    }

    private static Trial account(String id, String name, String detail, String metric, int target, Tier tier)
    {
        return new Trial(id, DEEDS, name, detail, metric, target, tier, true);
    }
}
