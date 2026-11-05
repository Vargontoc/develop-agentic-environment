package es.vargontoc.agentic.viewer.cli;

import java.nio.charset.StandardCharsets;

/**
 * CLI falso para las pruebas del proceso: no depende del jar real.
 */
public final class FakeCli {

    private FakeCli() {}

    public static void main(String[] args) throws Exception {
        String command = args.length == 0 ? "" : args[0];
        switch (command) {
            case "ok" -> System.out.print("{\"ok\":true}");
            case "fail" -> {
                System.err.print("fallo");
                System.exit(1);
            }
            case "echo" -> System.out.print(new String(System.in.readAllBytes(), StandardCharsets.UTF_8));
            case "sleep" -> Thread.sleep(5000);
            default -> System.exit(2);
        }
    }
}
