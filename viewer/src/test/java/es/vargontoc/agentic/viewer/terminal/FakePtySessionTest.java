package es.vargontoc.agentic.viewer.terminal;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class FakePtySessionTest {

    @Test
    void startMakesSessionAlive() throws Exception {
        FakePtySession session = new FakePtySession("respuesta");
        assertFalse(session.isAlive());
        session.start();
        assertTrue(session.isAlive());
        session.close();
        assertFalse(session.isAlive());
    }

    @Test
    void readsResponseText() throws Exception {
        FakePtySession session = new FakePtySession("hola mundo");
        session.start();
        String output = new String(session.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals("hola mundo", output);
    }

    @Test
    void capturesSentInput() throws Exception {
        FakePtySession session = new FakePtySession("");
        session.start();
        session.getOutputStream().write("1\n".getBytes(StandardCharsets.UTF_8));
        assertEquals("1\n", session.getSentInput());
    }

    @Test
    void resizeRecordsDimensions() {
        FakePtySession session = new FakePtySession("");
        session.resize(120, 40);
        assertEquals(120, session.getLastColumns());
        assertEquals(40, session.getLastRows());
    }

    @Test
    void interruptFlagIsSet() throws Exception {
        FakePtySession session = new FakePtySession("");
        session.start();
        assertFalse(session.wasInterrupted());
        session.interrupt();
        assertTrue(session.wasInterrupted());
    }

    @Test
    void closeMarksSessionClosed() throws Exception {
        FakePtySession session = new FakePtySession("");
        session.start();
        assertFalse(session.wasClosed());
        session.close();
        assertTrue(session.wasClosed());
        assertFalse(session.isAlive());
    }

    @Test
    void waitForReturnsZero() throws Exception {
        FakePtySession session = new FakePtySession("");
        session.start();
        session.finish();
        assertEquals(0, session.waitFor());
    }
}
