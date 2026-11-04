package es.vargontoc.agentic.develop.cli.command;

import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;

/** Mensajes comprensibles para los códigos de error de la aplicación. */
final class ErrorMessages {

    private ErrorMessages() {}

    static String of(AppErrorCode code) {
        return switch(code) {
            case INCOMPATIBLE_ENVIRONMENT -> "El entorno no es compatible: no se encuentra el directorio del usuario";
            case PROJECT_PATH_INVALID -> "La ruta del proyecto no es válida";
            case PROJECT_PATH_NOT_FOUND -> "La ruta del proyecto no existe";
            case PROJECT_PATH_NOT_DIRECTORY -> "La ruta del proyecto no es un directorio";
            case MODULE_NAME_INVALID_CHARS -> "El nombre del módulo solo admite letras (a-z) y guiones";
            case MODULE_NAME_INVALID_HYPHENS -> "El guion no puede ir al inicio, al final ni repetido";
            case MODULE_NAME_TOO_SHORT -> "El nombre del módulo debe tener al menos 3 caracteres";
            case MODULE_NAME_RESERVED -> "El nombre del módulo está reservado";
            case MODULE_NO_PROJECT -> "No hay un proyecto iniciado";
            case SDD_RESOURCE_NOT_FOUND -> "No se encontró la plantilla .sdd en la aplicación";
            case PROJECT_COPY_FAILED -> "No se pudieron copiar los ficheros al proyecto";
            case MODULE_SKILLS_NOT_FOUND -> "No se encontraron las skills del proyecto en la aplicación";
            case MODULE_WRITE_FAILED -> "No se pudo escribir un fichero del módulo";
            case ARTIFACT_NOT_FOUND -> "El artefacto no existe";
            case ARTIFACT_NOT_EDITABLE -> "El artefacto está dentro de la aplicación y no es editable";
            case ARTIFACT_NOT_CATALOGUED -> "El fichero no es un artefacto de la aplicación";
            case ARTIFACT_KIND_MISMATCH -> "El artefacto no es del tipo esperado (fichero o directorio)";
            case ARTIFACT_FORMAT_INVALID -> "El artefacto no tiene un formato reconocible";
            case RESOURCE_READ_FAILED -> "No se pudo leer un recurso de la aplicación";
            case PROVIDER_UNKNOWN -> "Proveedor desconocido";
            case PATH_OUT_OF_SCOPE -> "La ruta queda fuera del ámbito permitido";
            case TEXT_READ_FAILED -> "No se pudo leer el fichero";
            case TEXT_WRITE_FAILED -> "No se pudo escribir el fichero";
        };
    }
}
