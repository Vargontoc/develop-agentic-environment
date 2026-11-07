package es.vargontoc.agentic.develop.cli.infrastructure.resources;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import es.vargontoc.agentic.develop.cli.domain.format.TargetOs;

class HookResourceTest {

    private static byte[] resource(String name) throws IOException {
        try(InputStream in = HookResourceTest.class.getResourceAsStream("/global/hooks/" + name)) {
            return in.readAllBytes();
        }
    }

    @Test
    void windowsGetsTheBatVariantAndLinuxTheShVariant() throws IOException {
        assertEquals("sdd-specs-only.bat", HookResource.fileName(TargetOs.WINDOWS));
        assertEquals("sdd-specs-only.sh", HookResource.fileName(TargetOs.LINUX));

        assertArrayEquals(resource("sdd-specs-only.bat"), HookResource.content(TargetOs.WINDOWS));
        assertArrayEquals(resource("sdd-specs-only.sh"), HookResource.content(TargetOs.LINUX));
    }

    @Test
    void neverReturnsTheVariantOfTheOtherSystem() throws IOException {
        byte[] windows = HookResource.content(TargetOs.WINDOWS);
        byte[] linux = HookResource.content(TargetOs.LINUX);

        assertTrue(windows.length > 0);
        assertTrue(linux.length > 0);
        assertFalse(java.util.Arrays.equals(windows, linux));
        assertTrue(new String(linux, StandardCharsets.UTF_8).startsWith("#!"));
        assertNotEquals("#!", new String(windows, StandardCharsets.UTF_8).substring(0, 2));
    }

    @Test
    void contentHasNoMarkersToSubstitute() throws IOException {
        for(TargetOs os : TargetOs.values()) {
            String text = new String(HookResource.content(os), StandardCharsets.UTF_8);
            assertFalse(text.contains("{hook:"), os.name());
            assertFalse(text.contains("{home}"), os.name());
            assertFalse(text.contains("{shell}"), os.name());
            assertFalse(text.contains("{os}"), os.name());
        }
    }
}
