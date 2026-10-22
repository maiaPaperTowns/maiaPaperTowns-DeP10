package edu.depauw.dep10.resources;

import edu.depauw.dep10.simulator.PlainController;
import edu.depauw.dep10.simulator.Simulator;
import edu.depauw.dep10.simulator.State;
import edu.depauw.dep10.simulator.StepCountController;
import edu.depauw.dep10.util.Word;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the "Two Threads, One Printer" files in demo/race: the starter, whose printLong keeps its
 * working variables in static storage, and the solution, which keeps them on the stack. The
 * program runs two threads under the fancy machine's timer; the kernel records the running thread
 * at 0xF000, so each thread's console output can be collected separately and compared with what
 * that thread should have printed.
 */
class RaceExerciseTest {
    private static final String A_TEXT = "2147483647\n-2147483648\n0\n1234567\n-90\n";
    private static final String B_TEXT = "100000\n-1\n2000000000\n7\n-305419896\n";
    private static final int LIMIT = 20_000_000;
    private static final String QUANTUM = "quantum:  .EQUATE 40";

    private static String read(String name) throws IOException {
        return Files.readString(Path.of("demo", "race", name));
    }

    /** Runs the program with the given quantum; returns {thread 1 output, thread 2 output}. */
    private static String[] run(String src, int quantum) {
        String text = src.replace(QUANTUM, "quantum:  .EQUATE " + quantum);
        assertFalse(text.equals(src) && quantum != 40, "the quantum edit must change the program");
        State s = PepHarness.load(PepHarness.assemble(text), null);
        s.setFancy(true);
        var outs = new ByteArrayOutputStream[] {new ByteArrayOutputStream(), new ByteArrayOutputStream(),
                new ByteArrayOutputStream()};
        s.setOutput(new OutputStream() {
            @Override
            public void write(int b) {
                int who = s.mem2Safe(Word.of(0xF000)).value();
                outs[who >= 1 && who <= 2 ? who : 0].write(b);
            }
        });
        new Simulator(s).run(new StepCountController(new PlainController(), LIMIT));
        assertFalse(s.isRunning(), "machine should have powered off (quantum " + quantum + ")");
        assertEquals(0, PepHarness.mem(s, 0xF004), "no unknown system call (quantum " + quantum + ")");
        return new String[] {outs[1].toString(StandardCharsets.US_ASCII), outs[2].toString(StandardCharsets.US_ASCII)};
    }

    private static boolean correct(String[] out) {
        return A_TEXT.equals(out[0]) && B_TEXT.equals(out[1]);
    }

    private static final int[] QUANTA = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 17, 19, 23, 29, 31, 37, 40, 41, 43,
            47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97, 101, 127, 211, 509, 997, 4001};

    @Test
    @DisplayName("Solution: every quantum gives each thread its own correct output")
    void solutionIsSafe() throws IOException {
        String src = read("race_solution.pep");
        for (int q : QUANTA) {
            String[] out = run(src, q);
            assertEquals(A_TEXT, out[0], "thread 1 output, quantum " + q);
            assertEquals(B_TEXT, out[1], "thread 2 output, quantum " + q);
        }
    }

    @Test
    @DisplayName("Starter with no timer: works, because the threads never interrupt each other")
    void starterWorksWithoutTimer() throws IOException {
        String[] out = run(read("race_starter.pep"), 0);
        assertEquals(A_TEXT, out[0]);
        assertEquals(B_TEXT, out[1]);
    }

    @Test
    @DisplayName("Starter: some quantum garbles a number (the shared static variables)")
    void starterIsBroken() throws IOException {
        String src = read("race_starter.pep");
        int broken = 0;
        for (int q : QUANTA) {
            if (!correct(run(src, q))) {
                broken++;
            }
        }
        assertTrue(broken >= 5, "only " + broken + " quanta exposed the bug");
    }

    @Test
    @DisplayName("Starter and solution differ only inside the student block")
    void filesDifferOnlyInStudentBlock() throws IOException {
        String starter = read("race_starter.pep");
        String solution = read("race_solution.pep");
        String begin = "; ---- BEGIN STUDENT CODE";
        String end = "; ---- END STUDENT CODE";
        assertEquals(starter.substring(0, starter.indexOf(begin)), solution.substring(0, solution.indexOf(begin)),
                "code before the student block");
        assertEquals(starter.substring(starter.indexOf(end)), solution.substring(solution.indexOf(end)),
                "code after the student block");
    }
}
