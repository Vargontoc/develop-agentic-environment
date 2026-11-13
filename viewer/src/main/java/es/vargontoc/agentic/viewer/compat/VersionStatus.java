package es.vargontoc.agentic.viewer.compat;

/**
 * Versiones del visor y del CLI y compatibilidad del contrato {@code cli-json}.
 *
 * <p>El visor compara la {@code schemaVersion} del envelope con la que entiende.
 * Así se avisa antes de que las operaciones fallen con {@code client.unsupported_schema}.</p>
 */
public record VersionStatus(String viewerVersion, String cliVersion, int expectedContract,
                            int cliContract, State state, String detail) {

    public enum State {
        /** Versiones conocidas y contrato coincidente. */
        COMPATIBLE,
        /** El CLI habla una versión de contrato distinta de la del visor. */
        CONTRACT_MISMATCH,
        /** No se pudo ejecutar o leer la versión del CLI. */
        CLI_UNAVAILABLE
    }

    public VersionStatus {
        viewerVersion = blankTo(viewerVersion, "desconocida");
        cliVersion = blankTo(cliVersion, "desconocida");
        detail = detail == null ? "" : detail;
    }

    public static VersionStatus compatible(String viewerVersion, String cliVersion, int contract) {
        return new VersionStatus(viewerVersion, cliVersion, contract, contract, State.COMPATIBLE, "");
    }

    public static VersionStatus mismatch(String viewerVersion, String cliVersion, int expected, int actual) {
        return new VersionStatus(viewerVersion, cliVersion, expected, actual, State.CONTRACT_MISMATCH, "");
    }

    public static VersionStatus unavailable(String viewerVersion, int expected, String detail) {
        return new VersionStatus(viewerVersion, "desconocida", expected, -1, State.CLI_UNAVAILABLE, detail);
    }

    public boolean compatible() {
        return state == State.COMPATIBLE;
    }

    /** Línea compacta para la barra de estado y la bienvenida. */
    public String summary() {
        return switch (state) {
            case COMPATIBLE -> "Visor " + viewerVersion + " · CLI " + cliVersion
                + " · contrato " + expectedContract;
            case CONTRACT_MISMATCH -> "Visor " + viewerVersion + " · CLI " + cliVersion
                + " · contrato " + cliContract + " (visor espera " + expectedContract + ")";
            case CLI_UNAVAILABLE -> "Visor " + viewerVersion + " · CLI no disponible";
        };
    }

    /** Aviso legible cuando algo no cuadra; {@code null} si todo es compatible. */
    public String warning() {
        return switch (state) {
            case COMPATIBLE -> null;
            case CONTRACT_MISMATCH -> "El contrato del CLI (v" + cliContract
                + ") no coincide con el del visor (v" + expectedContract
                + "). Actualiza el visor o el CLI para que coincidan.";
            case CLI_UNAVAILABLE -> "No se pudo comprobar la versión del CLI"
                + (detail.isBlank() ? "." : ": " + detail)
                + " Revisa que viewer/cli/agentic-develop.jar esté actualizado.";
        };
    }

    /** Detalle multilínea para «Acerca de» y las ayudas. */
    public String aboutText() {
        StringBuilder text = new StringBuilder()
            .append("Visor: ").append(viewerVersion).append('\n')
            .append("CLI: ").append(cliVersion).append('\n')
            .append("Contrato cli-json: ");
        if (cliContract < 0) {
            text.append("desconocido");
        } else {
            text.append(cliContract);
        }
        text.append(" (el visor espera ").append(expectedContract).append(")\n")
            .append("Compatibilidad: ").append(switch (state) {
                case COMPATIBLE -> "correcta";
                case CONTRACT_MISMATCH -> "incompatible";
                case CLI_UNAVAILABLE -> "sin comprobar";
            });
        if (!detail.isBlank()) {
            text.append("\nDetalle: ").append(detail);
        }
        return text.toString();
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
