package es.vargontoc.agentic.develop.cli.ui.dtos;

import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.Technician;

public record TerminalRequest(TerminalRequestType type, String module, Technician[] techs, ProviderType[] providers) {
    
}
