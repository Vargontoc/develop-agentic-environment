package es.vargontoc.agentic.develop.cli.domain.validators;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import es.vargontoc.agentic.develop.cli.domain.ProviderType;

public final class EnvironmentValidator {
    
    static final String CLAUDE_DIR = ".claude";
    static final String OPEN_CODE_DIR = ".config\\opencode";
    static final String CODEX_DIR = ".codex";
    static final String GEMINI_DIR = ".gemini";
    static final String USERPROFILE_DIR = "USERPROFILE";


    static Map<ProviderType, Path> globalPaths = Map.of(
        ProviderType.CLAUDE, Path.of(System.getenv(USERPROFILE_DIR), CLAUDE_DIR),
        ProviderType.OPENCODE, Path.of(System.getenv(USERPROFILE_DIR), OPEN_CODE_DIR),
        ProviderType.CODEX, Path.of(System.getenv(USERPROFILE_DIR), CODEX_DIR),
        ProviderType.GEMINI, Path.of(System.getenv(USERPROFILE_DIR), GEMINI_DIR)
    );

    private EnvironmentValidator(){}


    public static boolean hasProvider(ProviderType type) {
        String userProfile = System.getenv(USERPROFILE_DIR);
        if(userProfile == null || userProfile.isEmpty()) return false;

        return globalPaths.get(type).toFile().exists();
    }

    public static Path getPath(ProviderType type) {
        return globalPaths.get(type);
    }


    public static boolean canRunApp(){
        return List.of(ProviderType.values()).stream().anyMatch(EnvironmentValidator::hasProvider);
    }
}
