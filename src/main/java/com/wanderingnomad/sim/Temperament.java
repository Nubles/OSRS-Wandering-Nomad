package com.wanderingnomad.sim;

/**
 * How a circle likes to move. Every circle switches between moods (roam, meander, loop, dash,
 * linger, drift), and its temperament sets how often it picks each one, how long they last and
 * how fast it goes, so each kingdom feels different. Mirrors TEMPERAMENTS in sim.ts.
 */
final class Temperament
{
    static final int ROAM = 0;
    static final int MEANDER = 1;
    static final int LOOP = 2;
    static final int DASH = 3;
    static final int LINGER = 4;
    static final int DRIFT = 5;
    static final int MOODS = 6;

    final String name;
    /** Multiplies the circle's drift speed. */
    final double speed;
    /** How likely each mood is to come next, by mood index. */
    final double[] weights;
    /** Multiplies how long each mood lasts. */
    final double duration;
    /** Multiplies the chance of a pulse each minute. */
    final double pulses;
    /** Tiles a pulse grows or shrinks the circle by. */
    final int pulse;
    /** Multiplies how often the circle picks a new size to ease toward. */
    final double breathing;
    /** New sizes lean toward the small end of the range. */
    final boolean hungry;

    private Temperament(String name, double speed, double[] weights, double duration, double pulses, int pulse,
        double breathing, boolean hungry)
    {
        this.name = name;
        this.speed = speed;
        this.weights = weights;
        this.duration = duration;
        this.pulses = pulses;
        this.pulse = pulse;
        this.breathing = breathing;
        this.hungry = hungry;
    }

    private static Temperament of(String name, double speed, double duration, double... weights)
    {
        return new Temperament(name, speed, weights, duration, 1, 8, 1, false);
    }

    static final Temperament MAIN = of("Restless", 1, 1, 4, 2, 1, 1.5, 0.7, 1);

    private static final java.util.Map<String, Temperament> BY_REGION = new java.util.concurrent.ConcurrentHashMap<>();

    /** By region id; the Main circle's for null. */
    static Temperament of(String regionId)
    {
        if (regionId == null) return MAIN;
        return BY_REGION.computeIfAbsent(regionId, Temperament::create);
    }

    private static Temperament create(String regionId)
    {
        switch (regionId)
        {
            case "misthalin":
                return of("Gentle", 0.7, 1.4, 3, 2, 0.5, 0.3, 1.5, 2);
            case "asgarnia":
                return of("Dutiful", 1, 1, 2, 1, 4, 0.5, 0.5, 1);
            case "kandarin":
                return of("Curious", 1, 0.6, 2, 3, 1, 1, 0.5, 1);
            case "fremennik":
                return new Temperament("Stubborn", 0.5, new double[] {3, 1, 1, 0.3, 2, 1}, 1.5, 1, 12, 1, false);
            case "desert":
                // Only dashes and rests, and a mood never repeats, so it alternates between them.
                return of("Sandstorm", 1, 1, 0, 0, 0, 4, 4, 0);
            case "morytania":
                return new Temperament("Hungry", 1, new double[] {2, 2, 1, 1, 1, 1}, 1, 2, 8, 1, true);
            case "wilderness":
                return of("Wild", 1.5, 0.7, 2, 1, 1, 3, 0.3, 1);
            case "karamja":
                return new Temperament("Tidal", 1, new double[] {1, 2, 2, 1, 1, 1}, 1, 1, 8, 3, false);
            case "kourend":
                return of("Stately", 0.8, 1.5, 4, 1, 2, 0.5, 1, 1);
            case "tirannwn":
                return of("Elusive", 1.1, 0.6, 1, 4, 1, 1, 0.5, 1);
            case "varlamore":
                return of("Proud", 1, 1, 2, 1, 3, 2, 0.5, 0.5);
            default:
                return MAIN;
        }
    }
}
