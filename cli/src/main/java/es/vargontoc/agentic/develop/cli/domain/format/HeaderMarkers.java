package es.vargontoc.agentic.develop.cli.domain.format;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.PathDomainException;

/**
 * Sustituye en la cabecera de un recurso los marcadores que dependen del sistema operativo (DEC-012), de modo que
 * cada agente tiene un único fichero de recurso y cada instalación recibe la cabecera de su sistema.
 * <ul>
 *   <li>{@code {shell}}: {@code powershell} en Windows, {@code bash} en Linux</li>
 *   <li>{@code {home}}: {@code $env:USERPROFILE} en Windows, {@code $HOME} en Linux</li>
 *   <li>{@code {os}}: {@code windows} o {@code linux}</li>
 *   <li>{@code {hook:<script> <argumentos>}}: la orden completa que lanza el script de la carpeta {@code hooks} del
 *       proveedor: un {@code & "...\hooks\<script>.bat" <argumentos>; exit $LASTEXITCODE} bajo {@code $env:USERPROFILE}
 *       en Windows y un {@code "$HOME/.../hooks/<script>.sh" <argumentos>} en Linux</li>
 * </ul>
 * Cualquier otra llave se deja intacta. Un {@code {hook:...}} vacío es un error del recurso.
 */
public final class HeaderMarkers {

    private static final Pattern MARKER = Pattern.compile("\\{(shell|home|os)\\}|\\{hook:([^}]*)\\}");

    private HeaderMarkers() {}

    /** @throws PathDomainException con {@code HEADER_MARKER_INVALID} y el marcador afectado como ubicación */
    public static String render(String header, TargetOs os, ProviderType provider) {
        Matcher matcher = MARKER.matcher(header);
        StringBuilder out = new StringBuilder();
        while(matcher.find()) {
            String replacement = matcher.group(2) != null
                ? hook(matcher.group(0), matcher.group(2).strip(), os, provider)
                : simple(matcher.group(1), os);
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String simple(String name, TargetOs os) {
        return switch(name) {
            case "shell" -> os == TargetOs.WINDOWS ? "powershell" : "bash";
            case "home" -> home(os);
            default -> os == TargetOs.WINDOWS ? "windows" : "linux";
        };
    }

    private static String home(TargetOs os) {
        return os == TargetOs.WINDOWS ? "$env:USERPROFILE" : "$HOME";
    }

    private static String hook(String marker, String spec, TargetOs os, ProviderType provider) {
        if(spec.isEmpty())
            throw new PathDomainException(AppErrorCode.HEADER_MARKER_INVALID, marker);

        int space = spec.indexOf(' ');
        String script = space < 0 ? spec : spec.substring(0, space);
        String args = space < 0 ? "" : " " + spec.substring(space + 1).strip();
        String dir = provider.globalDirName();

        if(os == TargetOs.WINDOWS)
            return "& \"" + home(os) + "\\" + dir.replace('/', '\\') + "\\hooks\\" + script + ".bat\"" + args
                + "; exit $LASTEXITCODE";
        return "\"" + home(os) + "/" + dir + "/hooks/" + script + ".sh\"" + args;
    }
}
