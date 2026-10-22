package edu.depauw.dep10.simulator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.Scanner;

import edu.depauw.dep10.op.Operation;
import edu.depauw.dep10.op.Pep10;
import edu.depauw.dep10.op.Table;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

public class State {
    private Word A;
    private Word X;
    private Word H;
    private Word PC;
    private Word SP;
    private UByte PRE;
    private UByte IR1;
    private Word IR2;
    private Word EA;
    private UByte Flags;
    private boolean running;
    private Operation op;

    // Fancy machine: supervisor (protected) mode, timer and invalid-instruction trap.
    // Off by default, so bare-metal programs see the original machine.
    private boolean fancy = false;
    private boolean protectedMode = true; // the machine boots in the OS
    private boolean tickEligible = false;

    private UByte[] memory = new UByte[65536];
    
    private InputStream in = System.in;
    private PrintStream out = System.out;
    private PrintStream err = System.err;

    public State() {
        A = Word.of(0);
        X = Word.of(0);
        H = Word.of(0);
        PC = Word.of(0);
        SP = Word.of(0);
        PRE = UByte.of(0);
        IR1 = UByte.of(0);
        IR2 = Word.of(0);
        EA = Word.of(0);
        Flags = UByte.of(0);
        running = false;
        op = null;

        for (int i = 0; i < memory.length; i++) {
            memory[i] = UByte.of(0);
        }
    }

    void initialize(Word initPC, Word initSP) {
        setPC(mem2(initPC));
        setSP(mem2(initSP));
        running = true;
    }
    
    public UByte mem1(Word addr) {
        // TODO check permissions
        if (addr.equals(Pep10.CHARIN)) {
            try {
                return UByte.of(in.read());
            } catch (IOException e) {
                haltWithError("Unable to read from console input");
            }
        }

        return memory[addr.value()];
    }

    public UByte mem1Safe(Word addr) {
        return memory[addr.value()];
    }
    
    public Word mem2(Word addr) {
        var hi = mem1(addr);
        var lo = mem1(addr.plus(1));
        return Word.of((hi.value() << 8) + lo.value());
    }

    public Word mem2Safe(Word addr) {
        var hi = mem1Safe(addr);
        var lo = mem1Safe(addr.plus(1));
        return Word.of((hi.value() << 8) + lo.value());
    }

    public void setMem1(Word addr, UByte n) {
        // TODO check permissions
        if (addr.equals(Pep10.SHUTDOWN)) {
            stop();
        } else if (addr.equals(Pep10.CHAROUT)) {
            out.write(n.value());
        }

        memory[addr.value()] = n;
    }

    public void setMem2(Word addr, Word n) {
        setMem1(addr, n.hi());
        setMem1(addr.plus(1), n.lo());
    }

    public Word getA() {
        return A;
    }

    public Word getX() {
        return X;
    }
    
    public Word getH() {
        return H;
    }

    public Word getPC() {
        return PC;
    }

    public Word getSP() {
        return SP;
    }

    public UByte getPrefix() {
        return PRE;
    }

    public UByte getOpCode() {
        return IR1;
    }

    public Word getOperand() {
        return IR2;
    }

    public Word getEA() {
        return EA;
    }

    public void setA(Word n) {
        this.A = n;
    }

    public void setA(UByte n) {
        this.A = Word.of(n.value());
    }

    public void setX(Word n) {
        this.X = n;
    }

    public void setX(UByte n) {
        this.X = Word.of(n.value());
    }
    
    public void setH(Word n) {
        this.H = n;
    }

    public void setPC(Word n) {
        this.PC = n;
    }

    public void setSP(Word n) {
        this.SP = n;
    }

    public void setPrefix(UByte opCode) {
        PRE = opCode;
    }

    public void setOpCode(UByte n) {
        this.IR1 = n;
    }

    public void setOperand(Word n) {
        this.IR2 = n;
    }

    public void setEA(Word address) {
        EA = address;
    }

    public boolean getN() {
        return Flags.bit(3);
    }

    public boolean getZ() {
        return Flags.bit(2);
    }

    public boolean getV() {
        return Flags.bit(1);
    }

    public boolean getC() {
        return Flags.bit(0);
    }

    public void setN(boolean b) {
        Flags = Flags.withBit(3, b);
    }

    public void setZ(boolean b) {
        Flags = Flags.withBit(2, b);
    }

    public void setV(boolean b) {
        Flags = Flags.withBit(1, b);
    }

    public void setC(boolean b) {
        Flags = Flags.withBit(0, b);
    }

    public void setFlags(UByte flags) {
        Flags = UByte.of(flags.value() & 0x0F);
    }

    public UByte getFlags() {
        return Flags;
    }

    public boolean isRunning() {
        return running;
    }
    
    public Operation getOp() {
        return op;
    }
    
    public void setOp(Operation op) {
        this.op = op;
    }
    
    public void pause() {
        running = false;
        flushIO();
    }
    
    public void stop() {
        pause();
        shutdownIO();
    }
    
    public void resume() {
        running = true;
    }
    
    public void loadFile(String param) {
        try (var scanner = new Scanner(new File(param))) {
            load(scanner);
        } catch (FileNotFoundException e) {
            haltWithError("Unable to load " + param);
        }
    }
    
    public void loadResource(String resource) {
        URL url = getClass().getResource(resource);
        if (url == null) {
            haltWithError("Unable to open resource " + resource);
        }

        try (var scanner = new Scanner(url.openStream())) {
            load(scanner);
        } catch (IOException e) {
            haltWithError("Unable to open resource " + resource);
        }
    }
    
    public void loadString(String object) {
    	try (var scanner = new Scanner(object)) {
    		load(scanner);
    	}
    }

    private void load(Scanner scanner) {
        int addr = 0;

        while (scanner.hasNext()) {
            String token = scanner.next();
            if (token.startsWith("[")) {
                addr = Integer.parseInt(token.substring(1, 5), 16);
                // TODO also handle protection bits
            } else {
                var value = Integer.parseInt(token, 16);
                memory[addr] = UByte.of(value);
                addr = (addr + 1) % memory.length;
            }
        }
    }

    public void dump(String memDump) {
        try (var dumpOut = new FileOutputStream(memDump)) {
            var mem = new byte[memory.length];
            for (int i = 0; i < memory.length; i++) {
                mem[i] = (byte) memory[i].value();
            }
            
            dumpOut.write(mem);
        } catch (IOException e) {
            err.println("Error writing memory dump: " + e.getMessage());
        }
    }

    private void haltWithError(String message) {
        err.println(message);
        stop();
    }
    
    private void flushIO() {
        out.flush();
        if (err != System.err) {
            err.flush(); // System.err is autoflushing
        }
    }

    private void shutdownIO() {
        if (in != System.in) {
            try {
                in.close();
            } catch (IOException e) {
                err.println("Unable to close console input");
            }
        }
        
        if (out != System.out) {
            out.close();
        }
        
        if (err != System.err) {
            err.close();
        }
    }

    public void setInput(String consoleIn) {
        if (consoleIn != null) {
            try {
                in = new FileInputStream(new File(consoleIn));
            } catch (FileNotFoundException e) {
                haltWithError("Unable to open console input: " + consoleIn);
            }
        } else {
            in = System.in;
        }
    }
    
    public void setInput(InputStream is) {
        in = is;
    }
    
    public void setOutput(String consoleOut) {
        if (consoleOut != null) {
            try {
                out = new PrintStream(new File(consoleOut));
            } catch (FileNotFoundException e) {
                haltWithError("Unable to open console output: " + consoleOut);
            }
        } else {
            out = System.out;
        }
    }
    
    public void setOutput(OutputStream os) {
        out = new PrintStream(os);
    }
    
    public void setError(String errOut) {
        if (errOut != null) {
            try {
                err = new PrintStream(new File(errOut));
            } catch (FileNotFoundException e) {
                haltWithError("Unable to open error output: " + errOut);
            }
        } else {
            err = System.err;
        }
    }
    
    public void setError(OutputStream os) {
        err = new PrintStream(os);
    }

    public boolean isFancy() {
        return fancy;
    }

    /** Turns the fancy machine (protected/user modes, timer, invalid-instruction trap) on or off. */
    public void setFancy(boolean fancy) {
        this.fancy = fancy;
    }

    /** True while the OS is running (supervisor mode); false while a user program runs. Meaningful only on the fancy machine. */
    public boolean isProtectedMode() {
        return protectedMode;
    }

    public void leaveProtectedMode() {
        if (fancy) {
            protectedMode = false;
        }
    }

    /**
     * Pushes the 12-byte trap frame on the system stack (flags at +0, A +1, X +3, PC +5, SP +7,
     * opcode +9, operand +10), enters protected mode and jumps to the trap handler.
     * SCALL uses its own opcode and raw operand. The machine's own traps use opcode 0:
     * operand 1 is the timer, operand 2 an invalid instruction (the bad opcode is then in the
     * frame's opcode byte, so an invalid opcode 0 is told apart from the timer by its operand).
     */
    public void trap(UByte opcode, Word operand) {
        var t = mem2(Pep10.SYSTEM_STACK_POINTER);

        setMem2(t.plus(-2), operand);
        setMem1(t.plus(-3), opcode);
        setMem2(t.plus(-5), SP);
        setMem2(t.plus(-7), PC);
        setMem2(t.plus(-9), X);
        setMem2(t.plus(-11), A);
        setMem1(t.plus(-12), Flags);

        setSP(t.plus(-12));
        setPC(mem2(Pep10.TRAP_HANDLER_POINTER));
        if (fancy) {
            protectedMode = true;
        }
    }

    /** Called when the current instruction is not defined. */
    public void invalidInstruction() {
        if (!fancy) {
            throw new IllegalStateException("Unimplemented operation");
        }
        if (protectedMode) {
            err.println(String.format("Invalid instruction 0x%02X at 0x%04X in supervisor mode; stopping",
                    IR1.value(), PC.plus(-1).value()));
            stop();
        } else {
            trap(IR1, Word.of(2));
        }
    }

    /** Called after each instruction that actually ran; counts the timer down while in user mode. */
    public void instructionDone() {
        if (tickEligible && !protectedMode) {
            int t = mem2Safe(Pep10.TIMER).value();
            if (t != 0) {
                t--;
                memory[Pep10.TIMER.value()] = UByte.of(t >> 8);
                memory[Pep10.TIMER.value() + 1] = UByte.of(t & 0xFF);
                if (t == 0) {
                    trap(UByte.of(0), Word.of(1));
                }
            }
        }
    }

    public void doStep(Controller control, Table table) {
        var origPC = PC;
        tickEligible = fancy && !protectedMode;
        
        PRE = UByte.of(0);
        IR1 = mem1(PC);
        PC = PC.plus(1);
        
        var op = table.getOp(IR1);
        op.perform(this, origPC, control);
    }
}
