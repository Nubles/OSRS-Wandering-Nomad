package com.wanderingnomad;

import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Radar;
import com.wanderingnomad.sim.Tile;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/** Draws the sidebar with a sample run into a PNG, to look at its layout without the game: run its main. */
public class SidebarSnapshot
{
    public static void main(String[] args) throws Exception
    {
        File out = new File(args.length > 0 ? args[0] : "build/sidebar.png");
        SwingUtilities.invokeAndWait(() ->
        {
            try
            {
                NomadPanel panel = new NomadPanel(i -> { }, (i, s) -> { }, () -> { }, () -> { }, () -> { }, () -> { });
                NomadSim sim = NomadSim.start(483574);
                for (int m = 0; m < 30; m++) sim.step(new Tile(3222, 3218));
                sim.summon(NomadSim.COMMON);
                Tile at = new Tile(3222, 3218);
                List<NomadPanel.Moment> moments = new ArrayList<>();
                int minute = 0;
                for (Moments.Banner b : Moments.samples(sim)) moments.add(0, new NomadPanel.Moment(minute += 3, b));
                RunStats stats = new RunStats();
                stats.tilesWalked = 1234;
                stats.crossings = 2;
                panel.display(NomadPanel.View.of(sim, at, Collections.singletonList("A sample log line"), Radar.of(sim, at),
                    null, null, stats, moments, false));
                JFrame frame = new JFrame();
                JScrollPane scroll = new JScrollPane(panel);
                scroll.setPreferredSize(new Dimension(NomadPanel.PANEL_WIDTH + 20, 1400));
                frame.add(scroll);
                frame.pack();
                frame.validate();
                panel.setSize(panel.getPreferredSize());
                panel.validate();
                BufferedImage img = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
                panel.printAll(img.getGraphics());
                ImageIO.write(img, "png", out);
                frame.dispose();
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });
        System.out.println("Wrote " + out.getAbsolutePath());
    }
}
