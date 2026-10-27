package com.wanderingnomad.sim;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Test;

public class SharedVectorsTest
{
    /** The website's sim is tested against this same file, so a match here means the two agree. */
    @Test
    public void matchesTheSharedVectors() throws IOException
    {
        String onDisk = new String(Files.readAllBytes(SharedVectors.FILE), StandardCharsets.UTF_8).trim();
        assertEquals("web/src/engine/vectors.json is stale: run SharedVectors.main to regenerate it",
            onDisk, SharedVectors.generate());
    }
}
