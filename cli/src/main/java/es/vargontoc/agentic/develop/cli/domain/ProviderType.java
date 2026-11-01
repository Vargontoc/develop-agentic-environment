package es.vargontoc.agentic.develop.cli.domain;

public enum ProviderType {
    CLAUDE("Claude"), CODEX("Codex"), GEMINI("Gemini"), OPENCODE("OpenCode");

    final String name;

    ProviderType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
