package es.vargontoc.agentic.develop.cli.application;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.vargontoc.agentic.develop.cli.domain.Provider;
import es.vargontoc.agentic.develop.cli.domain.ProviderType;

public class ConfigureProvider {

    private static final Logger log = LoggerFactory.getLogger(ConfigureProvider.class);

    private static final String SKILLS_RESOURCE = "/global/skills";
    private static final String HELPER_DIR = "/global/helper";
    private static final String AGENTS_RESOURCE = "/global/agents";
    private static final String TEMPLATES_RESOURCE = "/global/agents/shared";
    private static final String PROFILES_RESOURCE = "/global/profiles";
    private static final String CODEX_PROFILE = "sdd.config.toml";

    private ProviderType type;
    public void execute(Provider provider, boolean global) {
        
        type = provider.getType();
        switch(provider.getType()){
            case CLAUDE -> System.out.println("Configurando Claude");
            case OPENCODE -> System.out.println("Configurando OpenCode");
            case CODEX -> System.out.println("Configurando Codex");
            case GEMINI -> System.out.println("Configurando Gemini");
        }
        manageConfiguration(provider, global);
    }


    private String getDirectoryByType() {
        return switch(type) {
            case CLAUDE -> "/claude";
            case OPENCODE -> "/opencode";
            case CODEX -> "/codex";
            case GEMINI -> "/gemini";
        };
    }

    private void loadAgents(Path providerDir){
        String realPath = AGENTS_RESOURCE +  getDirectoryByType(); 

        URL resource = ConfigureProvider.class.getResource(realPath);
        if(resource == null) {
            log.error("Resource '{}' not found", realPath);
            return;
        }

        try {
            URI uri = resource.toURI();
            Path destRoot = providerDir.resolve("agents");
            if("jar".equals(uri.getScheme())) {
                try(FileSystem fs = FileSystems.newFileSystem(uri, Map.of())) {
                    createAgents(fs.getPath(realPath), destRoot);
                }
            }else {
                createAgents(Path.of(uri), destRoot);
            }
        }catch(URISyntaxException | IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void createAgents(Path agentsDir, Path destRoot) throws IOException {
        Files.createDirectories(destRoot);

        List<Path> agents;
        try(Stream<Path> files = Files.list(agentsDir)) {
            agents = files.filter(Files::isRegularFile).toList();
        }

        for(Path agent : agents) {
            String fileName = agent.getFileName().toString();
            Path target = destRoot.resolve(fileName);

            if(Files.exists(target)) {
                System.out.println("Agent '" + fileName + "' exists");
                continue;
            }

            String body = readAgentBody(fileName);
            if(body == null)
                continue;

            Path temp = Files.createTempFile("agent-", ".md");
            try {
                Files.writeString(temp, composeAgent(Files.readString(agent), body));
                Files.copy(temp, target);
                System.out.println("Copied agent '" + fileName + "'");
            } finally {
                Files.deleteIfExists(temp);
            }
        }
    }

    // Codex: el cuerpo se añade tal cual entre comillas triples (cadena multilínea TOML),
    // pegado a "developer_instructions = ". El resto: frontmatter + cuerpo.
    private String composeAgent(String agent, String body) {
        if(type == ProviderType.CODEX)
            return agent + "\"\"\"" + body + "\"\"\"";
        return agent.stripTrailing() + "\n\n" + body;
    }

    // <agente>.<ext> -> shared/<agente>-body.md (los "_" del nombre se corresponden con "-")
    private String readAgentBody(String agentFileName) throws IOException {
        String baseName = agentFileName.substring(0, agentFileName.lastIndexOf('.')).replace('_', '-');
        String bodyResource = TEMPLATES_RESOURCE + "/" + baseName + "-body.md";

        try(InputStream body = ConfigureProvider.class.getResourceAsStream(bodyResource)) {
            if(body == null) {
                log.error("Resource '{}' not found", bodyResource);
                return null;
            }
            return new String(body.readAllBytes(), StandardCharsets.UTF_8);
        }
    }



    private void loadSkills(Path providerDir)
    {
        URL resource = ConfigureProvider.class.getResource(SKILLS_RESOURCE);
        if(resource == null) {
            log.error("Resource '{}' not found", SKILLS_RESOURCE);
            return;
        }

        try {
            URI uri = resource.toURI();
            Path destRoot = providerDir.resolve("skills");

            if("jar".equals(uri.getScheme())) {
                try(FileSystem fs = FileSystems.newFileSystem(uri, Map.of())) {
                    copySkills(fs.getPath(SKILLS_RESOURCE), destRoot);
                }
            }else {
                copySkills(Path.of(uri), destRoot);
            }
        }catch(URISyntaxException | IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void copySkills(Path srcRoot, Path destRoot) throws IOException {
        Files.createDirectories(destRoot);

        List<Path> skills;
        try(Stream<Path> list = Files.list(srcRoot)) {
            skills = list.filter(Files::isDirectory).toList();
        }

        for(Path skill : skills) {
            String name = skillName(skill);
            Path target = destRoot.resolve(name);

            if(Files.exists(target)) {
                System.out.println("Skill '" + name + "' exists");
                continue;
            }

            copyTree(skill, target);
            if(type == ProviderType.CODEX)
                copyCodexHelper(target);
            System.out.println("Copied skill '" + name + "'");
        }
    }

    private void copyTree(Path src, Path dest) throws IOException {
        try(Stream<Path> files = Files.walk(src)) {
            for(Path file : files.toList()) {
                Path target = dest.resolve(src.relativize(file).toString());
                if(Files.isDirectory(file))
                    Files.createDirectories(target);
                else
                    Files.copy(file, target);
            }
        }
    }

    private void copyCodexHelper(Path skillDir) throws IOException {
        Path agents = skillDir.resolve("agents");
        if(Files.exists(agents))
            return;

        try(InputStream helper = ConfigureProvider.class.getResourceAsStream(HELPER_DIR + "/openai.yml")) {
            if(helper == null) {
                log.error("Resource '{}/openai.yml' not found", HELPER_DIR);
                return;
            }
            Files.createDirectories(agents);
            Files.copy(helper, agents.resolve("openai.yml"));
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

    // En un jar, el nombre de una carpeta puede terminar en "/"
    private String skillName(Path skill) {
        String name = skill.getFileName().toString();
        return name.endsWith("/") ? name.substring(0, name.length() - 1) : name;
    }



    void manageConfiguration(Provider provider, boolean global)
    {
        if(global)
        {
            // Skills
            loadSkills(provider.getPath());

            // Agents
            loadAgents(provider.getPath());

            // Hooks
            if(type == ProviderType.CLAUDE) {

            }

            // Profiles (Codex)
            if(type == ProviderType.CODEX)
                copyCodexProfile(provider.getPath());
        }
    }
}
