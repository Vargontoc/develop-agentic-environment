package es.vargontoc.agentic.develop.cli.domain.tree;

import java.nio.file.Path;
import java.util.List;

import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactStatus;

/**
 * Nodo del árbol global o de proyecto.
 *
 * @param path     ruta en disco; null en nodos lógicos como un técnico
 * @param provider proveedor del nodo; null en nodos que no son de un proveedor concreto
 * @param status   estado de vinculación; null salvo en agentes, skills y perfiles instalados
 * @param pending  true si el elemento está previsto pero aún no existe: {@code .sdd} sin iniciar o técnico sin generar
 */
public record TreeNode(
        NodeType type,
        String name,
        Path path,
        ProviderType provider,
        ArtifactStatus status,
        boolean pending,
        List<TreeNode> children) {

    public enum NodeType { PROVIDER, AGENT, SKILL, PROFILE, SDD, MODULE, TECHNICIAN }

    public TreeNode {
        children = List.copyOf(children);
    }
}
