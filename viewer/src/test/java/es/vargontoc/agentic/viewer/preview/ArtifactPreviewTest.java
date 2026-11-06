package es.vargontoc.agentic.viewer.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import es.vargontoc.agentic.viewer.cli.model.DescribeData;

class ArtifactPreviewTest {

    @Test
    void separaFrontmatterYamlDelCuerpoMarkdown() {
        ArtifactPreview preview = ArtifactPreview.of(describe(
            "markdown", "agent", "---\nname: a\n---\n", "# Cuerpo", List.of()));

        assertEquals(CodeHighlighter.YAML, preview.headerLanguage());
        assertEquals("markdown", preview.bodyLanguage());
        assertTrue(preview.hasHeader());
        assertTrue(preview.hasBody());
    }

    @Test
    void marcaLaCabeceraTomlDelAgenteCodex() {
        ArtifactPreview preview = ArtifactPreview.of(describe(
            "codex_toml", "agent", "name = \"a\"\n", "instrucciones", List.of()));

        assertEquals(CodeHighlighter.TOML, preview.headerLanguage());
        assertEquals("markdown", preview.bodyLanguage());
    }

    @Test
    void trataElPerfilComoTomlSinCabecera() {
        ArtifactPreview preview = ArtifactPreview.of(describe(
            null, "profile", "", "llave = 1", List.of()));

        assertEquals(CodeHighlighter.TOML, preview.bodyLanguage());
        assertFalse(preview.hasHeader());
        assertTrue(preview.isSkill() == false);
    }

    @Test
    void listaLosFicherosDeUnaSkill() {
        ArtifactPreview preview = ArtifactPreview.of(describe(
            null, "skill", "", "", List.of("SKILL.md", "assets/guia.md")));

        assertTrue(preview.isSkill());
        assertEquals(List.of("SKILL.md", "assets/guia.md"), preview.files());
        assertFalse(preview.hasBody());
    }

    @Test
    void normalizaFilesNulosAVacio() {
        ArtifactPreview preview = ArtifactPreview.of(describe(
            "markdown", "agent", "---\nname: a\n---\n", "# Cuerpo", null));

        assertEquals(List.of(), preview.files());
    }

    private static DescribeData describe(String format, String kind, String header, String body,
                                         List<String> files) {
        return new DescribeData("claude", "global", kind, "nombre", "/ruta", format, null,
            header, body, null, "hash", "linked", false, files);
    }
}
