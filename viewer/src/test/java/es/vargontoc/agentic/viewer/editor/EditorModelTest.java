package es.vargontoc.agentic.viewer.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import es.vargontoc.agentic.viewer.cli.model.DescribeData;

class EditorModelTest {

    @Test
    void unAgenteTieneCabeceraYCuerpoEditables() {
        EditorModel editor = EditorModel.of(
            describe("markdown", "agent", "---\nname: a\n---\n", "cuerpo", null, null), null);

        assertTrue(editor.headerEditable());
        assertTrue(editor.bodyEditable());
        assertFalse(editor.isDirty());
    }

    @Test
    void unaSkillNoTieneCabeceraNiCuerpoEditables() {
        EditorModel editor = EditorModel.of(
            describe(null, "skill", "", "", null, List.of("SKILL.md")), null);

        assertFalse(editor.headerEditable());
        assertFalse(editor.bodyEditable());
    }

    @Test
    void unPerfilTieneCuerpoEditableSinCabecera() {
        EditorModel editor = EditorModel.of(
            describe(null, "profile", "", "llave = 1", null, null), null);

        assertFalse(editor.headerEditable());
        assertTrue(editor.bodyEditable());
    }

    @Test
    void detectaLosCambiosDeCuerpoYCabecera() {
        EditorModel editor = EditorModel.of(
            describe("markdown", "agent", "---\nname: a\n---\n", "cuerpo", null, null), null);

        editor.setBody("cuerpo nuevo");
        assertTrue(editor.bodyDirty());
        assertTrue(editor.isDirty());

        editor.markBodySaved();
        assertFalse(editor.bodyDirty());

        editor.setHeader("---\nname: b\n---\n");
        assertTrue(editor.headerDirty());
        editor.markHeaderSaved();
        assertFalse(editor.isDirty());
    }

    @Test
    void marcaElGrupoYElPlaceholderDelEspecialista() {
        EditorModel especialista = EditorModel.of(describe(
            "markdown", "agent", "---\nname: a\n---\n", "Eres analista-facturacion", "Eres analista-{module}", null), null);

        assertTrue(especialista.hasGroup());
        assertTrue(especialista.hasPlaceholder());
        assertFalse(especialista.sharedBodyDirty());
    }

    @Test
    void sinGrupoNoHayCuerpoCompartido() {
        EditorModel editor = EditorModel.of(describe(
            "markdown", "agent", "---\nname: a\n---\n", "cuerpo", null, null), null);

        assertFalse(editor.hasGroup());
        assertFalse(editor.hasPlaceholder());
        assertFalse(editor.sharedBodyDirty());
    }

    @Test
    void detectaCambiosEnElCuerpoCompartido() {
        EditorModel editor = EditorModel.of(describe(
            "markdown", "agent", "---\nname: a\n---\n", "Eres analista-facturacion", "Eres analista-{module}", null), null);

        editor.setSharedBody("Eres analista-{module} mejorado");
        assertTrue(editor.sharedBodyDirty());
        assertTrue(editor.isDirty());

        editor.markSharedBodySaved();
        assertFalse(editor.sharedBodyDirty());
        assertFalse(editor.isDirty());
    }

    @Test
    void conservaLaRutaYElProyecto() {
        EditorModel editor = EditorModel.of(
            describe("markdown", "agent", "---", "cuerpo", null, null), Path.of("tmp", "proyecto"));

        assertEquals(Path.of("tmp", "agente.md"), editor.path());
        assertEquals(Path.of("tmp", "proyecto"), editor.project());
    }

    private static DescribeData describe(String format, String kind, String header, String body,
                                         String sharedBody, List<String> files) {
        return new DescribeData("claude", "global", kind, "nombre", "tmp/agente.md", format, null,
            header, body, sharedBody, "hash", "linked", false, files);
    }
}
