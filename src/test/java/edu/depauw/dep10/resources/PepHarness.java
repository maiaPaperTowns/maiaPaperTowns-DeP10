package edu.depauw.dep10.resources;

import edu.depauw.dep10.assemble.Assembler;
import edu.depauw.dep10.assemble.Result;
import edu.depauw.dep10.driver.ErrorLog;
import edu.depauw.dep10.preprocess.Preprocessor;
import edu.depauw.dep10.preprocess.Sources;
import edu.depauw.dep10.simulator.PlainController;
import edu.depauw.dep10.simulator.Simulator;
import edu.depauw.dep10.simulator.State;
import edu.depauw.dep10.simulator.StepCountController;
import edu.depauw.dep10.util.Word;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Shared helper for tests that assemble and run a full .pep source string
 * (driver code plus one or more .INCLUDELIB library routines) against the
 * real preprocessor, assembler, and simulator, rather than unit-testing a
 * single opcode's exec() method in isolation. This is for library routines
 * written in DeP10 assembly itself (daddsub.pep, ddiv32.pep), which have no
 * single Java class to unit-test directly.
 *
 * Driver code must come first in the source, with any .INCLUDELIB directives
 * placed after the halt sequence but before .END: execution starts at
 * address 0 and falls straight into whatever is physically first, so a
 * library placed first would be executed instead of jumped into.
 */
public final class PepHarness {
    private PepHarness() {}

    /** Default step cap, generous for these routines (32-bit division is the heaviest, well under this). */
    private static final int DEFAULT_STEP_LIMIT = 200_000;

    public static State run(String source) {
        var log = new ErrorLog();
        Sources sources = new Sources();
        sources.addResource("/pep10baremetal.peph", log);
        sources.addString(source);

        var preprocessor = new Preprocessor(log);
        var lines = preprocessor.preprocess(sources);

        if (!log.noErrors()) {
            fail("Preprocess errors: " + log.getMessages());
        }

        var assembler = new Assembler(log);
        Result result = assembler.assemble(lines);

        if (!log.noErrors() || result.hasErrors()) {
            var sw = new java.io.StringWriter();
            result.printErrors(new java.io.PrintWriter(sw));
            fail("Assemble errors: " + log.getMessages() + "\n" + sw);
        }

        String obj = result.toObjectFile();

        State state = new State();
        state.loadResource("/pep10baremetal.pepo");
        state.loadString(obj);

        Simulator sim = new Simulator(state);
        var control = new StepCountController(new PlainController(), DEFAULT_STEP_LIMIT);
        sim.run(control);

        return state;
    }

    /** Reads a 16-bit memory word, for checking a fixed scratch address the driver code wrote a result to. */
    public static int mem(State state, int addr) {
        return state.mem2(Word.of(addr)).value();
    }
}
