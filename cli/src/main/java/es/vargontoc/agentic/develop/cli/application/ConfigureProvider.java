package es.vargontoc.agentic.develop.cli.application;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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

            // Hooks
        }
    }
}
