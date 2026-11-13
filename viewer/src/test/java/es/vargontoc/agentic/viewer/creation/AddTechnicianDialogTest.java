package es.vargontoc.agentic.viewer.creation;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * Pruebas de la lógica previa al diálogo de añadir técnicos: no debe abrirse
 * ni devolver resultado cuando no hay módulos donde añadir.
 */
class AddTechnicianDialogTest {

    @Test
    void showReturnsEmptyWithNoModules() {
        Optional<AddTechnicianDialog.Result> result =
            AddTechnicianDialog.show(null, List.of(), List.of("claude"));
        assertTrue(result.isEmpty(), "Sin módulos no debe devolver resultado");
    }

    @Test
    void showReturnsEmptyWithNullModules() {
        Optional<AddTechnicianDialog.Result> result =
            AddTechnicianDialog.show(null, null, List.of("claude"));
        assertTrue(result.isEmpty(), "Con módulos nulos no debe devolver resultado");
    }

    @Test
    void resultCarriesSelectedModule() {
        AddTechnicianDialog.Result result =
            new AddTechnicianDialog.Result("auth", List.of("analista"), List.of("claude"));

        assertEquals("auth", result.module(), "El módulo seleccionado debe conservarse");
        assertEquals(List.of("analista"), result.technicians());
        assertEquals(List.of("claude"), result.providers());
    }
}
