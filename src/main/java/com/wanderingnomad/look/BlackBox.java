package com.wanderingnomad.look;

import com.wanderingnomad.sim.Circle;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * The black box: the last three minutes of you and your circle's edge, a frame a game tick, so the
 * sidebar can replay them as a fast top-down animation after a death, or any time. View only.
 */
public final class BlackBox
{
    /** Three minutes of game ticks. */
    public static final int FRAMES = 300;

    public static final class Frame
    {
        public final int x;
        public final int y;
        public final double cx;
        public final double cy;
        public final double r;
        public final boolean inside;
        public final int colour;

        public Frame(int x, int y, Circle c, boolean inside, int colour)
        {
            this.x = x;
            this.y = y;
            this.cx = c.x();
            this.cy = c.y();
            this.r = c.radius();
            this.inside = inside;
            this.colour = colour;
        }
    }

    private final Deque<Frame> frames = new ArrayDeque<>();

    public synchronized void add(Frame f)
    {
        frames.addLast(f);
        while (frames.size() > FRAMES) frames.removeFirst();
    }

    public synchronized List<Frame> frames()
    {
        return new ArrayList<>(frames);
    }

    public synchronized void clear()
    {
        frames.clear();
    }
}
