package es.vargontoc.agentic.viewer.draft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import es.vargontoc.agentic.viewer.cli.model.DescribeData;
import es.vargontoc.agentic.viewer.editor.EditorModel;
import es.vargontoc.agentic.viewer.editor.SkillEditorModel;

class DraftStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void guardaYRecuperaUnAgente() {
        Path artifact = tempDir.resolve("agente.md");
        Path project = tempDir.resolve("proyecto");
        EditorModel editor = EditorModel.of(describe(artifact, "agent", "header", "body", null, null), project);
        editor.setBody("borrador");

        DraftStore store = new DraftStore(tempDir.resolve("drafts.json"));
        store.save(editor);
        DraftStore.Draft draft = store.find(artifact.toString()).orElseThrow();

        assertEquals("borrador", draft.body());
        assertEquals(project.toAbsolutePath().normalize().toString(), draft.project());
        assertEquals("agent", draft.kind());
        assertTrue(store.hasDrafts());
    }

    @Test
    void guardaTodosLosFicherosDeUnaSkill() {
        Path skill = tempDir.resolve("skills/mi-skill");
        DescribeData data = describe(skill, "skill", null, null, null, List.of("SKILL.md", "agents/openai.yaml"));
        SkillEditorModel editor = new SkillEditorModel(data, tempDir, Map.of(
            "SKILL.md", "original",
            "agents/openai.yaml", "model: x"));
        editor.setContent("SKILL.md", "borrador");

        DraftStore store = new DraftStore(tempDir.resolve("drafts.json"));
        store.save(editor);
        DraftStore.Draft draft = store.find(skill.toString()).orElseThrow();

        assertEquals("skill", draft.kind());
        assertEquals("borrador", draft.files().get("SKILL.md"));
        assertEquals("model: x", draft.files().get("agents/openai.yaml"));
    }

    @Test
    void borrarUnArtefactoQuitaSuBorrador() {
        Path artifact = tempDir.resolve("agente.md");
        EditorModel editor = EditorModel.of(describe(artifact, "agent", "h", "b", null, null), null);
        editor.setBody("cambio");

        DraftStore store = new DraftStore(tempDir.resolve("drafts.json"));
        store.save(editor);
        assertTrue(store.find(artifact.toString()).isPresent());

        store.delete(artifact.toString());

        assertFalse(store.find(artifact.toString()).isPresent());
        assertFalse(store.hasDrafts());
    }

    @Test
    void losBorradoresCaducadosNoSeRecuperan() {
        Path artifact = tempDir.resolve("agente.md");
        DraftStore store = new DraftStore(tempDir.resolve("drafts.json"));
        store.put(new DraftStore.Draft(
            artifact.toAbsolutePath().normalize().toString(), null, "agent",
            null, "viejo", null, Map.of(), 1L));

        assertFalse(store.hasDrafts());
    }

    private static DescribeData describe(Path path, String kind, String header, String body,
                                         String sharedBody, List<String> files) {
        return new DescribeData(
            "claude", "project", kind, path.getFileName().toString(), path.toString(),
            "markdown", null, header, body, sharedBody, "hash", "linked", false, files);
    }
}
