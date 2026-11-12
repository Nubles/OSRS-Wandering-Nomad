package com.wanderingnomad.look;

/**
 * How the 3D edge wall moves: a wave of light by default, or one shaped by your circle's mood
 * (quick streaks when it dashes, a slow swell when it lingers, a shimmer when it meanders), or
 * still. Each panel's brightness is 0 to 1; the wall rounds it to one of a few prebuilt models.
 */
public final class Motion
{
    public static final String MOOD = "mood";
    public static final String WAVE = "wave";
    public static final String STILL = "still";

    private Motion()
    {
    }

    /**
     * A panel's brightness. {@code along} is its distance round the edge in tiles, {@code index}
     * its place in the wall, {@code t} the time in seconds. A gateway always runs the quick wave.
     */
    public static double band(String motion, String mood, double along, int index, double t, boolean gate)
    {
        if (STILL.equals(motion)) return 0.3;
        if (gate) return wave(along, t, 3.2);
        if (!MOOD.equals(motion) || mood == null) return wave(along, t, 1.8);
        switch (mood)
        {
            case "Dashing":
                // Short bright streaks racing along the wall.
                return Math.pow(0.5 + 0.5 * Math.sin(along * 0.45 - t * 6.5), 8);
            case "Lingering":
                // The whole wall breathes slowly, as one.
                return 0.5 + 0.5 * Math.sin(t * 0.7);
            case "Meandering":
                // A shimmer: each panel flickers on its own, a few times a second.
                return ForkTear.hash(index * 131 + (int) Math.floor(t * 5)) * 0.8;
            case "Looping":
                return wave(along * 0.35, t, 2.6);
            case "Drifting":
                return wave(along, t, 0.8);
            default:
                return wave(along, t, 1.8);
        }
    }

    private static double wave(double along, double t, double speed)
    {
        return 0.5 + 0.5 * Math.sin(along - t * speed);
    }

    /**
     * How brightly the fork tear lights a stretch of Main's wall at angle {@code angle}, where the
     * tear meets the edge at {@code tear} and {@code tear + pi}: 0 away from it, up to 1 on it,
     * growing with the tear's {@code progress}.
     */
    public static double tear(double angle, double tear, double progress, double t)
    {
        double d = Math.min(near(angle, tear), near(angle, tear + Math.PI));
        if (d > 0.2) return 0;
        double flicker = 0.75 + 0.25 * Math.sin(t * 14);
        return (1 - d / 0.2) * Math.max(0.3, progress) * flicker;
    }

    private static double near(double a, double b)
    {
        double d = Math.abs(a - b) % (Math.PI * 2);
        return d > Math.PI ? Math.PI * 2 - d : d;
    }
}
