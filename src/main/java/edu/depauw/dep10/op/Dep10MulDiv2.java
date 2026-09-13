package edu.depauw.dep10.op;

import edu.depauw.dep10.simulator.State;
import edu.depauw.dep10.util.Word;

public class Dep10MulDiv2 {
    public static final Table table = new Table();

    /**
     * 16-bit (A) times 16-bit (operand) signed integer multiply, 32-bit result
     * high word will be in H register, low word in A
     * Z is true if full product is zero
     * N is true if full product is negative
     * C is true if full product does not fit in 16 bits (that is, if H is not just
     *   the sign-extend of A)
     * V is not affected
     */
    public static final OpCore SMULA = new OpCore("SMULA", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);
            
            var a = s.getA().signedValue();
            var op = operand.signedValue();

            var product = a * op;
            var low_bits = Word.of(product);
            var high_bits = Word.of(product >>> 16);

            s.setA(low_bits);
            s.setH(high_bits);

            s.setN(high_bits.isNegative());
            s.setZ(low_bits.isZero() && high_bits.isZero());
            s.setC(low_bits.signedValue() != product);
        }
    };

    /**
     * 16-bit (X) times 16-bit (operand) signed integer multiply, 32-bit result
     * high word will be in H register, low word in X
     * Z is true if full product is zero
     * N is true if full product is negative
     * C is true if full product does not fit in 16 bits (that is, if H is not just
     *   the sign-extend of X)
     * V is not affected
     */
    public static final OpCore SMULX = new OpCore("SMULX", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);
            
            var x = s.getX().signedValue();
            var op = operand.signedValue();

            var product = x * op;
            var low_bits = Word.of(product);
            var high_bits = Word.of(product >>> 16);

            s.setX(low_bits);
            s.setH(high_bits);

            s.setN(high_bits.isNegative());
            s.setZ(low_bits.isZero() && high_bits.isZero());
            s.setC(low_bits.signedValue() != product);
        }
    };

    /**
     * 16-bit (A) times 16-bit (operand) unsigned integer multiply, 32-bit result
     * high word will be in H register, low word in A
     * Z is true if full product is zero
     * N is true if full product is negative (as a signed 32-bit number)
     * C is true if full product does not fit in 16 bits (that is, if H is not zero)
     * V is not affected
     */
    public static final OpCore UMULA = new OpCore("UMULA", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);
            
            var a = s.getA().value();
            var op = operand.value();

            var product = a * op;
            var low_bits = Word.of(product);
            var high_bits = Word.of(product >> 16);

            s.setA(low_bits);
            s.setH(high_bits);

            s.setN(high_bits.isNegative());
            s.setZ(low_bits.isZero() && high_bits.isZero());
            s.setC(!high_bits.isZero());
        }
    };

    /**
     * 16-bit (X) times 16-bit (operand) unsigned integer multiply, 32-bit result
     * high word will be in H register, low word in X
     * Z is true if full product is zero
     * N is true if full product is negative (as a signed 32-bit number)
     * C is true if full product does not fit in 16 bits (that is, if H is not zero)
     * V is not affected
     */
    public static final OpCore UMULX = new OpCore("UMULX", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);
            
            var x = s.getX().value();
            var op = operand.value();

            var product = x * op;
            var low_bits = Word.of(product);
            var high_bits = Word.of(product >> 16);

            s.setX(low_bits);
            s.setH(high_bits);

            s.setN(high_bits.isNegative());
            s.setZ(low_bits.isZero() && high_bits.isZero());
            s.setC(!high_bits.isZero());
        }
    };
    
    // TODO UMULSA (unsigned operand, signed A)? SMULUA (signed operand, unsigned A)?

    /**
     * 32-bit (H:A) by 16-bit (operand) signed integer division, 16-bit result
     * quotient will be in A register, remainder in H
     * Z is true if quotient is zero
     * N is true if quotient is negative
     * V is true if quotient does not fit in 16 bits, or divide-by-zero
     * C is true if divide-by-zero
     * On divide-by-zero, quotient and remainder (A and H) will both be zero
     */
    public static final OpCore SDIVA = new OpCore("SDIVA", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);
            
            var dividend = (s.getH().value() << 16) + s.getA().value();
            var op = operand.signedValue();
            
            if (op == 0) {
                s.setA(Word.of(0));
                s.setH(Word.of(0));
                s.setN(false);
                s.setZ(true);
                s.setV(true);
                s.setC(true);
            } else {
                var quotient = dividend / op;
                var remainder = dividend % op;
                
                var q = Word.of(quotient);
                s.setA(q);
                s.setH(Word.of(remainder));
                s.setN(q.isNegative());
                s.setZ(q.isZero());
                s.setV(q.signedValue() != quotient);
                s.setC(false);
            }
        }
    };

    /**
     * 32-bit (H:X) by 16-bit (operand) signed integer division, 16-bit result
     * quotient will be in X register, remainder in H
     * Z is true if quotient is zero
     * N is true if quotient is negative
     * V is true if quotient does not fit in 16 bits, or divide-by-zero
     * C is true if divide-by-zero
     * On divide-by-zero, quotient and remainder (X and H) will both be zero
     */
    public static final OpCore SDIVX = new OpCore("SDIVX", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);
            
            var dividend = (s.getH().value() << 16) + s.getX().value();
            var op = operand.signedValue();
            
            if (op == 0) {
                s.setX(Word.of(0));
                s.setH(Word.of(0));
                s.setN(false);
                s.setZ(true);
                s.setV(true);
                s.setC(true);
            } else {
                var quotient = dividend / op;
                var remainder = dividend % op;
                
                var q = Word.of(quotient);
                s.setX(q);
                s.setH(Word.of(remainder));
                s.setN(q.isNegative());
                s.setZ(q.isZero());
                s.setV(q.signedValue() != quotient);
                s.setC(false);
            }
        }
    };

    /**
     * 32-bit (H:A) by 16-bit (operand) unsigned integer division, 16-bit result
     * quotient will be in A register, remainder in H
     * Z is true if quotient is zero
     * N is true if quotient is negative (as a signed 16-bit number)
     * V is true if quotient does not fit in 16 bits, or divide-by-zero
     * C is true if divide-by-zero
     * On divide-by-zero, quotient and remainder (A and H) will both be zero
     */
    public static final OpCore UDIVA = new OpCore("UDIVA", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);
            
            var dividend = ((long) s.getH().value() << 16) + s.getA().value();
            var op = operand.value();
            
            if (op == 0) {
                s.setA(Word.of(0));
                s.setH(Word.of(0));
                s.setN(false);
                s.setZ(true);
                s.setV(true);
                s.setC(true);
            } else {
                var quotient = dividend / op;
                var remainder = dividend % op;
                
                var q = Word.of((int) quotient);
                s.setA(q);
                s.setH(Word.of((int) remainder));
                s.setN(q.isNegative());
                s.setZ(q.isZero());
                s.setV(q.value() != quotient);
                s.setC(false);
            }
        }
    };

    /**
     * 32-bit (H:X) by 16-bit (operand) unsigned integer division, 16-bit result
     * quotient will be in X register, remainder in H
     * Z is true if quotient is zero
     * N is true if quotient is negative (as a signed 16-bit number)
     * V is true if quotient does not fit in 16 bits, or divide-by-zero
     * C is true if divide-by-zero
     * On divide-by-zero, quotient and remainder (X and H) will both be zero
     */
    public static final OpCore UDIVX = new OpCore("UDIVX", Modes.All) {
        public void exec(State s, Mode mode) {
            var operand = mode.resolveWord(s);
            
            var dividend = ((long) s.getH().value() << 16) + s.getX().value();
            var op = operand.value();
            
            if (op == 0) {
                s.setX(Word.of(0));
                s.setH(Word.of(0));
                s.setN(false);
                s.setZ(true);
                s.setV(true);
                s.setC(true);
            } else {
                var quotient = dividend / op;
                var remainder = dividend % op;
                
                var q = Word.of((int) quotient);
                s.setX(q);
                s.setH(Word.of((int) remainder));
                s.setN(q.isNegative());
                s.setZ(q.isZero());
                s.setV(q.value() != quotient);
                s.setC(false);
            }
        }
    };

    static {
        table.install(8, SMULA); // NOTE opcode 0 should be unimplemented in any table, except as a prefix
        table.install(16, SMULX);
        table.install(24, UMULA);
        table.install(32, UMULX);
        table.install(40, SDIVA);
        table.install(48, SDIVX);
        table.install(56, UDIVA);
        table.install(64, UDIVX);
    }
}
