package es.vargontoc.agentic.viewer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ErrorPresenterTest {

    @Test
    void conservaCodigoRutaYMensaje() {
        CliClientException error = new CliClientException(
            "conflict.artifact.modified_externally", "C:/agente.md", "Cambió fuera", 1);

        ErrorPresenter.ErrorNotice notice = ErrorPresenter.present(error);

        assertEquals("conflict.artifact.modified_externally", notice.code());
        assertEquals("C:/agente.md", notice.path());
        assertEquals("Cambió fuera", notice.message());
        assertTrue(notice.action().contains("Recargar"));
    }

    @Test
    void traduceErroresDelCliente() {
        ErrorPresenter.ErrorNotice notice = ErrorPresenter.present(
            new CliClientException("client.unsupported_schema", null, "Versión 2", -1));

        assertTrue(notice.action().contains("Actualizar"));
    }

    @Test
    void usaUnaAccionGenericaParaCodigosDesconocidos() {
        ErrorPresenter.ErrorNotice notice = ErrorPresenter.present(
            new CliClientException("codigo.desconocido", null, "Mensaje", 1));

        assertTrue(notice.action().contains("logs"));
    }
}
