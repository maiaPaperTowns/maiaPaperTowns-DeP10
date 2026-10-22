package edu.depauw.dep10.driver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Properties;

import com.beust.jcommander.JCommander;

import edu.depauw.dep10.assemble.Assembler;
import edu.depauw.dep10.assemble.Result;
import edu.depauw.dep10.preprocess.Preprocessor;
import edu.depauw.dep10.preprocess.Sources;
import edu.depauw.dep10.simulator.Controller;
import edu.depauw.dep10.simulator.DebugState;
import edu.depauw.dep10.simulator.PlainController;
import edu.depauw.dep10.simulator.Simulator;
import edu.depauw.dep10.simulator.State;
import edu.depauw.dep10.simulator.StepCountController;
import edu.depauw.dep10.simulator.TracingController;
import edu.depauw.dep10.ui.Main;

public class Driver {
    public static final String PROPERTIES = "project.properties";
    public static final String VERSION_PROP = "version";

    public static final String BARE_METAL_OS_HEADER = "/pep10baremetal.peph";
    public static final String FULL_OS_HEADER = "/pep10os.peph";
    public static final String FORTH_OS_HEADER = "/pep10forth.peph";

    public static final String BARE_METAL_OS_OBJECT = "/pep10baremetal.pepo";
    public static final String FULL_OS_OBJECT = "/pep10os.pepo";
    public static final String FORTH_OS_OBJECT = "/pep10forth.pepo";

    public static final String STD_MACROS = "/stdmacro.pep";

    public static void main(String[] args) {
        if (args.length == 0 || args[0].equals("-g") || args[0].equals("--gui")) {
            // assume GUI mode: no args, or explicit -g/--gui first arg
            Main.main(args);
        } else {
            InitialArgs init = new InitialArgs();
            CommandAsm asm = new CommandAsm();
            CommandRun run = new CommandRun();
            JCommander jc = JCommander.newBuilder()
                    .addObject(init)
                    .addCommand("asm", asm)
                    .addCommand("run", run)
                    .build();
            jc.parse(args);

            if (init.showVersion) {
                try {
                    var properties = new Properties();
                    properties.load(Driver.class.getClassLoader().getResourceAsStream(PROPERTIES));
                    System.out.println(properties.getProperty(VERSION_PROP));
                } catch (IOException e) {
                    System.err.println("Unable to load resource " + PROPERTIES);
                }
            } else if (init.showGUI) {
                Main.main(args);
            } else if (init.showHelp || jc.getParsedCommand() == null) {
                jc.usage();
            } else {
                var log = new ErrorLog();

                switch (jc.getParsedCommand()) {
                case "asm":
                    if (asm.showHelp) {
                        jc.usage("asm");
                    } else {
                        doAsm(asm, log);
                    }
                    break;
                case "run":
                    if (run.showHelp) {
                        jc.usage("run");
                    } else {
                        doRun(run, log);
                    }
                    break;
                default:
                    jc.usage();
                    break;
                }
            }
        }
    }

    private static void doAsm(CommandAsm asm, ErrorLog log) {
        Sources sources = new Sources();

        // Include header for selected OS
        if (asm.bareMetal) {
            if (asm.osName != null) {
                log.error("Bare metal excludes specifying OS.");
            }
            sources.addResource(BARE_METAL_OS_HEADER, log);
        } else if (asm.osName != null) {
            if (asm.osName.equalsIgnoreCase("FULL")) {
                sources.addResource(FULL_OS_HEADER, log);
            } else if (asm.osName.equalsIgnoreCase("FORTH")) {
                sources.addResource(FORTH_OS_HEADER, log);
            } else {
                sources.addFile(asm.osName + ".peph", log);
            }
        } else if (!asm.sysMode) {
            sources.addResource(FULL_OS_HEADER, log);
        } else {
            // assembling an OS in system mode; don't include
            // an OS header unless specifically requested,
            // but do include the standard macros
            sources.addResource(STD_MACROS, log);
        }

        // Next add the distinguished source file, if any
        if (asm.sourceFile != null) {
            sources.addFile(asm.sourceFile, log);
        }

        // Add any other source files from the command line
        for (var name : asm.sourceList) {
            sources.addFile(name, log);
        }

        // If no sources specified, add standard input
        if (asm.sourceFile == null && asm.sourceList.isEmpty()) {
            sources.addStdIn();
        }

        var preprocessor = new Preprocessor(log);
        Result result = null;

        if (log.noErrors()) {
            var lines = preprocessor.preprocess(sources);
            // all macros and includes have been expanded

            var assembler = new Assembler(log);
            result = assembler.assemble(lines);

            if (asm.objectFile != null) {
                try (var out = new BufferedWriter(new FileWriter(asm.objectFile))) {
                    out.write(result.toObjectFile());
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            } else {
                System.out.println(result.toObjectFile());
            }

            if (asm.listingFile != null) {
                try (var out = new PrintWriter(new BufferedWriter(new FileWriter(asm.listingFile)))) {
                    result.printListing(out);
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }

            if (asm.exportFile != null) {
                // Generate a header file
                try (var out = new PrintWriter(new BufferedWriter(new FileWriter(asm.exportFile)))) {
                    preprocessor.printMacros(out);
                    result.printHeader(out);
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
        }

        if (asm.errorFile != null) {
            try (var out = new PrintWriter(new BufferedWriter(new FileWriter(asm.errorFile)))) {
                for (var message : log.getMessages()) {
                    out.println(message);
                }

                if (result != null) {
                    result.printErrors(out);
                }
            } catch (IOException e) {
                System.err.println("Error writing error file!");
            }
        } else {
            for (var message : log.getMessages()) {
                System.err.println(message);
            }
        }
    }

    private static void doRun(CommandRun run, ErrorLog log) {
        State state = (run.trace == null) ? new State() : new DebugState();

        for (var param : run.parameters) {
            state.loadFile(param);
        }

        // load object code for selected OS
        if (run.bareMetal) {
            if (run.osName != null) {
                log.error("Bare metal excludes specifying OS.");
            }
            state.loadResource(BARE_METAL_OS_OBJECT);
        } else if (run.osName != null) {
            if (run.osName.equalsIgnoreCase("FULL")) {
                state.loadResource(FULL_OS_OBJECT);
            } else if (run.osName.equalsIgnoreCase("FORTH")) {
                state.loadResource(FORTH_OS_OBJECT);
            } else {
                state.loadFile(run.osName + ".pepo");
            }
        } else {
            state.loadResource(FULL_OS_OBJECT);
        }

        state.setInput(run.consoleIn);
        state.setOutput(run.consoleOut);
        state.setError(run.errOut);
        state.setFancy(run.fancy);

        Simulator sim = new Simulator(state);

        Controller control = new PlainController();
        if (run.max > 0) {
            control = new StepCountController(control, run.max);
        }

        TracingController tc = null;
        if (run.trace != null) {
            tc = new TracingController(control);
            control = tc;
        }

        // TODO for interactive use, also support breakpoints and single-stepping
        sim.run(control);

        if (run.memDump != null) {
            state.dump(run.memDump);
        }

        if (run.trace != null) {
            try {
                var output = new PrintStream(new File(run.trace));
                tc.printTrace(output);
                output.close();
            } catch (FileNotFoundException e) {
                System.err.println("Unable to open tracing output: " + run.trace);
            }
        }
    }
}
