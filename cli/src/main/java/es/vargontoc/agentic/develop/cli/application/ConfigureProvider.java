package es.vargontoc.agentic.develop.cli.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.vargontoc.agentic.develop.cli.domain.Provider;

public class ConfigureProvider {

    private static final Logger log = LoggerFactory.getLogger(ConfigureProvider.class);

    public void execute(Provider provider) {
        switch(provider.getType()){
            case CLAUDE -> log.info("Configurando Claude");
            case OPENCODE -> log.info("Configurando OpenCode");
            case CODEX -> log.info("Configurando Codex");
            case GEMINI -> log.info("Configurando Gemini");
        }
    }
}
