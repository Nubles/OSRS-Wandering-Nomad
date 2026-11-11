package com.wanderingnomad.sim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Rings and roots: the travel networks Main can hop the way it sails, once you've unlocked them.
 * Curated once, a handful of well-known stops per network, every one on open land. Mirrors
 * networks.ts.
 */
public final class Networks
{
    public static final int FAIRY_RING = 0;
    public static final int SPIRIT_TREE = 1;
    public static final int GLIDER = 2;
    public static final int BALLOON = 3;
    public static final int CARPET = 4;
    public static final int CANOE = 5;
    public static final int QUETZAL = 6;
    public static final String[] NAMES = {"fairy ring", "spirit tree", "gnome glider", "balloon", "magic carpet", "canoe", "quetzal"};
    /** Wilderness obelisks: stepping through one carries the Wilderness circle to where you land. */
    public static final List<Tile> OBELISKS = Collections.unmodifiableList(Arrays.asList(
        new Tile(3156, 3620), new Tile(3227, 3667), new Tile(3035, 3732),
        new Tile(3106, 3794), new Tile(2980, 3866), new Tile(3307, 3916)));

    public static final class Node
    {
        public final int network;
        public final String name;
        public final int x;
        public final int y;

        Node(int network, String name, int x, int y)
        {
            this.network = network;
            this.name = name;
            this.x = x;
            this.y = y;
        }
    }

    public static final List<Node> NODES = Collections.unmodifiableList(Arrays.asList(
        new Node(FAIRY_RING, "Mudskipper Point (AIQ)", 2996, 3114),
        new Node(FAIRY_RING, "the Wizards' Tower (DIS)", 3108, 3149),
        new Node(FAIRY_RING, "Edgeville (DKR)", 3129, 3496),
        new Node(FAIRY_RING, "Canifis (CKS)", 3447, 3470),
        new Node(FAIRY_RING, "Mort Myre (BKR)", 3469, 3431),
        new Node(FAIRY_RING, "the Legends' Guild (BLR)", 2740, 3351),
        new Node(FAIRY_RING, "McGrubor's Wood (ALS)", 2644, 3495),
        new Node(FAIRY_RING, "Piscatoris (AKQ)", 2319, 3619),
        new Node(FAIRY_RING, "Castle Wars (BKP)", 2385, 3035),
        new Node(FAIRY_RING, "south Tai Bwo Wannai (CKR)", 2801, 3003),
        new Node(FAIRY_RING, "the Feldip Hills (AKS)", 2571, 2956),
        new Node(FAIRY_RING, "Nardah (DLQ)", 3423, 3016),
        new Node(FAIRY_RING, "Sinclair Mansion (CJR)", 2705, 3576),
        new Node(SPIRIT_TREE, "Tree Gnome Village", 2542, 3170),
        new Node(SPIRIT_TREE, "the Gnome Stronghold", 2461, 3444),
        new Node(SPIRIT_TREE, "the Battlefield of Khazard", 2555, 3259),
        new Node(SPIRIT_TREE, "the Grand Exchange", 3185, 3508),
        new Node(SPIRIT_TREE, "the Feldip Hills", 2488, 2850),
        new Node(SPIRIT_TREE, "the Poison Waste", 2339, 3109),
        new Node(GLIDER, "Ta Quir Priw (Gnome Stronghold)", 2465, 3501),
        new Node(GLIDER, "Sindarpos (White Wolf Mountain)", 2848, 3497),
        new Node(GLIDER, "Lemanto Andra (Digsite)", 3321, 3427),
        new Node(GLIDER, "Kar-Hewo (Al Kharid)", 3284, 3213),
        new Node(GLIDER, "Gandius (Karamja)", 2971, 2969),
        new Node(GLIDER, "Lemantolly Undri (Feldip Hills)", 2544, 2970),
        new Node(BALLOON, "Entrana", 2809, 3356),
        new Node(BALLOON, "Taverley", 2936, 3420),
        new Node(BALLOON, "the Crafting Guild", 2924, 3302),
        new Node(BALLOON, "Varrock", 3298, 3481),
        new Node(BALLOON, "Castle Wars", 2462, 3108),
        new Node(BALLOON, "the Grand Tree", 2480, 3458),
        new Node(CARPET, "the Shantay Pass", 3311, 3109),
        new Node(CARPET, "Pollnivneach", 3349, 3002),
        new Node(CARPET, "Uzer", 3469, 3113),
        new Node(CARPET, "the Bedabin Camp", 3181, 3043),
        new Node(CARPET, "Nardah", 3401, 2916),
        new Node(CARPET, "Sophanem", 3285, 2813),
        new Node(CANOE, "Lumbridge", 3241, 3235),
        new Node(CANOE, "the Champions' Guild", 3202, 3343),
        new Node(CANOE, "Barbarian Village", 3111, 3409),
        new Node(CANOE, "Edgeville", 3132, 3510),
        new Node(QUETZAL, "Civitas illa Fortis", 1697, 3140),
        new Node(QUETZAL, "the Hunter Guild", 1585, 3053),
        new Node(QUETZAL, "the Sunset Coast", 1548, 2995),
        new Node(QUETZAL, "Quetzacalli Gorge", 1510, 3222),
        new Node(QUETZAL, "Aldarin", 1389, 2901),
        new Node(QUETZAL, "Cam Torum", 1446, 3108)));

    private Networks()
    {
    }

    /** The stops of one network. */
    static List<Node> of(int network)
    {
        List<Node> out = new ArrayList<>();
        for (Node n : NODES)
        {
            if (n.network == network) out.add(n);
        }
        return out;
    }

    public static Node byName(String name)
    {
        for (Node n : NODES)
        {
            if (n.name.equals(name)) return n;
        }
        return null;
    }

    /** The obelisk within {@code reach} tiles of a tile, or null. */
    public static Tile obeliskNear(double x, double y, double reach)
    {
        for (Tile o : OBELISKS)
        {
            if (Circle.dist(o.x - x, o.y - y) <= reach) return o;
        }
        return null;
    }
}
