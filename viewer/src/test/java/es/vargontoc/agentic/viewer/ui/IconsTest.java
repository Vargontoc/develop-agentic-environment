package es.vargontoc.agentic.viewer.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import javafx.scene.Node;

class IconsTest {

    @Test
    void everyIconCanBeBuiltAndHasVisibleBounds() {
        for (Icons.Icon icon : Icons.Icon.values()) {
            Node node = Icons.of(icon, 16);
            assertTrue(node.getBoundsInLocal().getWidth() > 0, icon + " no tiene ancho");
            assertTrue(node.getBoundsInLocal().getHeight() > 0, icon + " no tiene alto");
        }
    }
}
