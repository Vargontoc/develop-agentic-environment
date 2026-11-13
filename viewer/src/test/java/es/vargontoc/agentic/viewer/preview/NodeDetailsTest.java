package es.vargontoc.agentic.viewer.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import es.vargontoc.agentic.viewer.cli.model.TeamData;
import es.vargontoc.agentic.viewer.cli.model.TreeData;

class NodeDetailsTest {

    @Test
    void detallaModuloConMapaRutasTecnicosYArtefactos() {
        TreeData.Node artifact = artifact("analista-alfa", "claude", "linked", "/p/.claude/agents/analista-alfa.md");
        TreeData.Node technician = new TreeData.Node("technician", "analista", null, null, null, false,
            List.of(artifact));
        TreeData.Node module = new TreeData.Node("module", "alfa", null, null, null, false, List.of(technician));
        TeamData team = new TeamData("/p/.sdd/team.md", "hash",
            List.of(new TeamData.Module("alfa", "ALFA", List.of("src/alfa", "docs/alfa"))),
            new TeamData.Options("si", "balanced", "implementation-first", null), List.of());

        NodeDetail detail = NodeDetails.module(module, team);

        assertEquals("alfa", detail.heading());
        assertTrue(detail.subtitle().contains("1 técnico"));
        assertEquals("Módulo", detail.sections().get(0).title());
        assertEquals("ALFA", item(detail, "Módulo", "Identificador").detail());
        assertEquals("src/alfa, docs/alfa", item(detail, "Módulo", "Rutas").detail());
        assertEquals("1 artefacto", item(detail, "Técnicos (1)", "analista").detail());
        NodeDetail.Item artifactItem = item(detail, "Artefactos (1)", "analista-alfa");
        assertEquals("vinculado", artifactItem.status());
        assertEquals("/p/.claude/agents/analista-alfa.md", artifactItem.path());
        assertTrue(artifactItem.detail().contains("claude"));
    }

    @Test
    void detallaTecnicoConSuModuloYArtefactos() {
        TreeData.Node artifact = artifact("documentador-alfa", "codex", null, "/p/.codex/agents/documentador_alfa.toml");
        TreeData.Node technician = new TreeData.Node("technician", "documentador", null, null, null, true,
            List.of(artifact));
        TreeData.Node module = new TreeData.Node("module", "alfa", null, null, null, false, List.of(technician));

        NodeDetail detail = NodeDetails.technician(technician, module);

        assertEquals("documentador", detail.heading());
        assertTrue(detail.subtitle().contains("alfa"));
        assertEquals("pendiente", item(detail, "Técnico", "Rol").status());
        assertEquals("alfa", item(detail, "Técnico", "Módulo").detail());
        assertEquals("1 instalado", item(detail, "Técnico", "Artefactos").detail());
        assertEquals("/p/.codex/agents/documentador_alfa.toml",
            item(detail, "Artefactos (1)", "documentador-alfa").path());
    }

    @Test
    void sinTeamNiPadreSigueMostrandoUnaFicha() {
        TreeData.Node technician = new TreeData.Node("technician", "analista", null, null, null, true, List.of());
        TreeData.Node module = new TreeData.Node("module", "alfa", null, null, null, false, List.of(technician));

        NodeDetail moduleDetail = NodeDetails.module(module, null);
        NodeDetail technicianDetail = NodeDetails.technician(technician, null);

        assertEquals("sin datos", item(moduleDetail, "Módulo", "Nombre").status());
        assertEquals("—", item(technicianDetail, "Técnico", "Módulo").detail());
    }

    private static NodeDetail.Item item(NodeDetail detail, String section, String title) {
        return detail.sections().stream()
            .filter(s -> section.equals(s.title()))
            .flatMap(s -> s.items().stream())
            .filter(i -> title.equals(i.title()))
            .findFirst()
            .orElseThrow();
    }

    private static TreeData.Node artifact(String name, String provider, String status, String path) {
        return new TreeData.Node("agent", name, path, provider, status, false, List.of());
    }
}
