package es.vargontoc.agentic.develop.cli.application;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.vargontoc.agentic.develop.cli.domain.Provider;
import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.Technician;
import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.infrastructure.resources.ClasspathResources;

public class CreateModule {

    private static final Logger log = LoggerFactory.getLogger(CreateModule.class);

    private static final String SKILLS_RESOURCE = "/project/skills";
    private static final String AGENTS_RESOURCE = "/project/agents/";
    private static final String TEAM_FILE = ".sdd/team.md";
    private static final String MODULES_HEADER = "## Módulos";

    private static final int MIN_NAME_LENGTH = 3;
    private static final Pattern NAME_CHARS = Pattern.compile("[A-Za-z-]+");

    /**
     * Solo letras ASCII y guiones simples (nunca al inicio, al final ni seguidos), mínimo 3 caracteres.
     * Devuelve el nombre normalizado a minúsculas.
     */
    public String normalizeName(String raw) {
        String name = raw == null ? "" : raw.trim();

        if(!NAME_CHARS.matcher(name).matches())
            throw new DomainException(AppErrorCode.MODULE_NAME_INVALID_CHARS);
        if(name.startsWith("-") || name.endsWith("-") || name.contains("--"))
            throw new DomainException(AppErrorCode.MODULE_NAME_INVALID_HYPHENS);
        if(name.length() < MIN_NAME_LENGTH)
            throw new DomainException(AppErrorCode.MODULE_NAME_TOO_SHORT);

        return name.toLowerCase(Locale.ROOT);
    }

    /**
     * Por cada proveedor copia las skills del proyecto (sin sobrescribir) y genera, por técnico,
     * su router y su especialista del módulo. Nada existente se sobrescribe.
     */
    public ModuleResult execute(Path project, String module, List<Technician> technicians, List<Provider> providers) {
        ModuleResult result = new ModuleResult();

        registerInTeam(project, module, result);

        for(Provider provider : providers) {
            ProviderType type = provider.getType();
            if(!type.supportsProject()) {
                warn(result, type + ": aún no se soporta a nivel de proyecto, se omite");
                continue;
            }

            Path providerDir = project.resolve(type.projectDir());
            copySkills(providerDir, type, result);

            for(Technician technician : technicians) {
                String slug = technician.slug();
                // El router es único por técnico y proveedor; el especialista depende del módulo
                generateAgent(providerDir, type, module, slug + "-router", slug + "-router-template", slug + "-router", result);
                generateAgent(providerDir, type, module, slug, slug + "-template", slug + "-" + module, result);
            }
        }
        return result;
    }

    // Añade "- <modulo> | - | -" al final de la sección "## Módulos" de .sdd/team.md, sin duplicar
    private void registerInTeam(Path project, String module, ModuleResult result) {
        Path team = project.resolve(TEAM_FILE);
        String label = TEAM_FILE + ": módulo '" + module + "'";

        try {
            if(!Files.exists(team)) {
                warn(result, TEAM_FILE + " no existe, el módulo no se ha registrado");
                return;
            }

            String content = Files.readString(team, StandardCharsets.UTF_8);
            String newline = content.contains("\r\n") ? "\r\n" : "\n";
            List<String> lines = new ArrayList<>(List.of(content.split("\\R", -1)));

            int header = lines.indexOf(MODULES_HEADER);
            if(header < 0) {
                warn(result, TEAM_FILE + " no tiene la sección '" + MODULES_HEADER + "', el módulo no se ha registrado");
                return;
            }

            int end = header + 1;
            int lastEntry = -1;
            while(end < lines.size() && !lines.get(end).startsWith("## ")) {
                if(lines.get(end).startsWith("-"))
                    lastEntry = end;
                end++;
            }

            Pattern existing = Pattern.compile("-\\s*" + Pattern.quote(module) + "\\s*\\|.*");
            for(int i = header + 1; i < end; i++) {
                if(existing.matcher(lines.get(i).trim()).matches()) {
                    result.skipped().add(label + " (ya registrado)");
                    return;
                }
            }

            String entry = "- " + module + " | - | -";
            if(lastEntry >= 0) {
                lines.add(lastEntry + 1, entry);
            } else {
                // Sin módulos: tras la línea en blanco que sigue al encabezado, dejando otra antes de la siguiente sección
                int at = header + 1;
                if(at < lines.size() && lines.get(at).isBlank())
                    at++;
                lines.add(at, entry);
                if(at + 1 < lines.size() && !lines.get(at + 1).isBlank())
                    lines.add(at + 1, "");
            }

            Files.writeString(team, String.join(newline, lines), StandardCharsets.UTF_8);
            result.created().add(label + " registrado");
        } catch(IOException e) {
            log.error(e.getMessage(), e);
            throw new DomainException(AppErrorCode.MODULE_WRITE_FAILED, e);
        }
    }

    private void copySkills(Path providerDir, ProviderType type, ModuleResult result) {
        try {
            Optional<ClasspathResources.CopyStats> stats =
                ClasspathResources.copyTreeIfAbsent(SKILLS_RESOURCE, providerDir.resolve("skills"));
            if(stats.isEmpty()) {
                log.error("Resource '{}' not found", SKILLS_RESOURCE);
                throw new DomainException(AppErrorCode.MODULE_SKILLS_NOT_FOUND);
            }
            if(stats.get().copied() > 0)
                result.created().add(type + ": skills (" + stats.get().copied() + " ficheros copiados)");
            if(stats.get().skipped() > 0)
                result.skipped().add(type + ": skills (" + stats.get().skipped() + " ficheros ya existían)");
        } catch(IOException e) {
            log.error(e.getMessage(), e);
            throw new DomainException(AppErrorCode.PROJECT_COPY_FAILED, e);
        }
    }

    // Une cabecera del proveedor + plantilla compartida y sustituye los slugs
    private void generateAgent(Path providerDir, ProviderType type, String module,
                               String headerName, String templateName, String targetName, ModuleResult result) {
        Path target = providerDir.resolve("agents").resolve(targetName + ".md");
        String label = type + ": " + type.projectDir() + "/agents/" + targetName + ".md";

        if(Files.exists(target)) {
            result.skipped().add(label + " (ya existe)");
            return;
        }

        try {
            Optional<String> header = ClasspathResources.readText(AGENTS_RESOURCE + type.resourceDir() + "/" + headerName + ".md");
            Optional<String> template = ClasspathResources.readText(AGENTS_RESOURCE + "shared/" + templateName + ".md");
            if(header.isEmpty() || template.isEmpty()) {
                String missing = header.isEmpty()
                    ? AGENTS_RESOURCE + type.resourceDir() + "/" + headerName + ".md"
                    : AGENTS_RESOURCE + "shared/" + templateName + ".md";
                warn(result, label + ": no existe el recurso " + missing);
                return;
            }

            String content = (header.get().stripTrailing() + "\n\n" + template.get().strip() + "\n")
                .replace("{module}", module)
                .replace("{slug-path-agents}", type.projectDir() + "/agents")
                .replace("{slug-provider}", type.projectDir());

            Files.createDirectories(target.getParent());
            Files.writeString(target, content, StandardCharsets.UTF_8);
            result.created().add(label);
        } catch(IOException e) {
            log.error(e.getMessage(), e);
            throw new DomainException(AppErrorCode.MODULE_WRITE_FAILED, e);
        }
    }

    private void warn(ModuleResult result, String message) {
        log.warn(message);
        result.warnings().add(message);
    }
}
