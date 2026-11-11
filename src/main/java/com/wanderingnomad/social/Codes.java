package com.wanderingnomad.social;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * The codes Nomads hand each other with no server: a kind's prefix, then JSON gzipped and
 * base64url-encoded, like a run code. Bottles, dares, torches, colours, footprints, batons, and a stream's votes and flags.
 */
public final class Codes
{
    public static final String BOTTLE = "NB1.";
    public static final String DARE = "ND1.";
    public static final String TORCH = "NT1.";
    public static final String COLOURS = "NC1.";
    public static final String FOOTPRINT = "NF1.";
    public static final String BATON = "NR1.";
    /** Streaming: chat's votes on how the episode looks, and viewers' flags. */
    public static final String VOTES = "NW1.";
    public static final String FLAGS = "NV1.";
    /** The most JSON a pasted code may unpack to. */
    public static final int MAX_BYTES = 512 * 1024;

    private Codes()
    {
    }

    public static String encode(String kind, String json)
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(bytes))
        {
            gz.write(json.getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
        return kind + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
    }

    /** The JSON inside a code of this kind, or null if the text isn't one (spaces around it are fine). */
    public static String decode(String kind, String code)
    {
        if (code == null) return null;
        String text = code.trim();
        if (!text.startsWith(kind)) return null;
        try
        {
            byte[] gz = Base64.getUrlDecoder().decode(text.substring(kind.length()));
            try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(gz)))
            {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                for (int n; (n = in.read(buf)) > 0; )
                {
                    out.write(buf, 0, n);
                    if (out.size() > MAX_BYTES) return null;
                }
                return new String(out.toByteArray(), StandardCharsets.UTF_8);
            }
        }
        catch (IOException | IllegalArgumentException e)
        {
            return null;
        }
    }

    /** A name or note from someone else, made safe to show: no tags, no line breaks, at most {@code max} characters. */
    public static String clean(String text, int max)
    {
        if (text == null) return "";
        String out = text.replaceAll("<[^>]*>", "").replaceAll("[\\p{Cntrl}]", " ").replaceAll("\\s+", " ").trim();
        return out.length() > max ? out.substring(0, max) : out;
    }
}
