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
    /** Distinct rivals you've met inside each other's circles. */
    public static final String RENDEZVOUS = "rendezvous";
    /** The game's 64-tile map regions you've walked in while inside your circle: the run's footprint. */
    public static final String SQUARES = "squares";
    /** Comebacks: back inside after Main shrank below 15 tiles, into a fork's half in its last 30 seconds, a stone claimed with a minute left. */
    public static final String COMEBACK_SHRUNK = "comeback:shrunk";
    public static final String COMEBACK_FORK = "comeback:fork";
    public static final String COMEBACK_STONE = "comeback:stone";
    public static final String COMEBACKS = "comebacks";
    /** Weathered: storms over Main this run, stones claimed in a Gale, forks parted in a Squall, kingdoms a storm broke over. */
    public static final String STORMS = "storms";
    public static final String GALE_CLAIMS = "storm:galeclaim";
    public static final String SQUALL_FORKS = "storm:squallfork";
    public static final String STORM_KINGDOMS = "storm:kingdoms";
    /** Ancestor ghosts whose ring you've stood in, by seed; wagers won; prophecies that came true. */
    public static final String ANCESTORS = "ancestors";
    public static final String WAGERS = "wagers";
    public static final String PROPHECIES = "prophecies";
    /** Secret trials' metrics. */
    public static final String SECRET_STILL = "secret:still";
    public static final String SECRET_TRIPLE = "secret:triple";
    public static final String SECRET_EYE = "secret:eye";
    public static final String SECRET_SEED = "secret:seed";
    /** Minutes with XP gained within 2 tiles of the edge, inside (the Edgewalker omen), and monster species killed inside. */
    public static final String EDGE_XP = "edgexp";
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
    public static final String FELLOWS = "Fellow Nomads";
    public static final String COMEBACK = "Comebacks";
    public static final String WEATHERED = "Weathered";
    public static final String SECRETS = "Secrets";
    public static final List<String> CHAPTERS = Collections.unmodifiableList(java.util.Arrays.asList(
        FIRST_STEPS, LONG_ROAD, FATE, KINGDOMS, ATLAS, SEAFARER, COMEBACK, WEATHERED, FELLOWS, DEEDS, TREASURE, SECRETS));

    private Trials()
    {
    }

    /** The landmark a run's seed keeps for its secret trial. */
    public static Landmarks.Landmark seedLandmark(int seed)
    {
        return Landmarks.ALL.get(Math.floorMod(seed * 31 + 7, Landmarks.ALL.size()));
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
        t.add(trial("rendezvous-1", FELLOWS, "Rendezvous", "Stand inside a rival's circle while they stand inside yours.",
            RENDEZVOUS, 1, Tier.SILVER));
        t.add(trial("rendezvous-3", FELLOWS, "Caravan", "Meet 3 different rivals inside each other's circles.", RENDEZVOUS, 3, Tier.GOLD));
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

        t.add(trial("squares-25", ATLAS, "Footloose", "Walk inside your circle in 25 of the map's 64-tile regions.", SQUARES, 25, Tier.BRONZE));
        t.add(trial("squares-100", ATLAS, "Wide world", "Walk inside your circle in 100 map regions.", SQUARES, 100, Tier.SILVER));
        t.add(trial("squares-300", ATLAS, "Mapmaker", "Walk inside your circle in 300 map regions.", SQUARES, 300, Tier.GOLD));
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
        t.add(trial("comeback-shrunk", COMEBACK, "Back from the brink", "Get back inside your circle while it's under 15 tiles from middle to edge.",
            COMEBACK_SHRUNK, 1, Tier.SILVER));
        t.add(trial("comeback-fork", COMEBACK, "Photo finish", "Step into the half you keep in the last 30 seconds before a fork parts.",
            COMEBACK_FORK, 1, Tier.SILVER));
        t.add(trial("comeback-stone", COMEBACK, "Last breath", "Claim a waystone with under a minute left before it fades.", COMEBACK_STONE, 1,
            Tier.SILVER));
        t.add(trial("comebacks-10", COMEBACK, "Unbowed", "Make 10 comebacks of any kind.", COMEBACKS, 10, Tier.GOLD));

        t.add(trial("storms-10", WEATHERED, "Weathered", "See 10 storms over Main.", STORMS, 10, Tier.BRONZE));
        t.add(trial("storms-25", WEATHERED, "Storm-worn", "See 25 storms over Main.", STORMS, 25, Tier.SILVER));
        t.add(trial("storms-50", WEATHERED, "Eye of every storm", "See 50 storms over Main.", STORMS, 50, Tier.GOLD));
        t.add(trial("storm-gale-claim", WEATHERED, "Gale-taken", "Claim a waystone while a Gale drives Main.", GALE_CLAIMS, 1, Tier.SILVER));
        t.add(trial("storm-squall-fork", WEATHERED, "Split by the squall", "Be inside when a fork parts during a Squall.", SQUALL_FORKS, 1,
            Tier.SILVER));
        t.add(trial("storm-kingdoms", WEATHERED, "Every sky", "See a storm break over Main in all " + regions + " kingdoms.", STORM_KINGDOMS,
            regions, Tier.LEGEND));

        t.add(trial("ancestor-1", FATE, "Ancestor's footsteps", "Stand inside the ghost of one of your past runs' Main circles.", ANCESTORS, 1,
            Tier.SILVER));
        t.add(trial("wager-3", FATE, "Called it", "Win 3 fork wagers: call the half you'll keep before it parts.", WAGERS, 3, Tier.SILVER));
        t.add(trial("prophecy-3", FATE, "Seer", "Have all three of the run's prophecies come true.", PROPHECIES, 3, Tier.LEGEND));

        t.add(secret("secret-still", "The still point", "Stand within a tile of your circle's middle for 10 played minutes in a row.",
            SECRET_STILL, 10, "Where the circle turns, be still."));
        t.add(secret("secret-triple", "Where three roads meet", "Stand where three circles overlap.", SECRET_TRIPLE, 1,
            "Three rings, one tile."));
        t.add(secret("secret-eye", "The eye of the storm", "Stand within 3 tiles of Main's middle while a storm rages over it.", SECRET_EYE, 1,
            "Calm at the heart of the gale."));
        t.add(secret("secret-seed", "The seed remembers", "Stand at the landmark your run's seed keeps, inside your circle.", SECRET_SEED, 1,
            "Your seed remembers a place. Its number knows the way."));

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

    private static Trial secret(String id, String name, String detail, String metric, int target, String riddle)
    {
        return new Trial(id, SECRETS, name, detail, metric, target, Tier.GOLD, false, riddle);
    }

    private static Trial account(String id, String name, String detail, String metric, int target, Tier tier)
    {
        return new Trial(id, DEEDS, name, detail, metric, target, tier, true);
    }
}
