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
import es.vargontoc.agentic.develop.cli.domain.format.ArtifactFormat;
import es.vargontoc.agentic.develop.cli.infrastructure.resources.ClasspathResources;

public class ConfigureProvider {

    private static final Logger log = LoggerFactory.getLogger(ConfigureProvider.class);

    private static final String SKILLS_RESOURCE = "/global/skills";
    private static final String HELPER_DIR = "/global/helper";
    private static final String AGENTS_RESOURCE = "/global/agents";
    private static final String TEMPLATES_RESOURCE = "/global/agents/shared";
    private static final String PROFILES_RESOURCE = "/global/profiles";
    private static final String CODEX_PROFILE = "sdd.config.toml";

    public void execute(Provider provider, boolean global) {
        System.out.println("Configurando " + provider.getType());
        manageConfiguration(provider, global);
    }

    private void loadAgents(Path providerDir, ProviderType type) {
        String realPath = AGENTS_RESOURCE + "/" + type.resourceDir();

        try {
            Optional<List<String>> agents = ClasspathResources.listFiles(realPath);
            if(agents.isEmpty()) {
                log.error("Resource '{}' not found", realPath);
                return;
            }
            createAgents(realPath, agents.get(), providerDir.resolve("agents"), type);
        }catch(IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void createAgents(String agentsResource, List<String> agents, Path destRoot, ProviderType type) throws IOException {
        Files.createDirectories(destRoot);

        for(String fileName : agents) {
            Path target = destRoot.resolve(fileName);

            if(Files.exists(target)) {
                System.out.println("Agent '" + fileName + "' exists");
                continue;
            }

            String body = readAgentBody(fileName);
            if(body == null)
                continue;

            String agent = ClasspathResources.readText(agentsResource + "/" + fileName).orElse(null);
            if(agent == null) {
                log.error("Resource '{}/{}' not found", agentsResource, fileName);
                continue;
            }

            Path temp = Files.createTempFile("agent-", ".md");
            try {
                Files.writeString(temp, composeAgent(agent, body, type));
                Files.copy(temp, target);
                System.out.println("Copied agent '" + fileName + "'");
            } finally {
                Files.deleteIfExists(temp);
            }
        }
    }

    // Codex: el cuerpo se añade tal cual entre comillas triples (cadena multilínea TOML),
    // pegado a "developer_instructions = ". El resto: frontmatter + cuerpo.
    private String composeAgent(String agent, String body, ProviderType type) {
        ArtifactFormat format = type == ProviderType.CODEX ? ArtifactFormat.CODEX_TOML : ArtifactFormat.MARKDOWN;
        return format.assemble(agent, body);
    }

    // <agente>.<ext> -> shared/<agente>-body.md (los "_" del nombre se corresponden con "-")
    private String readAgentBody(String agentFileName) throws IOException {
        String baseName = agentFileName.substring(0, agentFileName.lastIndexOf('.')).replace('_', '-');
        String bodyResource = TEMPLATES_RESOURCE + "/" + baseName + "-body.md";

        Optional<String> body = ClasspathResources.readText(bodyResource);
        if(body.isEmpty())
            log.error("Resource '{}' not found", bodyResource);
        return body.orElse(null);
    }

    private void loadSkills(Path providerDir, ProviderType type) {
        try {
            Optional<List<String>> skills = ClasspathResources.listDirectories(SKILLS_RESOURCE);
            if(skills.isEmpty()) {
                log.error("Resource '{}' not found", SKILLS_RESOURCE);
                return;
            }
            copySkills(skills.get(), providerDir.resolve("skills"), type);
        }catch(IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void copySkills(List<String> skills, Path destRoot, ProviderType type) throws IOException {
        Files.createDirectories(destRoot);

        for(String name : skills) {
            Path target = destRoot.resolve(name);

            if(Files.exists(target)) {
                System.out.println("Skill '" + name + "' exists");
                continue;
            }

            ClasspathResources.copyTreeIfAbsent(SKILLS_RESOURCE + "/" + name, target);
            if(type == ProviderType.CODEX)
                copyCodexHelper(target);
            System.out.println("Copied skill '" + name + "'");
        }
    }

    private void copyCodexHelper(Path skillDir) throws IOException {
        Path agents = skillDir.resolve("agents");
        if(Files.exists(agents))
            return;

        try(InputStream helper = ConfigureProvider.class.getResourceAsStream(HELPER_DIR + "/openai.yaml")) {
            if(helper == null) {
                log.error("Resource '{}/openai.yaml' not found", HELPER_DIR);
                return;
            }
            Files.createDirectories(agents);
            Files.copy(helper, agents.resolve("openai.yaml"));
        }
    }

    private void copyCodexProfile(Path providerDir) {
        Path target = providerDir.resolve(CODEX_PROFILE);
        if(Files.exists(target)) {
            System.out.println("Profile '" + CODEX_PROFILE + "' exists");
            return;
        }

        try(InputStream profile = ConfigureProvider.class.getResourceAsStream(PROFILES_RESOURCE + "/" + CODEX_PROFILE)) {
            if(profile == null) {
                log.error("Resource '{}/{}' not found", PROFILES_RESOURCE, CODEX_PROFILE);
                return;
            }
            Files.copy(profile, target);
            System.out.println("Copied profile '" + CODEX_PROFILE + "'");
        } catch(IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    void manageConfiguration(Provider provider, boolean global)
    {
        ProviderType type = provider.getType();

        if(global)
        {
            // Skills
            loadSkills(provider.getPath(), type);

            // Agents
            loadAgents(provider.getPath(), type);

            // Hooks
            if(type == ProviderType.CLAUDE) {

            }

            // Profiles (Codex)
            if(type == ProviderType.CODEX)
                copyCodexProfile(provider.getPath());
        }
    }
}
