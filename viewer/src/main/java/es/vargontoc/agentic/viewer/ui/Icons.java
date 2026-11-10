package es.vargontoc.agentic.viewer.ui;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.transform.Scale;

/**
 * Iconos de trazo para la interfaz. Los trazados proceden de
 * <a href="https://lucide.dev">Lucide</a> (licencia ISC) y se dibujan como
 * {@link SVGPath} de JavaFX: son vectoriales, escalan sin perder nitidez y se
 * colorean desde CSS con {@code -fx-stroke}.
 */
public final class Icons {

    /** Iconos disponibles en la aplicación. */
    public enum Icon {
        AGENT("""
            M12 8V4H8
            M6 8h12a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2z
            M2 14h2
            M20 14h2
            M15 13v2
            M9 13v2
            """),
        SKILL("""
            M11.017 2.814a1 1 0 0 1 1.966 0l1.051 5.558a2 2 0 0 0 1.594 1.594l5.558 1.051a1 1 0 0 1 0 1.966l-5.558 1.051a2 2 0 0 0-1.594 1.594l-1.051 5.558a1 1 0 0 1-1.966 0l-1.051-5.558a2 2 0 0 0-1.594-1.594l-5.558-1.051a1 1 0 0 1 0-1.966l5.558-1.051a2 2 0 0 0 1.594-1.594z
            M20 2v4
            M22 4h-4
            M2 20a2 2 0 1 0 4 0a2 2 0 1 0-4 0
            """),
        HOOK("""
            M18 16.98h-5.99c-1.1 0-1.95.94-2.48 1.9A4 4 0 0 1 2 17c.01-.7.2-1.4.57-2
            M6 17l3.13-5.78c.53-.97.1-2.18-.5-3.1a4 4 0 1 1 6.89-4.06
            M12 6l3.13 5.73C15.66 12.7 16.9 13 18 13a4 4 0 0 1 0 8
            """),
        PROFILE("""
            M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z
            """),
        PROVIDER("""
            M12 22v-5
            M15 8V2
            M17 8a1 1 0 0 1 1 1v4a4 4 0 0 1-4 4h-4a4 4 0 0 1-4-4V9a1 1 0 0 1 1-1z
            M9 8V2
            """),
        MODULE("""
            M11 21.73a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73z
            M12 22V12
            M3.29 7L12 12L20.71 7
            M7.5 4.27l9 5.15
            """),
        TECHNICIAN("""
            M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2
            M16 3.128a4 4 0 0 1 0 7.744
            M22 21v-2a4 4 0 0 0-3-3.87
            M5 7a4 4 0 1 0 8 0a4 4 0 1 0-8 0
            """),
        SDD("""
            M12 5v16
            M20.001 19A2 2 0 0 0 22 17V5a2 2 0 0 0-1.999-2L16 3.002A5 5 0 0 0 12 5a5 5 0 0 0-4-2H4a2 2 0 0 0-2 2v12a2 2 0 0 0 1.999 2H8a5 5 0 0 1 4 2a5 5 0 0 1 4-2z
            """),
        GLOBE("""
            M2 12a10 10 0 1 0 20 0a10 10 0 1 0-20 0
            M12 2a14.5 14.5 0 0 0 0 20a14.5 14.5 0 0 0 0-20
            M2 12h20
            """),
        FOLDER("""
            M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2Z
            """),
        FOLDER_OPEN("""
            M6 14l1.5-2.9A2 2 0 0 1 9.24 10H20a2 2 0 0 1 1.94 2.5l-1.54 6a2 2 0 0 1-1.95 1.5H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h3.9a2 2 0 0 1 1.69.9l.81 1.2a2 2 0 0 0 1.67.9H18a2 2 0 0 1 2 2v2
            """),
        FOLDER_PLUS("""
            M12 10v6
            M9 13h6
            M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2Z
            """),
        FILE_TEXT("""
            M6 22a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h8a2.4 2.4 0 0 1 1.704.706l3.588 3.588A2.4 2.4 0 0 1 20 8v12a2 2 0 0 1-2 2z
            M14 2v5a1 1 0 0 0 1 1h5
            M10 9H8
            M16 13H8
            M16 17H8
            """),
        FILE_PLUS("""
            M6 22a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h8a2.4 2.4 0 0 1 1.704.706l3.588 3.588A2.4 2.4 0 0 1 20 8v12a2 2 0 0 1-2 2z
            M14 2v5a1 1 0 0 0 1 1h5
            M9 15h6
            M12 18v-6
            """),
        SAVE("""
            M15.2 3a2 2 0 0 1 1.4.6l3.8 3.8a2 2 0 0 1 .6 1.4V19a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z
            M17 21v-7a1 1 0 0 0-1-1H8a1 1 0 0 0-1 1v7
            M7 3v4a1 1 0 0 0 1 1h7
            """),
        REFRESH("""
            M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8
            M21 3v5h-5
            M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16
            M8 16H3v5
            """),
        SEARCH("""
            M21 21l-4.34-4.34
            M3 11a8 8 0 1 0 16 0a8 8 0 1 0-16 0
            """),
        PLUS("""
            M5 12h14
            M12 5v14
            """),
        TERMINAL("""
            M12 19h8
            M4 17l6-6-6-6
            """),
        PLAY("""
            M5 5a2 2 0 0 1 3.008-1.728l11.997 6.998a2 2 0 0 1 .003 3.458l-12 7A2 2 0 0 1 5 19z
            """),
        STOP("""
            M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z
            """),
        TRASH("""
            M10 11v6
            M14 11v6
            M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6
            M3 6h18
            M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2
            """),
        CLOSE("""
            M18 6L6 18
            M6 6l12 12
            """),
        SETTINGS("""
            M9.671 4.136a2.34 2.34 0 0 1 4.659 0 2.34 2.34 0 0 0 3.319 1.915 2.34 2.34 0 0 1 2.33 4.033 2.34 2.34 0 0 0 0 3.831 2.34 2.34 0 0 1-2.33 4.033 2.34 2.34 0 0 0-3.319 1.915 2.34 2.34 0 0 1-4.659 0 2.34 2.34 0 0 0-3.32-1.915 2.34 2.34 0 0 1-2.33-4.033 2.34 2.34 0 0 0 0-3.831A2.34 2.34 0 0 1 6.35 6.051a2.34 2.34 0 0 0 3.319-1.915
            M9 12a3 3 0 1 0 6 0a3 3 0 1 0-6 0
            """),
        LINK("""
            M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71
            M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71
            """),
        UNLINK("""
            M18.84 12.25l1.72-1.71h-.02a5.004 5.004 0 0 0-.12-7.07 5.006 5.006 0 0 0-6.95 0l-1.72 1.71
            M5.17 11.75l-1.71 1.71a5.004 5.004 0 0 0 .12 7.07 5.006 5.006 0 0 0 6.95 0l1.71-1.71
            M8 2v3
            M2 8h3
            M16 19v3
            M19 16h3
            """),
        CLOCK("""
            M2 12a10 10 0 1 0 20 0a10 10 0 1 0-20 0
            M12 6v6l4 2
            """),
        PENCIL("""
            M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0 0 0 .83-.497z
            M15 5l4 4
            """),
        CHEVRON_RIGHT("""
            M9 18l6-6-6-6
            """),
        ALERT("""
            M21.73 18l-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3
            M12 9v4
            M12 17h.01
            """),
        INFO("""
            M2 12a10 10 0 1 0 20 0a10 10 0 1 0-20 0
            M12 16v-4
            M12 8h.01
            """),
        HELP("""
            M2 12a10 10 0 1 0 20 0a10 10 0 1 0-20 0
            M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3
            M12 17h.01
            """),
        CHECK("""
            M20 6L9 17l-5-5
            """),
        LOG_OUT("""
            M16 17l5-5-5-5
            M21 12H9
            M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4
            """),
        ZAP("""
            M15.914 4a1.5 1.5 0 0 0-2.474-1.561l-9 9A1.5 1.5 0 0 0 5.5 14h4.002a.5.5 0 0 1 .471.666L8.086 20a1.5 1.5 0 0 0 2.475 1.56l9-9A1.5 1.5 0 0 0 18.5 10h-3.997a.5.5 0 0 1-.472-.667z
            """);

        private final String path;

        Icon(String path) {
            this.path = path;
        }
    }

    private Icons() {}

    /** Crea el icono con un tamaño visual en píxeles y clases CSS opcionales. */
    public static Node of(Icon icon, double size, String... styleClasses) {
        SVGPath path = new SVGPath();
        path.setContent(icon.path);
        path.setFill(Color.TRANSPARENT);
        path.setStrokeWidth(2.0);
        path.setStrokeLineCap(StrokeLineCap.ROUND);
        path.setStrokeLineJoin(StrokeLineJoin.ROUND);
        path.getStyleClass().add("icon");
        if (styleClasses != null) {
            for (String styleClass : styleClasses) {
                if (styleClass != null && !styleClass.isBlank()) {
                    path.getStyleClass().add(styleClass);
                }
            }
        }
        double scale = size / 24.0;
        path.getTransforms().add(new Scale(scale, scale, 0, 0));

        Group group = new Group(path);
        group.getStyleClass().add("icon-box");
        group.setMouseTransparent(true);
        return group;
    }

    /** Crea el icono con el tamaño habitual de las acciones. */
    public static Node of(Icon icon) {
        return of(icon, 16);
    }
}
