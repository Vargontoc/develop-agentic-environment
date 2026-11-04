package es.vargontoc.agentic.develop.cli.domain.tree;

import java.util.List;

import es.vargontoc.agentic.develop.cli.domain.artifact.Inventory;
import es.vargontoc.agentic.develop.cli.domain.team.TeamWarning;

/**
 * @param project  vacío si no se pidió un proyecto
 * @param warnings avisos de {@code team.md}; el árbol sigue siendo válido pero puede ser parcial
 * @param problems artefactos ilegibles o inválidos que no se han podido incluir
 */
public record ProjectTree(List<TreeNode> global, List<TreeNode> project, List<TeamWarning> warnings,
                          List<Inventory.Problem> problems) {

    public ProjectTree {
        global = List.copyOf(global);
        project = List.copyOf(project);
        warnings = List.copyOf(warnings);
        problems = List.copyOf(problems);
    }
}
