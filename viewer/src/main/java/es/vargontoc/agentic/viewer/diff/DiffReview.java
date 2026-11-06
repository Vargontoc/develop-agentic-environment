package es.vargontoc.agentic.viewer.diff;

import java.util.List;

import es.vargontoc.agentic.viewer.cli.model.SaveData;

/**
 * Resume el resultado de una propagación: miembros, resultados y diffs. Puro y
 * sin JavaFX, para poder probarlo en JUnit.
 */
public final class DiffReview {

    private DiffReview() {}

    /** Texto legible con el resumen y el diff de cada miembro. */
    public static String summary(SaveData data) {
        List<SaveData.Member> members = data.members() == null ? List.of() : data.members();

        StringBuilder out = new StringBuilder();
        long updated = members.stream().filter(m -> "updated".equals(m.outcome())).count();
        long conflicts = members.stream().filter(m -> "conflict".equals(m.outcome())).count();
        long others = members.size() - updated - conflicts;

        out.append("Miembros: ").append(members.size())
            .append(" · actualizará: ").append(updated)
            .append(" · conflicto: ").append(conflicts);
        if (others > 0) {
            out.append(" · omitidos: ").append(others);
        }

        if (!data.diff().isBlank() && members.isEmpty()) {
            out.append("\n\n").append(data.diff());
        }
        for (SaveData.Member member : members) {
            out.append("\n\n").append(memberLine(member));
        }
        return out.toString();
    }

    private static String memberLine(SaveData.Member member) {
        StringBuilder line = new StringBuilder();
        line.append(label(member.outcome())).append("  ").append(member.path());
        if (member.module() != null && !member.module().isBlank()) {
            line.append("  (módulo ").append(member.module()).append(')');
        }
        if (member.diff() != null && !member.diff().isBlank()) {
            line.append('\n').append(member.diff());
        }
        return line.toString();
    }

    private static String label(String outcome) {
        return switch (outcome == null ? "" : outcome) {
            case "updated" -> "actualizará";
            case "unchanged" -> "sin cambios";
            case "conflict" -> "conflicto";
            case "custom" -> "personalizado (se omite)";
            case "detached" -> "desvinculado (se omite)";
            case "missing" -> "ausente (se omite)";
            case "out_of_scope" -> "fuera de ámbito (se omite)";
            case "failed" -> "falló";
            default -> outcome;
        };
    }
}
