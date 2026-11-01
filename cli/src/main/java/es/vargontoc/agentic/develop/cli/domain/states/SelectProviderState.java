package es.vargontoc.agentic.develop.cli.domain.states;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.vargontoc.agentic.develop.cli.domain.Provider;
import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.validators.EnvironmentValidator;

public class SelectProviderState implements TerminalState {
    
    static final  Logger log = LoggerFactory.getLogger(SelectProviderState.class);

    String errorMessage;
    TerminalState parent;

    public SelectProviderState(TerminalState parent){
        this.parent = parent;
    }


    static List<Provider> providers = List.of(
        new Provider(ProviderType.CLAUDE, EnvironmentValidator.hasProvider(ProviderType.CLAUDE)),
        new Provider(ProviderType.OPENCODE, EnvironmentValidator.hasProvider(ProviderType.OPENCODE)),
        new Provider(ProviderType.CODEX, EnvironmentValidator.hasProvider(ProviderType.CODEX)),
        new Provider(ProviderType.GEMINI, EnvironmentValidator.hasProvider(ProviderType.GEMINI))
    ).stream().filter(Provider::isAvailable).toList();

    @Override
    public String printMenu() {
        StringBuilder sb = new StringBuilder();

        if(errorMessage != null)
            sb.append("[ERROR] ").append(errorMessage).append("\n");


        sb.append("Seleccione el proveedor: (Ej: 1,2)\n");

        for(int i = 0; i < providers.size(); i++){
            String elementSelected = providers.get(i).isSelected() ? "[" + (i + 1) + "]" : "" + (i + 1);
            sb.append(elementSelected).append(". ").append(providers.get(i).getName()).append("\n");
        }
        
        sb.append("\n\n0. Cancelar\n");
        if(providers.stream().anyMatch(x -> x.isSelected()))
            sb.append("\nPulse ENTER para confirmar selección");

        return sb.toString();

    }
    @Override
    public TerminalState handleChoice(String choice) {
        errorMessage = null;
        if(choice.isEmpty() && providers.stream().anyMatch(x -> x.isSelected()))
        {
            for(Provider p : providers) {
                if(p.isSelected()) {
                    manage(p);
                }
            }

            return parent;
        }

        if("0".equals(choice))
            return parent;

        if(!choice.isEmpty()) {
            providers.forEach(e -> e.setSelected(false));
            String[] parts = choice.split(",");
            try {
                for(String part : parts){
                    if(part.isEmpty()) continue;
                    int index = Integer.parseInt(part.trim()) - 1;
                    providers.get(index).setSelected(true);
                }
            }catch(NumberFormatException e){
                log.error(e.getMessage());
                errorMessage = "Selección inválida";
            }
        }

        return this;
    }
    private void manage(Provider p) {
        switch(p.getType()){
            case CLAUDE -> log.info("Configurando Claude");
            case OPENCODE -> log.info("Configurando OpenCode");
            case CODEX -> log.info("Configurando Codex");
            case GEMINI -> log.info("Configurando Gemini");
        }
    }
}
