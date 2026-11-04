package es.vargontoc.agentic.develop.cli.domain.artifact;

import java.nio.file.Path;
import java.util.List;

import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.format.ArtifactFormat;

/**
 * Descripción editable de un artefacto instalado.
 *
 * @param format     null en skills y perfiles
 * @param module     módulo de un especialista de proyecto; null en el resto
 * @param header     cabecera tal cual está en el fichero; vacía en skills y perfiles
 * @param body       cuerpo del artefacto instalado (en un especialista, con el nombre de su módulo)
 * @param sharedBody cuerpo compartido del grupo, con el placeholder del módulo; null si el artefacto no tiene metadatos
 * @param files      en una skill, sus ficheros relativos al directorio ({@code SKILL.md} y de apoyo); vacío en el resto
 */
public record ArtifactDescription(
        ProviderType provider,
        ArtifactScope scope,
        ArtifactKind kind,
        String name,
        Path path,
        ArtifactFormat format,
        String module,
        String header,
        String body,
        String sharedBody,
        String hash,
        ArtifactStatus status,
        boolean modifiedExternally,
        List<String> files) {

    public ArtifactDescription {
        files = List.copyOf(files);
    }
}
