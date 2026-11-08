package es.vargontoc.agentic.develop.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import es.vargontoc.agentic.develop.cli.command.Command;
import es.vargontoc.agentic.develop.cli.command.CommandRegistry;
import es.vargontoc.agentic.develop.cli.command.Commands;
import es.vargontoc.agentic.develop.cli.command.Envelope;
import es.vargontoc.agentic.develop.cli.command.ExitCode;
import es.vargontoc.agentic.develop.cli.command.UsageError;
import es.vargontoc.agentic.develop.cli.domain.ProviderState;
import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactStatus;
import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.metadata.LinkState;
import es.vargontoc.agentic.develop.cli.domain.team.ModuleEntry;
import es.vargontoc.agentic.develop.cli.domain.team.TeamParser;
import es.vargontoc.agentic.develop.cli.domain.team.TeamWarning;
import es.vargontoc.agentic.develop.cli.domain.tree.TreeNode;
import es.vargontoc.agentic.develop.cli.infrastructure.metadata.MetadataStore;

/**
 * Los contratos de docs/contracts deben decir lo mismo que el código: si se añade un comando, una opción, un código de
 * error, un estado o un proveedor sin documentarlo, esta prueba falla.
 */
class ContractsTest {

    private static final Path CONTRACTS = Path.of("..", "docs", "contracts");

    private static String contract(String folder, String file) throws IOException {
        return Files.readString(CONTRACTS.resolve(folder).resolve(file));
    }

    private static String lower(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }

    @Test
    void cliJsonDocumentsEveryCommandWithItsOptions() throws IOException {
        String doc = contract("cli-json", "cli-json.md");
        CommandRegistry registry = Commands.standard(() -> { throw new IllegalStateException(); });

        for(Command command : registry.all()) {
            String heading = "### `" + command.name() + "`";
            int start = doc.indexOf(heading);
            assertTrue(start >= 0, "falta el comando " + command.name());
            int next = doc.indexOf("\n### ", start + heading.length());
            int end = next < 0 ? doc.length() : next;
            // La sección del último comando llega hasta el siguiente «##», no hasta el final del documento
            int section = doc.indexOf("\n## ", start);
            String text = doc.substring(start, section >= 0 ? Math.min(end, section) : end);

            for(var option : command.options())
                assertTrue(text.contains("`--" + option.name() + "`"), command.name() + " no documenta --" + option.name());
        }
    }

    @Test
    void cliJsonDocumentsEveryErrorCodeAndExitCode() throws IOException {
        String doc = contract("cli-json", "cli-json.md");

        for(AppErrorCode code : AppErrorCode.values()) {
            if(code == AppErrorCode.PROJECT_PATH_INVALID || !code.value().isEmpty())
                assertTrue(doc.contains("`" + code.value() + "`"), "falta el código " + code.value());
        }
        for(UsageError error : UsageError.values())
            assertTrue(doc.contains("`" + error.value() + "`"), "falta el código " + error.value());
        assertTrue(doc.contains("`internal.unexpected`"));
        for(ExitCode exit : ExitCode.values())
            assertTrue(doc.contains("| " + exit.value() + " |"), "falta el código de salida " + exit.value());
        assertTrue(doc.contains("Versión del esquema: **" + Envelope.SCHEMA_VERSION + "**"));
    }

    @Test
    void cliJsonDocumentsTheEnumerationsOfTheData() throws IOException {
        String doc = contract("cli-json", "cli-json.md");

        for(ProviderState state : ProviderState.values())
            assertTrue(doc.contains("`" + lower(state) + "`"), "falta el estado " + state);
        for(ArtifactStatus status : ArtifactStatus.values())
            assertTrue(doc.contains("`" + lower(status) + "`"), "falta el estado " + status);
        for(TeamWarning.Type type : TeamWarning.Type.values())
            assertTrue(doc.contains("`" + lower(type) + "`"), "falta el aviso " + type);
        for(TreeNode.NodeType type : TreeNode.NodeType.values())
            assertTrue(doc.contains("`" + lower(type) + "`"), "falta el tipo de nodo " + type);
    }

    @Test
    void artifactMetadataDocumentsTheStatesTheSchemaVersionAndTheGroupIds() throws IOException {
        String doc = contract("artifact-metadata", "artifact-metadata.md");

        for(LinkState state : LinkState.values())
            assertTrue(doc.contains("`" + state.name() + "`"), "falta el estado " + state);
        assertTrue(doc.contains("Versión de esquema: **" + MetadataStore.SCHEMA_VERSION + "**"));
        assertTrue(doc.contains("\"schemaVersion\": " + MetadataStore.SCHEMA_VERSION));
        for(String id : new String[] {"global/agent/", "/skill/", "/profile/", "project/router/", "project/specialist/"})
            assertTrue(doc.contains(id), "falta el identificador de grupo " + id);
        assertTrue(doc.contains("sha-256") || doc.contains("SHA-256"));
    }

    @Test
    void providerLayoutDocumentsEveryProviderWithItsFolders() throws IOException {
        String doc = contract("provider-layout", "provider-layout.md");

        for(ProviderType type : ProviderType.values()) {
            assertTrue(doc.contains("(`" + type.id() + "`)"), "falta el proveedor " + type.id());
            assertTrue(doc.contains(type.globalDirName().replace('/', '\\')) || doc.contains(type.globalDirName()),
                "falta la carpeta global de " + type.id());
            for(String dir : type.projectDirs())
                assertTrue(doc.contains("`" + dir + "`") || doc.contains("`" + dir + "/"),
                    "falta la carpeta de proyecto " + dir + " de " + type.id());
        }
        for(String marker : new String[] {"{shell}", "{home}", "{os}", "{hook:"})
            assertTrue(doc.contains(marker), "falta el marcador " + marker);
    }

    @Test
    void sddTeamDocumentsTheSectionTheLineFormatAndTheWarnings() throws IOException {
        String doc = contract("sdd-team", "sdd-team.md");

        assertTrue(doc.contains(TeamParser.MODULES_HEADER));
        for(TeamWarning.Type type : TeamWarning.Type.values())
            assertTrue(doc.contains("`" + lower(type) + "`"), "falta el aviso " + type);

        // La línea que documenta como escrita por create-module se lee como un módulo válido sin avisos
        String content = TeamParser.MODULES_HEADER + "\n\n- facturacion | FAC | src/facturacion docs/facturacion\n- alfa | - | -\n";
        var team = TeamParser.parse(content);
        assertTrue(team.warnings().isEmpty());
        assertEquals(2, team.modules().size());
        ModuleEntry alfa = team.modules().get(1);
        assertEquals("alfa", alfa.name());
        assertTrue(doc.contains("- alfa | - | -"));
        assertTrue(doc.contains("- facturacion | FAC | src/facturacion docs/facturacion"));
    }
}
