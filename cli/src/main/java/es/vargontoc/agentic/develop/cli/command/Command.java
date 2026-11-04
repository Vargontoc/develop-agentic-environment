package es.vargontoc.agentic.develop.cli.command;

import java.util.List;

/**
 * Un comando no interactivo. Nunca lee de la entrada estándar ni imprime: devuelve los datos serializables que irán
 * en el campo {@code data} del envelope, o lanza un error tipado.
 */
public interface Command {

    String name();

    String description();

    List<OptionSpec> options();

    /**
     * @return datos del resultado: mapas, listas, cadenas, números, booleanos o records de ellos.
     *         Las rutas deben ir como texto.
     */
    Object execute(ParsedArgs args);
}
