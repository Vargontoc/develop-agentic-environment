package es.vargontoc.agentic.develop.cli.domain.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.PathDomainException;

class HeaderMarkersTest {

    private static final String HEADER = """
        hooks:
          shell: {shell}
          command: '{hook:sdd-specs-only specs}'
        """;

    @Test
    void windowsGetsPowerShellAndTheBatHook() {
        String out = HeaderMarkers.render(HEADER, TargetOs.WINDOWS, ProviderType.CLAUDE);

        assertTrue(out.contains("shell: powershell"));
        assertTrue(out.contains("command: '& \"$env:USERPROFILE\\.claude\\hooks\\sdd-specs-only.bat\" specs; exit $LASTEXITCODE'"));
    }

    @Test
    void linuxGetsBashAndTheShellHookWithNoPowerShellNorUserProfile() {
        String out = HeaderMarkers.render(HEADER, TargetOs.LINUX, ProviderType.CLAUDE);

        assertTrue(out.contains("shell: bash"));
        assertTrue(out.contains("command: '\"$HOME/.claude/hooks/sdd-specs-only.sh\" specs'"));
        assertFalse(out.toLowerCase().contains("powershell"));
        assertFalse(out.contains("USERPROFILE"));
        assertFalse(out.contains("\\"));
    }

    @Test
    void theSameHeaderRendersDifferentlyPerSystem() {
        assertFalse(HeaderMarkers.render(HEADER, TargetOs.WINDOWS, ProviderType.CLAUDE)
            .equals(HeaderMarkers.render(HEADER, TargetOs.LINUX, ProviderType.CLAUDE)));
    }

    @Test
    void simpleMarkersAreReplacedEverywhere() {
        assertEquals("windows $env:USERPROFILE powershell",
            HeaderMarkers.render("{os} {home} {shell}", TargetOs.WINDOWS, ProviderType.CLAUDE));
        assertEquals("linux $HOME bash", HeaderMarkers.render("{os} {home} {shell}", TargetOs.LINUX, ProviderType.CLAUDE));
    }

    @Test
    void hooksUseTheProvidersOwnFolder() {
        assertEquals("\"$HOME/.config/opencode/hooks/x.sh\"", HeaderMarkers.render("{hook:x}", TargetOs.LINUX, ProviderType.OPENCODE));
        assertEquals("& \"$env:USERPROFILE\\.gemini\\config\\hooks\\x.bat\" a b; exit $LASTEXITCODE",
            HeaderMarkers.render("{hook:x a b}", TargetOs.WINDOWS, ProviderType.GEMINI));
    }

    @Test
    void otherBracesAndHeadersWithoutMarkersAreLeftUntouched() {
        String plain = "---\nname: x\nmodulo: {module}\n{ \"a\": 1 }\n---\n";

        assertEquals(plain, HeaderMarkers.render(plain, TargetOs.LINUX, ProviderType.CLAUDE));
        assertEquals(plain, HeaderMarkers.render(plain, TargetOs.WINDOWS, ProviderType.CLAUDE));
    }

    @Test
    void anEmptyHookMarkerIsAnErrorNamingTheMarker() {
        PathDomainException e = assertThrows(PathDomainException.class,
            () -> HeaderMarkers.render("command: '{hook: }'", TargetOs.LINUX, ProviderType.CLAUDE));

        assertEquals(AppErrorCode.HEADER_MARKER_INVALID, e.code());
        assertEquals("{hook: }", e.location());
    }
}
