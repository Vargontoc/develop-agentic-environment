package es.vargontoc.agentic.develop.cli.domain.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;

class ArtifactFormatTest {

    private static final String MD_HEADER = "---\nname: analista-x\n# comentario que debe sobrevivir\ntools: Read, Edit\n---\n\n";
    private static final String TOML_HEADER = "name = \"sdd_agent\"\n# comentario\ndeveloper_instructions = \"\"\"";

    // --- Markdown ---

    @Test
    void markdownSplitsHeaderAndBodyAndRecomposesTheSameText() {
        String text = MD_HEADER + "Eres analista.\n\n## Modo\n";

        ArtifactParts parts = ArtifactFormat.MARKDOWN.split(text);

        assertEquals(MD_HEADER, parts.header());
        assertEquals("Eres analista.\n\n## Modo\n", parts.body());
        assertEquals(text, ArtifactFormat.MARKDOWN.compose(parts));
    }

    @Test
    void markdownKeepsCrlfAndHeaderCommentsUntouched() {
        String text = MD_HEADER.replace("\n", "\r\n") + "Cuerpo\r\nsegunda línea\r\n";

        ArtifactParts parts = ArtifactFormat.MARKDOWN.split(text);

        assertTrue(parts.header().contains("# comentario que debe sobrevivir"));
        assertEquals("Cuerpo\r\nsegunda línea\r\n", parts.body());
        assertEquals(text, ArtifactFormat.MARKDOWN.compose(parts));
    }

    @Test
    void markdownWithOnlyFrontmatterHasEmptyBody() {
        String text = "---\nname: x\n---";

        ArtifactParts parts = ArtifactFormat.MARKDOWN.split(text);

        assertEquals("", parts.body());
        assertEquals(text, ArtifactFormat.MARKDOWN.compose(parts));
    }

    @Test
    void markdownBodyReplacementKeepsTheHeader() {
        ArtifactParts parts = ArtifactFormat.MARKDOWN.split(MD_HEADER + "viejo\n");

        String composed = ArtifactFormat.MARKDOWN.compose(parts.withBody("nuevo\n"));

        assertEquals(MD_HEADER + "nuevo\n", composed);
    }

    @Test
    void markdownWithoutRecognizableHeaderIsRejected() {
        assertInvalid(ArtifactFormat.MARKDOWN, "Solo cuerpo, sin frontmatter\n");
        assertInvalid(ArtifactFormat.MARKDOWN, "");
    }

    @Test
    void markdownWithUnclosedFrontmatterIsRejected() {
        assertInvalid(ArtifactFormat.MARKDOWN, "---\nname: x\nsin cierre\n");
    }

    @Test
    void markdownAssembleSeparatesHeaderAndBodyWithBlankLine() {
        String result = ArtifactFormat.MARKDOWN.assemble("---\nname: x\n---\n", "Cuerpo");

        assertEquals("---\nname: x\n---\n\nCuerpo", result);
    }

    // --- Codex TOML ---

    @Test
    void tomlSplitsHeaderBodyAndSuffixAndRecomposesTheSameText() {
        String text = TOML_HEADER + "Eres el agente.\nSegunda línea." + "\"\"\"\n";

        ArtifactParts parts = ArtifactFormat.CODEX_TOML.split(text);

        assertEquals(TOML_HEADER, parts.header());
        assertEquals("Eres el agente.\nSegunda línea.", parts.body());
        assertEquals("\n", parts.suffix());
        assertEquals(text, ArtifactFormat.CODEX_TOML.compose(parts));
    }

    @Test
    void tomlBodyWithTripleQuotesIsEscapedAndRoundTripsLogically() {
        String body = "Usa \"\"\"triple\"\"\" con cuidado y una ruta C:\\tmp\\x";

        String composed = ArtifactFormat.CODEX_TOML.compose(new ArtifactParts(TOML_HEADER, body));
        ArtifactParts parts = ArtifactFormat.CODEX_TOML.split(composed);

        assertEquals(body, parts.body());
        assertEquals(TOML_HEADER, parts.header());
        assertEquals("", parts.suffix());
    }

    @Test
    void tomlBodyEndingWithAQuoteDoesNotMergeWithTheClosingDelimiter() {
        String body = "termina con comilla\"";

        String composed = ArtifactFormat.CODEX_TOML.compose(new ArtifactParts(TOML_HEADER, body));

        assertEquals(body, ArtifactFormat.CODEX_TOML.split(composed).body());
    }

    @Test
    void tomlAssembleFromHeaderResourceAddsQuotesAndEscapes() {
        String headerResource = "name = \"x\"\ndeveloper_instructions = ";

        String result = ArtifactFormat.CODEX_TOML.assemble(headerResource, "Cuerpo con \\ barra y \"\"\" comillas");
        ArtifactParts parts = ArtifactFormat.CODEX_TOML.split(result);

        assertEquals("Cuerpo con \\ barra y \"\"\" comillas", parts.body());
        assertEquals(headerResource + "\"\"\"", parts.header());
    }

    @Test
    void tomlStandardEscapesAreUnescapedInTheLogicalBody() {
        String text = TOML_HEADER + "a\\tb\\n\\u00e1\\\"" + "\"\"\"";

        assertEquals("a\tb\n\u00e1\"", ArtifactFormat.CODEX_TOML.split(text).body());
    }

    @Test
    void tomlWithoutInstructionsMarkerOrClosingIsRejected() {
        assertInvalid(ArtifactFormat.CODEX_TOML, "name = \"x\"\n");
        assertInvalid(ArtifactFormat.CODEX_TOML, TOML_HEADER + "sin cierre");
        assertInvalid(ArtifactFormat.CODEX_TOML, TOML_HEADER + "escape roto \\q" + "\"\"\"");
    }

    private static void assertInvalid(ArtifactFormat format, String text) {
        DomainException e = assertThrows(DomainException.class, () -> format.split(text));
        assertEquals(AppErrorCode.ARTIFACT_FORMAT_INVALID, e.code());
    }
}
