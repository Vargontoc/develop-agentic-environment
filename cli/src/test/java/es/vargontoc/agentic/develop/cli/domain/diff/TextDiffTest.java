package es.vargontoc.agentic.develop.cli.domain.diff;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TextDiffTest {

    @Test
    void identicalTextsHaveNoDifferences() {
        assertEquals("", TextDiff.between("a\nb\n", "a\nb\n"));
    }

    @Test
    void lineEndingsAreNotDifferences() {
        assertEquals("", TextDiff.between("a\r\nb\r\n", "a\nb\n"));
    }

    @Test
    void reportsRemovedAndAddedLinesInOrder() {
        assertEquals("-b\n+x\n+y", TextDiff.between("a\nb\nc\n", "a\nx\ny\nc\n"));
    }

    @Test
    void reportsPureAdditionsAndRemovals() {
        assertEquals("+c", TextDiff.between("a\nb\n", "a\nb\nc\n"));
        assertEquals("-a", TextDiff.between("a\nb\n", "b\n"));
    }

    @Test
    void emptyTextsAreHandled() {
        assertEquals("+a", TextDiff.between("", "a\n"));
        assertEquals("-a", TextDiff.between("a\n", ""));
    }

    @Test
    void hugeChangedBlocksFallBackToReplacingEverything() {
        String before = "x\n".repeat(3000).replace("x", "a") + "fin\n";
        StringBuilder after = new StringBuilder();
        for(int i = 0; i < 3000; i++)
            after.append("b").append(i).append('\n');
        after.append("fin\n");

        String diff = TextDiff.between(before, after.toString());

        assertEquals(6000, diff.lines().count());
    }
}
