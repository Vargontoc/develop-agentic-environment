package es.vargontoc.agentic.develop.cli.command;

import java.util.List;
import java.util.function.Supplier;

import es.vargontoc.agentic.develop.cli.Services;

/**
 * {@code describe --path <ruta> [--project <ruta>]}: cabecera, cuerpo, hash y estado de un artefacto instalado.
 * {@code --project} hace falta para describir artefactos de proyecto. Solo lee.
 */
final class DescribeCommand implements Command {

    private static final String PATH = "path";

    private final Supplier<Services> services;

    DescribeCommand(Supplier<Services> services) {
        this.services = services;
    }

    @Override
    public String name() {
        return "describe";
    }

    @Override
    public String description() {
        return "Describe un artefacto instalado: cabecera, cuerpo, hash y estado";
    }

    @Override
    public List<OptionSpec> options() {
        return List.of(OptionSpec.value(PATH, true, "Ruta del agente o de la skill"), ProjectOption.spec(false));
    }

    @Override
    public Object execute(ParsedArgs args) {
        Services s = services.get();
        return Views.of(s.descriptor().describe(args.required(PATH), ProjectOption.resolve(s, args)));
    }
}
