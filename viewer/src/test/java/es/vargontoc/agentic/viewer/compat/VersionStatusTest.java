package es.vargontoc.agentic.viewer.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VersionStatusTest {

    @Test
    void compatibleCuandoElContratoCoincide() {
        VersionStatus status = VersionStatus.compatible("0.1.0", "1.0.0", 1);

        assertTrue(status.compatible());
        assertNull(status.warning());
        assertEquals("Visor 0.1.0 · CLI 1.0.0 · contrato 1", status.summary());
        assertTrue(status.aboutText().contains("Compatibilidad: correcta"));
    }

    @Test
    void avisaCuandoElContratoDelCliNoCoincide() {
        VersionStatus status = VersionStatus.mismatch("0.1.0", "2.0.0", 1, 2);

        assertFalse(status.compatible());
        assertEquals(VersionStatus.State.CONTRACT_MISMATCH, status.state());
        assertTrue(status.summary().contains("contrato 2"));
        assertTrue(status.summary().contains("visor espera 1"));
        assertTrue(status.warning().contains("no coincide"));
        assertTrue(status.warning().contains("Actualiza el visor o el CLI"));
    }

    @Test
    void avisaCuandoNoSePuedeComprobarElCli() {
        VersionStatus status = VersionStatus.unavailable("0.1.0", 1, "no se encontró el jar");

        assertFalse(status.compatible());
        assertEquals("Visor 0.1.0 · CLI no disponible", status.summary());
        assertTrue(status.warning().contains("no se encontró el jar"));
        assertTrue(status.aboutText().contains("Contrato cli-json: desconocido"));
    }

    @Test
    void usaValoresLegiblesSiFaltanLasVersiones() {
        VersionStatus status = VersionStatus.compatible(null, " ", 1);

        assertEquals("desconocida", status.viewerVersion());
        assertEquals("desconocida", status.cliVersion());
        assertTrue(status.summary().contains("Visor desconocida"));
    }
}
