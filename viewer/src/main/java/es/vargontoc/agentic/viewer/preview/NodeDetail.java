package es.vargontoc.agentic.viewer.preview;

import java.util.List;

/** Contenido estructurado para la ficha de un módulo o un técnico. */
public record NodeDetail(String heading, String subtitle, List<Section> sections) {

    public NodeDetail {
        sections = sections == null ? List.of() : List.copyOf(sections);
    }

    public record Section(String title, List<Item> items) {
        public Section {
            items = items == null ? List.of() : List.copyOf(items);
        }
    }

    /**
     * @param title   etiqueta principal
     * @param detail  explicación breve; puede ser null
     * @param status  estado legible (vinculado, pendiente, personalizado…); puede ser null
     * @param path    ruta en disco; puede ser null
     */
    public record Item(String title, String detail, String status, String path) {}
}
