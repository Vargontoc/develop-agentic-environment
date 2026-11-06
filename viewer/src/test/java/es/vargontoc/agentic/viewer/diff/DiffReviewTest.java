package es.vargontoc.agentic.viewer.diff;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import es.vargontoc.agentic.viewer.cli.model.SaveData;

class DiffReviewTest {

    @Test
    void resumeMiembrosActualizadosConflictosYOmitidos() {
        SaveData data = new SaveData("group", true, "/p/a.md", "g1", true, true, true, "",
            List.of(), List.of(
                member("updated", "auth", "-a\n+b"),
                member("conflict", "facturacion", null),
                member("missing", "billing", null)));

        String summary = DiffReview.summary(data);

        assertTrue(summary.contains("Miembros: 3"), summary);
        assertTrue(summary.contains("actualizará: 1"), summary);
        assertTrue(summary.contains("conflicto: 1"), summary);
        assertTrue(summary.contains("omitidos: 1"), summary);
        assertTrue(summary.contains("-a\n+b"), summary);
        assertTrue(summary.contains("módulo auth"), summary);
    }

    @Test
    void etiquetaLosEstadosQueSeOmiten() {
        SaveData data = new SaveData("group", true, "/p/a.md", "g1", true, true, true, "",
            List.of(), List.of(
                member("custom", null, null),
                member("detached", null, null),
                member("out_of_scope", null, null),
                member("unchanged", null, null)));

        String summary = DiffReview.summary(data);

        assertTrue(summary.contains("personalizado (se omite)"), summary);
        assertTrue(summary.contains("desvinculado (se omite)"), summary);
        assertTrue(summary.contains("fuera de ámbito (se omite)"), summary);
        assertTrue(summary.contains("sin cambios"), summary);
    }

    private static SaveData.Member member(String outcome, String module, String diff) {
        return new SaveData.Member("/p/agente.md", "claude", module, outcome, diff, List.of());
    }
}
