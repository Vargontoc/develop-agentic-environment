package es.vargontoc.agentic.develop.cli.application;

import java.nio.file.Path;

public record ProjectStart(Path path, boolean alreadyConfigured) {
}
