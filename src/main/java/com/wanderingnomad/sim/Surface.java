package com.wanderingnomad.sim;

/**
 * The Below law: underground, upstairs or in an instance, you count as standing on the surface
 * tile where you went in. Upstairs keeps its own x and y, so only the y band tells surface from
 * underground: the overworld sits below y 4200 and the dungeons above it.
 */
public final class Surface
{
    static final int SURFACE_MAX_Y = 4200;

    private Surface()
    {
    }

    public static boolean isSurface(int x, int y)
    {
        return x > 0 && y > 0 && y < SURFACE_MAX_Y;
    }
}
