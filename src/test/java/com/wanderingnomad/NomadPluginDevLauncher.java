package com.wanderingnomad;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/** Starts RuneLite with Nomad UIM loaded from source: gradle runClient. */
public class NomadPluginDevLauncher
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(NomadPlugin.class);
        RuneLite.main(args);
    }
}
