package edu.depauw.dep10.op;

import edu.depauw.dep10.simulator.State;
import edu.depauw.dep10.util.Word;

public class Pep10 {
    public static final Word TRAP_HANDLER_POINTER = Word.of(0xFFF7);
    public static final Word DISPATCHER_POINTER = Word.of(0xFFF9);
    public static final Word SYSTEM_STACK_POINTER = Word.of(0xFFFB);
    /** Timer device (fancy machine): instructions left to run in user mode before a timer trap; 0 = off. */
    public static final Word TIMER = Word.of(0xFFF5);
    public static final Word CHARIN = Word.of(0xFFFD);
    public static final Word CHAROUT = Word.of(0xFFFE);
    public static final Word SHUTDOWN = Word.of(0xFFFF);

    public static final Table table = new Table();

    public static final Operation RET = new Operation.Unary("RET") {
        public void exec(State s) {
            s.setPC(s.mem2(s.getSP()));
            s.setSP(s.getSP().plus(2));
        }
    };

    public static final Operation SRET = new Operation.Unary("SRET") {
        public void exec(State s) {
            var t = s.getSP();
            s.setFlags(s.mem1(t));
            s.setA(s.mem2(t.plus(1)));
            s.setX(s.mem2(t.plus(3)));
            s.setPC(s.mem2(t.plus(5)));
            s.setSP(s.mem2(t.plus(7)));
            s.setMem2(SYSTEM_STACK_POINTER, t.plus(12));
            s.leaveProtectedMode();
        }
    };

    public static final Operation MOVFLGA = new Operation.Unary("MOVFLGA") {
        public void exec(State s) {
            s.setA(s.getFlags());
        }
    };

    public static final Operation MOVAFLG = new Operation.Unary("MOVAFLG") {
        public void exec(State s) {
            s.setFlags(s.getA().lo());
        }
    };

    public static final Operation MOVSPA = new Operation.Unary("MOVSPA") {
        public void exec(State s) {
            s.setA(s.getSP());
        }
    };

    public static final Operation MOVASP = new Operation.Unary("MOVASP") {
        public void exec(State s) {
            s.setSP(s.getA());
        }
    };

    public static final Operation NOP = new Operation.Unary("NOP") {
        public void exec(State s) {
        }
    };

    public static final Operation NEGA = new Operation.Unary("NEGA") {
        public void exec(State s) {
            var a1 = s.getA();
            var sign1 = a1.isNegative();
            var zero1 = a1.isZero();

            var a2 = a1.negate();
            var sign2 = a2.isNegative();
            var zero2 = a2.isZero();

            s.setA(a2);
            s.setN(sign2);
            s.setZ(zero2);
            s.setV(sign1 && sign2);
            s.setC(zero1);
        }
    };

    public static final Operation NEGX = new Operation.Unary("NEGX") {
        public void exec(State s) {
            var x1 = s.getX();
            var sign1 = x1.isNegative();
            var zero1 = x1.isZero();

            var x2 = x1.negate();
            var sign2 = x2.isNegative();
            var zero2 = x2.isZero();

            s.setX(x2);
            s.setN(sign2);
            s.setZ(zero2);
            s.setV(sign1 && sign2);
            s.setC(zero1);
        }
    };

    public static final Operation ASLA = new Operation.Unary("ASLA") {
        public void exec(State s) {
            var a1 = s.getA();
            var sign1 = a1.isNegative();

            var a2 = a1.shiftLeft(false);
            var sign2 = a2.isNegative();
            var zero2 = a2.isZero();

            s.setC(sign1);
            s.setA(a2);
            s.setN(sign2);
            s.setZ(zero2);
            s.setV(sign1 != sign2);
        }
    };

    public static final Operation ASLX = new Operation.Unary("ASLX") {
        public void exec(State s) {
            var x1 = s.getX();
            var sign1 = x1.isNegative();

            var x2 = x1.shiftLeft(false);
            var sign2 = x2.isNegative();
            var zero2 = x2.isZero();

            s.setC(sign1);
            s.setX(x2);
            s.setN(sign2);
            s.setZ(zero2);
            s.setV(sign1 != sign2);
        }
    };

    public static final Operation ASRA = new Operation.Unary("ASRA") {
        public void exec(State s) {
            var a1 = s.getA();
            var sign1 = a1.isNegative();
            var carry = a1.bit(0);

            var a2 = a1.shiftRight(sign1);
            var zero2 = a2.isZero();

            s.setC(carry);
            s.setA(a2);
            s.setN(sign1);
            s.setZ(zero2);
            s.setV(false);
        }
    };

    public static final Operation ASRX = new Operation.Unary("ASRX") {
        public void exec(State s) {
            var x1 = s.getX();
            var sign1 = x1.isNegative();
            var carry = x1.bit(0);

            var x2 = x1.shiftRight(sign1);
            var zero2 = x2.isZero();

            s.setC(carry);
            s.setX(x2);
            s.setN(sign1);
            s.setZ(zero2);
            s.setV(false);
        }
    };

    public static final Operation NOTA = new Operation.Unary("NOTA") {
        public void exec(State s) {
            var a1 = s.getA();

            var a2 = a1.not();
            var sign2 = a2.isNegative();
            var zero2 = a2.isZero();

            s.setA(a2);
            s.setN(sign2);
            s.setZ(zero2);
        }
    };

    public static final Operation NOTX = new Operation.Unary("NOTX") {
        public void exec(State s) {
            var x1 = s.getX();

            var x2 = x1.not();
            var sign2 = x2.isNegative();
            var zero2 = x2.isZero();

            s.setX(x2);
            s.setN(sign2);
            s.setZ(zero2);
        }
    };

    public static final Operation ROLA = new Operation.Unary("ROLA") {
        public void exec(State s) {
            var a1 = s.getA();
            var carry = a1.isNegative();

            var a2 = a1.shiftLeft(s.getC());
            var sign2 = a2.isNegative();
            var zero2 = a2.isZero();

            s.setC(carry);
            s.setA(a2);
            s.setN(sign2);
            s.setZ(zero2);
        }
    };

    public static final Operation ROLX = new Operation.Unary("ROLX") {
        public void exec(State s) {
            var x1 = s.getX();
            var carry = x1.isNegative();

            var x2 = x1.shiftLeft(s.getC());
            var sign2 = x2.isNegative();
            var zero2 = x2.isZero();

            s.setC(carry);
            s.setX(x2);
            s.setN(sign2);
            s.setZ(zero2);
        }
    };

    public static final Operation RORA = new Operation.Unary("RORA") {
        public void exec(State s) {
            var a1 = s.getA();
            var carry = a1.bit(0);

            var a2 = a1.shiftRight(s.getC());
            var sign2 = a2.isNegative();
            var zero2 = a2.isZero();

            s.setC(carry);
            s.setA(a2);
            s.setN(sign2);
            s.setZ(zero2);
        }
    };

    public static final Operation RORX = new Operation.Unary("RORX") {
        public void exec(State s) {
            var x1 = s.getX();
            var carry = x1.bit(0);

            var x2 = x1.shiftRight(s.getC());
            var sign2 = x2.isNegative();
            var zero2 = x2.isZero();

            s.setC(carry);
            s.setX(x2);
            s.setN(sign2);
            s.setZ(zero2);
        }
    };

    public static final OpCore BR = new OpCore("BR", Modes.IX) {
        public void exec(State s, Mode mode) {
            s.setPC(mode.resolveWord(s));
        }
    };

    public static final OpCore BRLE = new OpCore("BRLE", Modes.IX) {
        public void exec(State s, Mode mode) {
            if (s.getN() || s.getZ()) {
                s.setPC(mode.resolveWord(s));
            }
        }
    };

    public static final OpCore BRLT = new OpCore("BRLT", Modes.IX) {
        public void exec(State s, Mode mode) {
            if (s.getN()) {
                s.setPC(mode.resolveWord(s));
            }
        }
    };

    public static final OpCore BREQ = new OpCore("BREQ", Modes.IX) {
        public void exec(State s, Mode mode) {
            if (s.getZ()) {
                s.setPC(mode.resolveWord(s));
            }
        }
    };

    public static final OpCore BRNE = new OpCore("BRNE", Modes.IX) {
        public void exec(State s, Mode mode) {
            if (!s.getZ()) {
                s.setPC(mode.resolveWord(s));
            }
        }
    };

    public static final OpCore BRGE = new OpCore("BRGE", Modes.IX) {
        public void exec(State s, Mode mode) {
            if (!s.getN()) {
                s.setPC(mode.resolveWord(s));
            }
        }
    };

    public static final OpCore BRGT = new OpCore("BRGT", Modes.IX) {
        public void exec(State s, Mode mode) {
            if (!s.getN() && !s.getZ()) {
                s.setPC(mode.resolveWord(s));
            }
        }
    };

    public static final OpCore BRV = new OpCore("BRV", Modes.IX) {
        public void exec(State s, Mode mode) {
            if (s.getV()) {
                s.setPC(mode.resolveWord(s));
            }
        }
    };

    public static final OpCore BRC = new OpCore("BRC", Modes.IX) {
        public void exec(State s, Mode mode) {
            if (s.getC()) {
                s.setPC(mode.resolveWord(s));
            }
        }
    };

    public static final OpCore CALL = new OpCore("CALL", Modes.IX) {
        public void exec(State s, Mode mode) {
            s.setSP(s.getSP().plus(-2));
            s.setMem2(s.getSP(), s.getPC());
            s.setPC(mode.resolveWord(s));
        }
    };

    public static final OpCore SCALL = new OpCore("SCALL", Modes.All) {
        public void exec(State s, Mode mode) {
            s.trap(s.getOpCode(), s.getOperand()); // don't resolve the operand yet; the handler will do that
        }
    };

    public static final OpCore LDWA = new OpCore("LDWA", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);
            var sign = operand.isNegative();
            var zero = operand.isZero();

            s.setA(operand);
            s.setN(sign);
            s.setZ(zero);
        }
    };

    public static final OpCore LDWX = new OpCore("LDWX", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);
            var sign = operand.isNegative();
            var zero = operand.isZero();

            s.setX(operand);
            s.setN(sign);
            s.setZ(zero);
        }
    };

    public static final OpCore LDBA = new OpCore("LDBA", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveByte(s);
            var zero = operand.isZero();

            s.setA(operand);
            s.setN(false);
            s.setZ(zero);
        }
    };

    public static final OpCore LDBX = new OpCore("LDBX", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveByte(s);
            var zero = operand.isZero();

            s.setX(operand);
            s.setN(false);
            s.setZ(zero);
        }
    };

    public static final OpCore STWA = new OpCore("STWA", Modes.NotI) {
        public void exec(State s, Mode mode) {
            var address = mode.getAddress(s);
            s.setMem2(address, s.getA());
        }
    };

    public static final OpCore STWX = new OpCore("STWX", Modes.NotI) {
        public void exec(State s, Mode mode) {
            s.setMem2(mode.getAddress(s), s.getX());
        }
    };

    public static final OpCore STBA = new OpCore("STBA", Modes.NotI) {
        public void exec(State s, Mode mode) {
            s.setMem1(mode.getAddress(s), s.getA().lo());
        }
    };

    public static final OpCore STBX = new OpCore("STBX", Modes.NotI) {
        public void exec(State s, Mode mode) {
            s.setMem1(mode.getAddress(s), s.getX().lo());
        }
    };

    public static final OpCore CPWA = new OpCore("CPWA", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);

            var a1 = s.getA();
            var sign1 = a1.isNegative();

            var op = operand.negate();
            var signo = operand.isNegative(); // sign of the operand itself, not of its negation

            var a2 = a1.plus(op);
            var sign2 = a2.isNegative();
            var zero2 = a2.isZero();
            var overflow = (sign1 != signo) && (sign1 != sign2);
            var carry = !a1.lessThan(operand); // true if a1 >= operand, so no borrow

            s.setN(sign2 ^ overflow);
            s.setZ(zero2);
            s.setV(overflow);
            s.setC(carry);
        }
    };

    public static final OpCore CPWX = new OpCore("CPWX", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);

            var x1 = s.getX();
            var sign1 = x1.isNegative();

            var op = operand.negate();
            var signo = operand.isNegative(); // sign of the operand itself, not of its negation

            var x2 = x1.plus(op);
            var sign2 = x2.isNegative();
            var zero2 = x2.isZero();
            var overflow = (sign1 != signo) && (sign1 != sign2);
            var carry = !x1.lessThan(operand); // true if x1 >= operand, so no borrow

            s.setN(sign2 ^ overflow);
            s.setZ(zero2);
            s.setV(overflow);
            s.setC(carry);
        }
    };

    public static final OpCore CPBA = new OpCore("CPBA", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveByte(s);

            var a1 = s.getA();

            var a2 = a1.minus(operand);
            var sign2 = a2.isNegative();
            var zero2 = a2.isZero();

            s.setN(sign2);
            s.setZ(zero2);
            s.setV(false);
            s.setC(false);
        }
    };

    public static final OpCore CPBX = new OpCore("CPBX", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveByte(s);

            var x1 = s.getX();

            var x2 = x1.minus(operand);
            var sign2 = x2.isNegative();
            var zero2 = x2.isZero();

            s.setN(sign2);
            s.setZ(zero2);
            s.setV(false);
            s.setC(false);
        }
    };

    public static final OpCore ADDA = new OpCore("ADDA", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);

            var a1 = s.getA();
            var sign1 = a1.isNegative();

            var signo = operand.isNegative();

            var a2 = a1.plus(operand);
            var sign2 = a2.isNegative();
            var zero2 = a2.isZero();
            var overflow = (sign1 == signo) && (sign1 != sign2);
            var carry = a2.lessThan(a1) || a2.lessThan(operand);

            s.setA(a2);
            s.setN(sign2);
            s.setZ(zero2);
            s.setV(overflow);
            s.setC(carry);
        }
    };

    public static final OpCore ADDX = new OpCore("ADDX", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);

            var x1 = s.getX();
            var sign1 = x1.isNegative();

            var signo = operand.isNegative();

            var x2 = x1.plus(operand);
            var sign2 = x2.isNegative();
            var zero2 = x2.isZero();
            var overflow = (sign1 == signo) && (sign1 != sign2);
            var carry = x2.lessThan(x1) || x2.lessThan(operand);

            s.setX(x2);
            s.setN(sign2);
            s.setZ(zero2);
            s.setV(overflow);
            s.setC(carry);
        }
    };

    public static final OpCore SUBA = new OpCore("SUBA", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);

            var a1 = s.getA();
            var sign1 = a1.isNegative();

            var op = operand.negate();
            var signo = operand.isNegative(); // sign of the operand itself, not of its negation

            var a2 = a1.plus(op);
            var sign2 = a2.isNegative();
            var zero2 = a2.isZero();
            var overflow = (sign1 != signo) && (sign1 != sign2);
            var carry = !a1.lessThan(operand); // true if a1 >= operand, so no borrow

            s.setA(a2);
            s.setN(sign2);
            s.setZ(zero2);
            s.setV(overflow);
            s.setC(carry);
        }
    };

    public static final OpCore SUBX = new OpCore("SUBX", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);

            var x1 = s.getX();
            var sign1 = x1.isNegative();

            var op = operand.negate();
            var signo = operand.isNegative(); // sign of the operand itself, not of its negation

            var x2 = x1.plus(op);
            var sign2 = x2.isNegative();
            var zero2 = x2.isZero();
            var overflow = (sign1 != signo) && (sign1 != sign2);
            var carry = !x1.lessThan(operand); // true if x1 >= operand, so no borrow

            s.setX(x2);
            s.setN(sign2);
            s.setZ(zero2);
            s.setV(overflow);
            s.setC(carry);
        }
    };

    public static final OpCore ANDA = new OpCore("ANDA", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);

            var a1 = s.getA();

            var a2 = a1.and(operand);
            var sign2 = a2.isNegative();
            var zero2 = a2.isZero();

            s.setA(a2);
            s.setN(sign2);
            s.setZ(zero2);
        }
    };

    public static final OpCore ANDX = new OpCore("ANDX", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);

            var x1 = s.getX();

            var x2 = x1.and(operand);
            var sign2 = x2.isNegative();
            var zero2 = x2.isZero();

            s.setX(x2);
            s.setN(sign2);
            s.setZ(zero2);
        }
    };

    public static final OpCore ORA = new OpCore("ORA", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);

            var a1 = s.getA();

            var a2 = a1.or(operand);
            var sign2 = a2.isNegative();
            var zero2 = a2.isZero();

            s.setA(a2);
            s.setN(sign2);
            s.setZ(zero2);
        }
    };

    public static final OpCore ORX = new OpCore("ORX", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);

            var x1 = s.getX();

            var x2 = x1.or(operand);
            var sign2 = x2.isNegative();
            var zero2 = x2.isZero();

            s.setX(x2);
            s.setN(sign2);
            s.setZ(zero2);
        }
    };

    public static final OpCore XORA = new OpCore("XORA", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);

            var a1 = s.getA();

            var a2 = a1.xor(operand);
            var sign2 = a2.isNegative();
            var zero2 = a2.isZero();

            s.setA(a2);
            s.setN(sign2);
            s.setZ(zero2);
        }
    };

    public static final OpCore XORX = new OpCore("XORX", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);

            var x1 = s.getX();

            var x2 = x1.xor(operand);
            var sign2 = x2.isNegative();
            var zero2 = x2.isZero();

            s.setX(x2);
            s.setN(sign2);
            s.setZ(zero2);
        }
    };

    public static final OpCore ADDSP = new OpCore("ADDSP", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);

            var sp1 = s.getSP();

            var sp2 = sp1.plus(operand);

            s.setSP(sp2);
        }
    };

    public static final OpCore SUBSP = new OpCore("SUBSP", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);

            var sp1 = s.getSP();

            var op = operand.negate();

            var sp2 = sp1.plus(op);

            s.setSP(sp2);
        }
    };

    static {
        table.install(1, RET);
        table.install(2, SRET);
        table.install(3, MOVFLGA);
        table.install(4, MOVAFLG);
        table.install(5, MOVSPA);
        table.install(6, MOVASP);
        table.install(7, NOP);

        table.install(8, Dep10Arith.table);
        table.install(9, Dep10Util.table);

        table.install(24, NEGA);
        table.install(25, NEGX);
        table.install(26, ASLA);
        table.install(27, ASLX);
        table.install(28, ASRA);
        table.install(29, ASRX);
        table.install(30, NOTA);
        table.install(31, NOTX);
        table.install(32, ROLA);
        table.install(33, ROLX);
        table.install(34, RORA);
        table.install(35, RORX);

        table.install(36, BR);
        table.install(38, BRLE);
        table.install(40, BRLT);
        table.install(42, BREQ);
        table.install(44, BRNE);
        table.install(46, BRGE);
        table.install(48, BRGT);
        table.install(50, BRV);
        table.install(52, BRC);
        table.install(54, CALL);

        table.install(56, SCALL);
        table.install(64, ADDSP);
        table.install(72, SUBSP);

        table.install(80, ADDA);
        table.install(88, ADDX);
        table.install(96, SUBA);
        table.install(104, SUBX);
        table.install(112, ANDA);
        table.install(120, ANDX);
        table.install(128, ORA);
        table.install(136, ORX);
        table.install(144, XORA);
        table.install(152, XORX);
        table.install(160, CPWA);
        table.install(168, CPWX);
        table.install(176, CPBA);
        table.install(184, CPBX);
        table.install(192, LDWA);
        table.install(200, LDWX);
        table.install(208, LDBA);
        table.install(216, LDBX);
        table.install(224, STWA);
        table.install(232, STWX);
        table.install(240, STBA);
        table.install(248, STBX);
    }
}
