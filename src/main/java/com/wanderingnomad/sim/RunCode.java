package com.wanderingnomad.sim;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A run code carries a saved run to the website: "N1." followed by the SavedRun JSON, gzipped and
 * base64url-encoded so it fits in a link. The website reads it with web/src/engine/runcode.ts.
 */
public final class RunCode
{
    static final String PREFIX = "N1.";

    private RunCode()
    {
    }

    public static String encode(String savedRunJson)
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(bytes))
        {
            gz.write(savedRunJson.getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
    }

    public static String decode(String code)
    {
        if (!code.startsWith(PREFIX)) throw new IllegalArgumentException("Not a Nomad UIM run code");
        byte[] gz = Base64.getUrlDecoder().decode(code.substring(PREFIX.length()));
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(gz)))
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }
}
