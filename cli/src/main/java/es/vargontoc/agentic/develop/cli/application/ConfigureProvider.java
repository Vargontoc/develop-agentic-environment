package es.vargontoc.agentic.develop.cli.application;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.vargontoc.agentic.develop.cli.domain.Provider;
import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.domain.format.ArtifactFormat;
import es.vargontoc.agentic.develop.cli.domain.format.HeaderMarkers;
import es.vargontoc.agentic.develop.cli.domain.format.TargetOs;
import es.vargontoc.agentic.develop.cli.infrastructure.resources.ClasspathResources;

/**
 * Instala en un proveedor los agentes, skills y perfiles globales de la aplicación sin sobrescribir lo existente.
 * Las rutas del resultado son relativas a la carpeta del proveedor (por ejemplo {@code skills/sdd-design}).
 */
public class ConfigureProvider {

    private static final Logger log = LoggerFactory.getLogger(ConfigureProvider.class);

    private static final String SKILLS_RESOURCE = "/global/skills";
    private static final String HELPER_DIR = "/global/helper";
    private static final String AGENTS_RESOURCE = "/global/agents";
    private static final String TEMPLATES_RESOURCE = "/global/agents/shared";
    private static final String PROFILES_RESOURCE = "/global/profiles";
    private static final String CODEX_PROFILE = "sdd.config.toml";

    private final TargetOs os;

    public ConfigureProvider() {
        this(TargetOs.current());
    }

    /** @param os sistema para el que se generan las cabeceras (DEC-012) */
    public ConfigureProvider(TargetOs os) {
        this.os = os;
    }

    public OperationResult execute(Provider provider, boolean global) {
        OperationResult result = new OperationResult();
        manageConfiguration(provider, global, result);
        return result;
    }

    private void loadAgents(Path providerDir, ProviderType type, OperationResult result) {
        String realPath = AGENTS_RESOURCE + "/" + type.resourceDir();

        try {
            Optional<List<String>> agents = ClasspathResources.listFiles(realPath);
            if(agents.isEmpty()) {
                fail(result, "Resource '" + realPath + "' not found", null);
                return;
            }
            createAgents(realPath, agents.get(), providerDir.resolve("agents"), type, result);
        }catch(IOException e) {
            fail(result, e.getMessage(), e);
        }
    }

    private void createAgents(String agentsResource, List<String> agents, Path destRoot, ProviderType type,
                              OperationResult result) throws IOException {
        Files.createDirectories(destRoot);

        for(String fileName : agents) {
            Path target = destRoot.resolve(fileName);
            String relative = "agents/" + fileName;

            if(Files.exists(target)) {
                result.skipped().add(relative);
                continue;
            }

            String body = readAgentBody(fileName, result);
            String agent = body == null ? null : readResource(agentsResource + "/" + fileName, result);
            if(agent == null)
                continue;
            try {
                agent = HeaderMarkers.render(agent, os, type);
            } catch(DomainException e) {
                fail(result, "Resource '" + agentsResource + "/" + fileName + "' has an invalid header marker", e);
                continue;
            }

            Path temp = Files.createTempFile("agent-", ".md");
            try {
                Files.writeString(temp, composeAgent(agent, body, type));
                Files.copy(temp, target);
                result.created().add(relative);
            } finally {
                Files.deleteIfExists(temp);
            }
        }
    }

    // Codex: el cuerpo se añade entre comillas triples (cadena multilínea TOML) pegado a "developer_instructions = ".
    // El resto: frontmatter + cuerpo.
    private String composeAgent(String agent, String body, ProviderType type) {
        ArtifactFormat format = type == ProviderType.CODEX ? ArtifactFormat.CODEX_TOML : ArtifactFormat.MARKDOWN;
        return format.assemble(agent, body);
    }

    // <agente>.<ext> -> shared/<agente>-body.md (los "_" del nombre se corresponden con "-")
    private String readAgentBody(String agentFileName, OperationResult result) throws IOException {
        String baseName = agentFileName.substring(0, agentFileName.lastIndexOf('.')).replace('_', '-');
        return readResource(TEMPLATES_RESOURCE + "/" + baseName + "-body.md", result);
    }

    private String readResource(String resource, OperationResult result) throws IOException {
        Optional<String> text = ClasspathResources.readText(resource);
        if(text.isEmpty())
            fail(result, "Resource '" + resource + "' not found", null);
        return text.orElse(null);
    }

    private void loadSkills(Path providerDir, ProviderType type, OperationResult result) {
        try {
            Optional<List<String>> skills = ClasspathResources.listDirectories(SKILLS_RESOURCE);
            if(skills.isEmpty()) {
                fail(result, "Resource '" + SKILLS_RESOURCE + "' not found", null);
                return;
            }
            copySkills(skills.get(), providerDir.resolve("skills"), type, result);
        }catch(IOException e) {
            fail(result, e.getMessage(), e);
        }
    }

    private void copySkills(List<String> skills, Path destRoot, ProviderType type, OperationResult result) throws IOException {
        Files.createDirectories(destRoot);

        for(String name : skills) {
            Path target = destRoot.resolve(name);
            String relative = "skills/" + name;

            if(Files.exists(target)) {
                result.skipped().add(relative);
                continue;
            }

            ClasspathResources.copyTreeIfAbsent(SKILLS_RESOURCE + "/" + name, target);
            if(type == ProviderType.CODEX)
                copyCodexHelper(target, result);
            result.created().add(relative);
        }
    }

    private void copyCodexHelper(Path skillDir, OperationResult result) throws IOException {
        Path agents = skillDir.resolve("agents");
        if(Files.exists(agents))
            return;

        try(InputStream helper = ConfigureProvider.class.getResourceAsStream(HELPER_DIR + "/openai.yaml")) {
            if(helper == null) {
                fail(result, "Resource '" + HELPER_DIR + "/openai.yaml' not found", null);
                return;
            }
            Files.createDirectories(agents);
            Files.copy(helper, agents.resolve("openai.yaml"));
        }
    }

    private void copyCodexProfile(Path providerDir, OperationResult result) {
        Path target = providerDir.resolve(CODEX_PROFILE);
        if(Files.exists(target)) {
            result.skipped().add(CODEX_PROFILE);
            return;
        }

        try(InputStream profile = ConfigureProvider.class.getResourceAsStream(PROFILES_RESOURCE + "/" + CODEX_PROFILE)) {
            if(profile == null) {
                fail(result, "Resource '" + PROFILES_RESOURCE + "/" + CODEX_PROFILE + "' not found", null);
                return;
            }
            Files.copy(profile, target);
            result.created().add(CODEX_PROFILE);
        } catch(IOException e) {
            fail(result, e.getMessage(), e);
        }
    }

    // Un fallo parcial no detiene la configuración: queda en el log y como aviso del resultado
    private void fail(OperationResult result, String message, Exception cause) {
        if(cause == null)
            log.error(message);
        else
            log.error(message, cause);
        result.warnings().add(message);
    }

    void manageConfiguration(Provider provider, boolean global, OperationResult result)
    {
        ProviderType type = provider.getType();

        if(global)
        {
            // Skills
            loadSkills(provider.getPath(), type, result);

            // Agents
            loadAgents(provider.getPath(), type, result);

            // Profiles (Codex)
            if(type == ProviderType.CODEX)
                copyCodexProfile(provider.getPath(), result);
        }
    }
}
