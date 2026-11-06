package es.vargontoc.agentic.viewer.cli.model;

import java.util.List;

/**
 * Resultado del comando {@code save}. Para el guardado de instancia (TASK-007)
 * interesa {@code written} y {@code diff}; {@code members} queda para la
 * propagación de grupo.
 */
public record SaveData(
    String mode,
    boolean dryRun,
    String path,
    String groupId,
    boolean written,
    boolean tracked,
    boolean groupUpdated,
    String diff,
    List<String> backups,
    List<Member> members
) {

    public record Member(String path, String provider, String module, String outcome, String diff,
                         List<String> backups) {
    }
}
