package es.vargontoc.agentic.develop.cli.domain.team;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import es.vargontoc.agentic.develop.cli.domain.team.TeamWarning.Type;

class TeamParserTest {

    private static final String HEAD = "# Equipo\n\n<!-- comentario -->\n<!--\n multilínea\n-->\n\n## Módulos\n\n";

    @Test
    void validTeamReturnsModulesWithIdsAndPaths() {
        TeamInfo info = TeamParser.parse(HEAD + "- alfa | cli | cli/src cli/test\n- beta |  -  |  -\n\n## Opciones\n\n- sprints: x\n");

        assertTrue(info.warnings().isEmpty());
        assertEquals(List.of("alfa", "beta"), info.moduleNames());
        ModuleEntry alfa = info.modules().get(0);
        assertEquals("cli", alfa.mapId());
        assertEquals(List.of("cli/src", "cli/test"), alfa.paths());
        assertNull(info.modules().get(1).mapId());
        assertTrue(info.modules().get(1).paths().isEmpty());
    }

    @Test
    void templateWithoutModulesIsValidAndEmpty() {
        TeamInfo info = TeamParser.parse(HEAD + "## Opciones\n\n- sprints: \n");

        assertTrue(info.modules().isEmpty());
        assertTrue(info.warnings().isEmpty());
    }

    @Test
    void malformedLinesAreReportedAndTheRestIsKept() {
        String content = HEAD
            + "- sin pipes\n"
            + "- ok | - | -\n"
            + "- a | b\n"
            + "texto suelto\n"
            + "- | x | y\n"
            + "- dos palabras | - | -\n"
            + "- a | b | c | d\n"
            + "- tambien | - | rutas\n";

        TeamInfo info = TeamParser.parse(content);

        assertEquals(List.of("ok", "tambien"), info.moduleNames());
        assertEquals(List.of(
            new TeamWarning(Type.MALFORMED_LINE, 10),
            new TeamWarning(Type.MALFORMED_LINE, 12),
            new TeamWarning(Type.MALFORMED_LINE, 13),
            new TeamWarning(Type.MALFORMED_LINE, 14),
            new TeamWarning(Type.MALFORMED_LINE, 15),
            new TeamWarning(Type.MALFORMED_LINE, 16)), info.warnings());
    }

    @Test
    void duplicatedModulesKeepTheFirstOneAndWarn() {
        TeamInfo info = TeamParser.parse(HEAD + "- alfa | uno | a\n- beta | - | -\n- alfa | dos | b\n");

        assertEquals(List.of("alfa", "beta"), info.moduleNames());
        assertEquals("uno", info.modules().get(0).mapId());
        assertEquals(List.of(new TeamWarning(Type.DUPLICATE_MODULE, 12)), info.warnings());
    }

    @Test
    void missingModulesSectionIsReported() {
        TeamInfo info = TeamParser.parse("# Equipo\n\n## Opciones\n- alfa | - | -\n");

        assertTrue(info.modules().isEmpty());
        assertEquals(List.of(new TeamWarning(Type.SECTION_MISSING, 0)), info.warnings());
    }

    @Test
    void crlfFilesAreParsedTheSame() {
        TeamInfo info = TeamParser.parse((HEAD + "- alfa | - | -\n- beta | - | -\n").replace("\n", "\r\n"));

        assertEquals(List.of("alfa", "beta"), info.moduleNames());
    }

    @Test
    void specialistsFollowTheRoleModuleConvention() {
        ModuleEntry entry = TeamParser.parse(HEAD + "- alfa | - | -\n").modules().get(0);

        assertEquals(List.of("analista-alfa", "desarrollador-alfa", "documentador-alfa"), entry.specialists());
    }
}
