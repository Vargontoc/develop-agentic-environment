package es.vargontoc.agentic.develop.cli.domain.validators;

import java.nio.file.Path;

public final class EnvironmentValidator {
    
    static final String CLAUDE_DIR = ".claude";
    static final String OPEN_CODE_DIR = ".config\\opencode";
    static final String CODEX_DIR = ".codex";
    static final String GEMINI_DIR = ".gemini";
    static final String USERPROFILE_DIR = "USERPROFILE";

    private EnvironmentValidator(){}


    public static boolean hasClaude() {
        String userProfile = System.getenv(USERPROFILE_DIR);
        if(userProfile == null || userProfile.isEmpty()) return false;

        Path claudePath = Path.of(userProfile, CLAUDE_DIR);
        return claudePath.toFile().exists();
    }

    public static boolean hasOpenCode() {
        String userProfile = System.getenv(USERPROFILE_DIR);
        if(userProfile == null || userProfile.isEmpty()) return false;
        Path openCodePath = Path.of(userProfile, OPEN_CODE_DIR);
        return openCodePath.toFile().exists();
    }
    
    public static boolean hasCodex() {
        String userProfile = System.getenv(USERPROFILE_DIR);
        if(userProfile == null || userProfile.isEmpty()) return false;
        Path codexPath = Path.of(userProfile, CODEX_DIR);
        return codexPath.toFile().exists();
    }
    
    public static boolean hasGemini() {
        String userProfile = System.getenv(USERPROFILE_DIR);
        if(userProfile == null || userProfile.isEmpty()) return false;
        Path geminiPath = Path.of(userProfile, GEMINI_DIR);
        return geminiPath.toFile().exists();
    }

    public static boolean canRunApp(){
        return hasClaude() || hasOpenCode() || hasCodex() || hasGemini();
    }
 }
