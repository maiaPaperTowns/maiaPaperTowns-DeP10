package edu.depauw.dep10.op;

import edu.depauw.dep10.simulator.State;
import edu.depauw.dep10.util.Word;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Dep10MulDiv2 (h_register branch): the H-register-based 32-bit
 * multiply/divide instructions. Unlike MulDivTest's freshState(), which sets
 * A and X to the same value, freshState() here gives A, X, and H distinct
 * sentinel values on purpose -- that's what actually catches a register
 * mix-up like "X-variant silently reads/writes A instead of X". A shared
 * A/X value would let that class of bug through undetected.
 *
 * See Issue #10 (github.com/bhoward/DeP10/issues/10) for the three bugs
 * these regression tests were written against: UMULX, SDIVX (zero-divide
 * branch), and UDIVX.
 */
public class MulDiv2Test {

    private static State freshState(int aValue, int xValue, int hValue, int operandValue) {
        State s = new State();
        s.setA(Word.of(aValue));
        s.setX(Word.of(xValue));
        s.setH(Word.of(hValue));
        s.setOperand(Word.of(operandValue));
        return s;
    }

    @Nested
    @DisplayName("UMULX (regression: used to write its result into A instead of X)")
    class UmulxRegression {

        @Test
        @DisplayName("UMULX: 7 * 6 = 42 lands in X; A is untouched")
        void testUmulx_writesToX_notA() {
            State s = freshState(999, 7, 0, 6);
            Dep10MulDiv2.UMULX.exec(s, Mode.I);
            assertEquals(42, s.getX().value(), "product should land in X, not A");
            assertEquals(999, s.getA().value(), "UMULX must not touch A");
            assertEquals(0, s.getH().value(), "no overflow expected for this product");
        }

        @Test
        @DisplayName("UMULX: product overflowing 16 bits sets C and splits correctly across X/H")
        void testUmulx_overflowSetsCarryAndSplitsAcrossXAndH() {
            State s = freshState(0, 60000, 0, 60000);
            Dep10MulDiv2.UMULX.exec(s, Mode.I);
            long product = 60000L * 60000L; // 3,600,000,000 -- doesn't fit in 16 bits
            assertEquals((int) (product & 0xFFFF), s.getX().value(), "low word in X");
            assertEquals((int) ((product >>> 16) & 0xFFFF), s.getH().value(), "high word in H");
            assertTrue(s.getC(), "C should be set since H is nonzero (result overflows 16 bits)");
        }
    }

    @Nested
    @DisplayName("SDIVX divide-by-zero (regression: used to zero A instead of X)")
    class SdivxZeroRegression {

        @Test
        @DisplayName("SDIVX / 0: X is zeroed, A is untouched, C and V are set")
        void testSdivx_divideByZero_zeroesX_notA() {
            State s = freshState(555, 1234, 0, 0);
            Dep10MulDiv2.SDIVX.exec(s, Mode.I);
            assertEquals(0, s.getX().value(), "X (the quotient register for SDIVX) must be zeroed");
            assertEquals(555, s.getA().value(), "A must be untouched by SDIVX's divide-by-zero path");
            assertEquals(0, s.getH().value(), "H (remainder) must also be zeroed");
            assertTrue(s.getC(), "C should be set on divide-by-zero");
            assertTrue(s.getV(), "V should be set on divide-by-zero");
        }

        @Test
        @DisplayName("SDIVX normal path (non-zero divisor) still works and still leaves A alone")
        void testSdivx_normalPath_stillCorrect() {
            // dividend = H:X = 0:17, divisor = 5 -> quotient 3, remainder 2
            State s = freshState(777, 17, 0, 5);
            Dep10MulDiv2.SDIVX.exec(s, Mode.I);
            assertEquals(3, s.getX().value());
            assertEquals(2, s.getH().value());
            assertEquals(777, s.getA().value(), "A must be untouched by SDIVX's normal path");
            assertFalse(s.getC());
        }
    }

    @Nested
    @DisplayName("UDIVX (regression: used to read/write A throughout, an exact copy of UDIVA)")
    class UdivxRegression {

        @Test
        @DisplayName("UDIVX: 100 / 5 = 20 lands in X; A is untouched")
        void testUdivx_readsAndWritesX_notA() {
            State s = freshState(0, 100, 0, 5);
            Dep10MulDiv2.UDIVX.exec(s, Mode.I);
            assertEquals(20, s.getX().value(), "quotient should land in X, not A");
            assertEquals(0, s.getA().value(), "UDIVX must not touch A");
        }

        @Test
        @DisplayName("UDIVX: dividend is read from H:X, not H:A")
        void testUdivx_readsDividendFromX_notA() {
            // If UDIVX mistakenly reads from A (as in the original bug), this
            // would divide 0 by 5 and give quotient 0 instead of 20.
            State s = freshState(999999 & 0xFFFF, 100, 0, 5);
            Dep10MulDiv2.UDIVX.exec(s, Mode.I);
            assertEquals(20, s.getX().value(), "dividend must come from X, not from whatever is in A");
        }

        @Test
        @DisplayName("UDIVX / 0: X is zeroed, A is untouched")
        void testUdivx_divideByZero_zeroesX_notA() {
            State s = freshState(42, 999, 0, 0);
            Dep10MulDiv2.UDIVX.exec(s, Mode.I);
            assertEquals(0, s.getX().value());
            assertEquals(42, s.getA().value(), "A must be untouched by UDIVX's divide-by-zero path");
            assertTrue(s.getC());
            assertTrue(s.getV());
        }
    }

    @Nested
    @DisplayName("A-variants (regression: confirm the X-variant fixes didn't break these)")
    class AVariantsUnaffected {

        @Test
        @DisplayName("UMULA: 7 * 6 = 42 still lands in A")
        void testUmula_stillCorrect() {
            State s = freshState(7, 999, 0, 6);
            Dep10MulDiv2.UMULA.exec(s, Mode.I);
            assertEquals(42, s.getA().value());
        }

        @Test
        @DisplayName("UDIVA: 100 / 5 = 20 still lands in A; X untouched")
        void testUdiva_stillCorrect() {
            State s = freshState(100, 111, 0, 5);
            Dep10MulDiv2.UDIVA.exec(s, Mode.I);
            assertEquals(20, s.getA().value());
            assertEquals(111, s.getX().value(), "UDIVA must not touch X");
        }

        @Test
        @DisplayName("SDIVA / 0: A is zeroed (its own divide-by-zero path was never buggy)")
        void testSdiva_divideByZero_stillCorrect() {
            State s = freshState(1234, 555, 0, 0);
            Dep10MulDiv2.SDIVA.exec(s, Mode.I);
            assertEquals(0, s.getA().value());
            assertTrue(s.getC());
            assertTrue(s.getV());
        }
    }
}
