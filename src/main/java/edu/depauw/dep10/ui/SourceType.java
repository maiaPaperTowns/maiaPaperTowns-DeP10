package edu.depauw.dep10.ui;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.fife.ui.rsyntaxtextarea.TextEditorPane;

import edu.depauw.declan.Reporter;
import edu.depauw.dep10.assemble.Assembler;
import edu.depauw.dep10.assemble.Result;
import edu.depauw.dep10.driver.Driver;
import edu.depauw.dep10.driver.ErrorLog;
import edu.depauw.dep10.preprocess.Preprocessor;
import edu.depauw.dep10.preprocess.Sources;
import edu.depauw.dep10.simulator.BreakpointController;
import edu.depauw.dep10.simulator.Controller;
import edu.depauw.dep10.simulator.DebugState;
import edu.depauw.dep10.simulator.PlainController;
import edu.depauw.dep10.simulator.Simulator;
import edu.depauw.dep10.simulator.SingleStepController;
import edu.depauw.dep10.simulator.State;
import edu.depauw.dep10.simulator.TracingController;

public interface SourceType {
    int DEFAULT_STEP_LIMIT = 100000000; // TODO make this a setting

    default Result build(String source, OutputPanel symbols, OutputPanel object, TextEditorPane resTextArea) {
        var log = new ErrorLog();
        Sources sources = new Sources();

        loadOSHeader(sources, log);

        sources.addString(source);

        var preprocessor = new Preprocessor(log);
        Result result = null;

        if (log.noErrors()) {
            var lines = preprocessor.preprocess(sources);
            var assembler = new Assembler(log);
            result = assembler.assemble(lines); // TODO include map from line numbers to addresses (what about DeCLan?)

            symbols.setContent(result.getSymbols());
            object.setContent(result.toObjectFile());
            resTextArea.setText(result.getResultText());

            return result;
        } else {
            return Result.errors(log);
        }

        // TODO deal with errors; don't run on UI thread!
    }

    default void run(MainFrame frame) {
        var source = frame.source;
        var object = frame.object;
        var terminal = frame.terminal;
        var batch = frame.batch;
        var sp = frame.statePanel;

        State state = new State();
        state.setFancy(frame.isFancy());
        state.loadString(object.getContent());
        loadOSObject(state);

        terminal.clear();
        if (batch.isActive()) {
            state.setInput(new ByteArrayInputStream(batch.getContent().getBytes(StandardCharsets.UTF_8)));
        } else {
            state.setInput(terminal.getInputStream());
        }
        state.setOutput(terminal.getOutputStream());
        state.setError(terminal.getOutputStream());

        Simulator sim = new Simulator(state);
        sp.attach(state);

        Controller control = new PlainController();
        frame.setController(control);

        var t = new Thread(() -> {
            sim.run(control);
            frame.setController(null);

            source.setStopped();

            sp.refresh();
        });
        t.start();
    }

    // TODO avoid duplication from run()
    default void debug(MainFrame frame, List<Integer> breakpoints) {
        var source = frame.source;
        var object = frame.object;
        var terminal = frame.terminal;
        var batch = frame.batch;
        var trace = frame.tracePanel;
        var sp = frame.statePanel;

        State state = new DebugState();
        state.setFancy(frame.isFancy());
        state.loadString(object.getContent());
        loadOSObject(state);

        terminal.clear();
        if (batch.isActive()) {
            state.setInput(new ByteArrayInputStream(batch.getContent().getBytes(StandardCharsets.UTF_8)));
        } else {
            state.setInput(terminal.getInputStream());
        }
        state.setOutput(terminal.getOutputStream());
        state.setError(terminal.getOutputStream());

        Simulator sim = new Simulator(state);
        sp.attach(state);

        // if breakpoints are empty, stop initially at PC == 0
        if (breakpoints.isEmpty()) {
            breakpoints.add(0);
        }
        
        TracingController control = new TracingController(
                new BreakpointController(new PlainController(), s -> breakpoints.contains(s.getPC().value())));
        frame.setController(control);

        var t = new Thread(() -> {
            sim.run(control);
            // leave controller in place for single-step or resume

            source.setPaused();
            source.showLine(state.getPC().value());

            sp.refresh();

            var bytes = new ByteArrayOutputStream();
            var out = new PrintStream(bytes);
            control.printTrace(out);
            out.close();
            trace.setContent(bytes.toString());
        });
        t.start();
    }

    default void resume(MainFrame frame, State state, List<Integer> breakpoints) {
        var source = frame.source;
        var sp = frame.statePanel;
        var trace = frame.tracePanel;

        Simulator sim = new Simulator(state, false);
        state.resume();

        TracingController control = new TracingController(
                new BreakpointController(new PlainController(), s -> breakpoints.contains(s.getPC().value())),
                frame.getController());
        frame.setController(control);

        var t = new Thread(() -> {
            sim.run(control);
            // leave controller in place for single-step or resume

            source.setPaused();
            source.showLine(state.getPC().value());

            sp.refresh();

            var bytes = new ByteArrayOutputStream();
            var out = new PrintStream(bytes);
            control.printTrace(out);
            out.close();
            trace.setContent(bytes.toString());
        });
        t.start();
    }

    default void forward(MainFrame frame, State state) {
        var source = frame.source;
        var sp = frame.statePanel;
        var trace = frame.tracePanel;

        Simulator sim = new Simulator(state, false);
        state.resume();

        TracingController control = new TracingController(new SingleStepController(new PlainController()),
                frame.getController());
        frame.setController(control);

        var t = new Thread(() -> {
            sim.run(control);
            // leave controller in place for single-step or resume

            source.setPaused();
            source.showLine(state.getPC().value());

            sp.refresh();

            var bytes = new ByteArrayOutputStream();
            var out = new PrintStream(bytes);
            control.printTrace(out);
            out.close();
            trace.setContent(bytes.toString());
        });
        t.start();
    }

    default void backward(MainFrame frame, State state) {
        var source = frame.source;
        var sp = frame.statePanel;
        var trace = frame.tracePanel;

        var control = frame.getController();
        if (control instanceof TracingController tc) {
            tc.goBackward(state);
            source.setPaused();
            source.showLine(state.getPC().value());

            sp.refresh();

            var bytes = new ByteArrayOutputStream();
            var out = new PrintStream(bytes);
            tc.printTrace(out);
            out.close();
            trace.setContent(bytes.toString());
        }
    }

    void loadOSHeader(Sources sources, ErrorLog log);

    void loadOSObject(State state);

    SourceType Pep10UserFull = new SourceType() {
        @Override
        public String toString() {
            return "Pep/10 User Code";
        }

        @Override
        public void loadOSHeader(Sources sources, ErrorLog log) {
            sources.addResource(Driver.FULL_OS_HEADER, log);
        }

        @Override
        public void loadOSObject(State state) {
            state.loadResource(Driver.FULL_OS_OBJECT);
        }
    };

    SourceType Pep10UserBare = new SourceType() {
        @Override
        public String toString() {
            return "Pep/10 Bare Metal";
        }

        @Override
        public void loadOSHeader(Sources sources, ErrorLog log) {
            sources.addResource(Driver.BARE_METAL_OS_HEADER, log);
        }

        @Override
        public void loadOSObject(State state) {
            state.loadResource(Driver.BARE_METAL_OS_OBJECT);
        }
    };

    SourceType Pep10System = new SourceType() {
        @Override
        public String toString() {
            return "Pep/10 System Code";
        }

        @Override
        public void loadOSHeader(Sources sources, ErrorLog log) {
            sources.addResource(Driver.STD_MACROS, log);
        }

        @Override
        public void loadOSObject(State state) {
            // Do nothing
        }
    };

    SourceType DeCLan = new SourceType() {
        @Override
        public String toString() {
            return "DeCLan";
        }

        @Override
        public Result build(String source, OutputPanel listing, OutputPanel object, TextEditorPane resTextArea) {
            var err = new ByteArrayOutputStream();
            var reporter = new Reporter(new PrintStream(err, true));

            var pepSource = edu.depauw.declan.DeCLan.run(source, reporter);
            if (reporter.hadError()) {
                pepSource = err.toString(); // this is a cheat...
            }

            return SourceType.super.build(pepSource, listing, object, resTextArea);
        }

        @Override
        public void loadOSHeader(Sources sources, ErrorLog log) {
            sources.addResource(Driver.FULL_OS_HEADER, log);
        }

        @Override
        public void loadOSObject(State state) {
            state.loadResource(Driver.FULL_OS_OBJECT);
        }
    };
}
