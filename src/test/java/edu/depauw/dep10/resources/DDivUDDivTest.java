package edu.depauw.dep10.resources;

import edu.depauw.dep10.simulator.State;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static edu.depauw.dep10.resources.PepHarness.mem;

/**
 * End-to-end tests for DDiv/UDDiv (src/main/resources/ddiv32.pep), pulled in
 * with .INCLUDELIB.
 *
 * Calling convention (as of PR #20, 9/30/2026, matching the DADD/DSUB/DMUL
 * convention from PR #19 and the real register-based DIV/MOD/DM instructions):
 * operands are pushed low word then high word, dividend first then divisor
 * (PUSH dividend; PUSH divisor; CALL DDiv/UDDiv). The quotient overwrites the
 * dividend's (first-pushed) slot; the remainder overwrites the divisor's
 * (last-pushed) slot. A plain ADDSP 4,i right after CALL discards the
 * remainder and leaves the quotient ready to feed into the next call in a
 * chain of divisions.
 */
class DDivUDDivTest {

    private static State run(String driverAndChecks) {
        return PepHarness.run(driverAndChecks + "\n.INCLUDELIB \"ddiv32\"\n        .END\n");
    }

    @Test
    @DisplayName("UDDiv: 100 / 7 = 14 remainder 2, fast (16-iteration) path")
    void uddiv_fastPath() {
        State s = run("""
                LDWA    0x0064,i        ; dividend low (100)
                PUSHA
                LDWA    0x0000,i        ; dividend high
                PUSHA
                LDWA    0x0007,i        ; divisor low (7)
                PUSHA
                LDWA    0x0000,i        ; divisor high
                PUSHA
                CALL    UDDiv,i
                POPA
                STWA    0xF000,d        ; remainder high (divisor's slot pops first)
                POPA
                STWA    0xF002,d        ; remainder low
                POPA
                STWA    0xF004,d        ; quotient high
                POPA
                STWA    0xF006,d        ; quotient low
                LDBA    0,i
                STBA    pwrOff,d
                """);
        assertEquals(0, mem(s, 0xF000), "remainder high word");
        assertEquals(2, mem(s, 0xF002), "remainder low word");
        assertEquals(0, mem(s, 0xF004), "quotient high word");
        assertEquals(14, mem(s, 0xF006), "quotient low word");
    }

    @Test
    @DisplayName("UDDiv: chained (1000 / 7) / 3 = 47 remainder 1")
    void uddiv_chains() {
        State s = run("""
                LDWA    0x03E8,i        ; dividend low (1000)
                PUSHA
                LDWA    0x0000,i
                PUSHA
                LDWA    0x0007,i        ; divisor low (7)
                PUSHA
                LDWA    0x0000,i
                PUSHA
                CALL    UDDiv,i
                ADDSP   4,i             ; discard the remainder, keep chaining on the quotient
                LDWA    0x0003,i        ; next divisor (3)
                PUSHA
                LDWA    0x0000,i
                PUSHA
                CALL    UDDiv,i
                POPA
                STWA    0xF010,d        ; remainder high
                POPA
                STWA    0xF012,d        ; remainder low
                POPA
                STWA    0xF014,d        ; quotient high
                POPA
                STWA    0xF016,d        ; quotient low
                LDBA    0,i
                STBA    pwrOff,d
                """);
        assertEquals(0, mem(s, 0xF010));
        assertEquals(1, mem(s, 0xF012), "final remainder");
        assertEquals(0, mem(s, 0xF014));
        assertEquals(47, mem(s, 0xF016), "final quotient");
    }

    @Test
    @DisplayName("UDDiv: general (32-iteration) path, quotient needs both words: 458757 / 2 = 229378 r 1")
    void uddiv_generalPathTwoWordQuotient() {
        State s = run("""
                LDWA    0x0005,i        ; dividend low (458757 = 0x0007_0005)
                PUSHA
                LDWA    0x0007,i        ; dividend high
                PUSHA
                LDWA    0x0002,i        ; divisor low (2, forces the general path since
                PUSHA                   ; the quotient would overflow the fast path's one word)
                LDWA    0x0000,i        ; divisor high
                PUSHA
                CALL    UDDiv,i
                POPA
                STWA    0xF020,d        ; remainder high
                POPA
                STWA    0xF022,d        ; remainder low
                POPA
                STWA    0xF024,d        ; quotient high
                POPA
                STWA    0xF026,d        ; quotient low
                LDBA    0,i
                STBA    pwrOff,d
                """);
        assertEquals(0, mem(s, 0xF020));
        assertEquals(1, mem(s, 0xF022), "remainder");
        assertEquals(0x0003, mem(s, 0xF024), "quotient high word");
        assertEquals(0x8002, mem(s, 0xF026), "quotient low word");
    }

    @Test
    @DisplayName("DDiv (signed): -100 / 7 = -14 (truncating toward zero)")
    void ddiv_signedNegativeDividend() {
        State s = run("""
                LDWA    0xFF9C,i        ; dividend low (-100 low word)
                PUSHA
                LDWA    0xFFFF,i        ; dividend high (sign-extended)
                PUSHA
                LDWA    0x0007,i        ; divisor low (7)
                PUSHA
                LDWA    0x0000,i        ; divisor high
                PUSHA
                CALL    DDiv,i
                ADDSP   4,i             ; only checking the quotient here
                POPA
                STWA    0xF030,d
                POPA
                STWA    0xF032,d
                LDBA    0,i
                STBA    pwrOff,d
                """);
        assertEquals(0xFFFF, mem(s, 0xF030), "quotient high word, sign-extended -14");
        assertEquals(0xFFF2, mem(s, 0xF032), "quotient low word, -14");
    }

    @Test
    @DisplayName("UDDiv: divide by zero leaves quotient and remainder as 0 and sets N=0,Z=1,V=1,C=1")
    void uddiv_divideByZeroContract() {
        State s = run("""
                LDWA    0x0005,i        ; dividend = 5
                PUSHA
                LDWA    0x0000,i
                PUSHA
                LDWA    0x0000,i        ; divisor = 0
                PUSHA
                LDWA    0x0000,i
                PUSHA
                CALL    UDDiv,i
                POPA
                STWA    0xF040,d
                POPA
                STWA    0xF042,d
                POPA
                STWA    0xF044,d
                POPA
                STWA    0xF046,d
                LDBA    0,i
                STBA    pwrOff,d
                """);
        assertEquals(0, mem(s, 0xF040));
        assertEquals(0, mem(s, 0xF042));
        assertEquals(0, mem(s, 0xF044));
        assertEquals(0, mem(s, 0xF046));
        assertFalse(s.getN(), "N should be 0");
        assertTrue(s.getZ(), "Z should be 1");
        assertTrue(s.getV(), "V should be 1");
        assertTrue(s.getC(), "C should be 1 (divide-by-zero signal)");
    }
}
