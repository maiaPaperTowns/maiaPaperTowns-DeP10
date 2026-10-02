package edu.depauw.dep10.resources;

import edu.depauw.dep10.simulator.State;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static edu.depauw.dep10.resources.PepHarness.mem;

/**
 * End-to-end tests for DADD/DSUB (src/main/resources/daddsub.pep), pulled in
 * with .INCLUDELIB the same way a student or another routine would use them.
 *
 * Calling convention (as of PR #19, 9/30/2026): operands are pushed low word
 * then high word, left-to-right operand order (PUSH A; PUSH B; CALL DADD/DSUB
 * computes A + B or A - B). The result overwrites the FIRST-pushed operand's
 * slot (A's), so a plain ADDSP 4,i right after CALL discards B's now-unused
 * slot and leaves the result ready to feed into the next call in a chain.
 */
class DAddDSubTest {

    private static State run(String driverAndChecks) {
        return PepHarness.run(driverAndChecks + "\n.INCLUDELIB \"daddsub\"\n        .END\n");
    }

    @Test
    @DisplayName("DADD: 5 + 3 = 8 (Prof. Howard's original chaining example)")
    void dadd_simple() {
        State s = run("""
                LDWA    0x0005,i        ; A low
                PUSHA
                LDWA    0x0000,i        ; A high
                PUSHA
                LDWA    0x0003,i        ; B low
                PUSHA
                LDWA    0x0000,i        ; B high
                PUSHA
                CALL    DADD,i
                ADDSP   4,i
                POPA
                STWA    0xF000,d
                POPA
                STWA    0xF002,d
                LDBA    0,i
                STBA    pwrOff,d
                """);
        assertEquals(0, mem(s, 0xF000), "result high word");
        assertEquals(8, mem(s, 0xF002), "result low word");
    }

    @Test
    @DisplayName("DADD: chained A + B + C, 5 + 3 + 100 = 108")
    void dadd_chains() {
        State s = run("""
                LDWA    0x0005,i
                PUSHA
                LDWA    0x0000,i
                PUSHA
                LDWA    0x0003,i
                PUSHA
                LDWA    0x0000,i
                PUSHA
                CALL    DADD,i
                ADDSP   4,i
                LDWA    0x0064,i        ; C = 100
                PUSHA
                LDWA    0x0000,i
                PUSHA
                CALL    DADD,i
                ADDSP   4,i
                POPA
                STWA    0xF010,d
                POPA
                STWA    0xF012,d
                LDBA    0,i
                STBA    pwrOff,d
                """);
        assertEquals(0, mem(s, 0xF010));
        assertEquals(108, mem(s, 0xF012));
    }

    @Test
    @DisplayName("DADD: 70000 + 5 = 70005, exercises carry out of the low word into the high word")
    void dadd_carryIntoHighWord() {
        State s = run("""
                LDWA    0x1170,i        ; 70000 low  (70000 = 0x00011170)
                PUSHA
                LDWA    0x0001,i        ; 70000 high
                PUSHA
                LDWA    0x0005,i        ; 5 low
                PUSHA
                LDWA    0x0000,i        ; 5 high
                PUSHA
                CALL    DADD,i
                ADDSP   4,i
                POPA
                STWA    0xF020,d
                POPA
                STWA    0xF022,d
                LDBA    0,i
                STBA    pwrOff,d
                """);
        long expected = 70005L;
        assertEquals((int) (expected >>> 16) & 0xFFFF, mem(s, 0xF020), "result high word");
        assertEquals((int) (expected & 0xFFFF), mem(s, 0xF022), "result low word");
    }

    @Test
    @DisplayName("DSUB: 20 - 3 = 17 (order check: A - B, not B - A)")
    void dsub_orderIsALeftMinusBRight() {
        State s = run("""
                LDWA    0x0014,i        ; A = 20
                PUSHA
                LDWA    0x0000,i
                PUSHA
                LDWA    0x0003,i        ; B = 3
                PUSHA
                LDWA    0x0000,i
                PUSHA
                CALL    DSUB,i
                ADDSP   4,i
                POPA
                STWA    0xF030,d
                POPA
                STWA    0xF032,d
                LDBA    0,i
                STBA    pwrOff,d
                """);
        assertEquals(0, mem(s, 0xF030));
        assertEquals(17, mem(s, 0xF032), "must be 17 (A - B), not 0xFFEF (-17, B - A)");
    }

    @Test
    @DisplayName("DSUB: chained non-commutative (100 - 20) - 5 = 75, not some other grouping")
    void dsub_chainsInOrder() {
        State s = run("""
                LDWA    0x0064,i        ; A = 100
                PUSHA
                LDWA    0x0000,i
                PUSHA
                LDWA    0x0014,i        ; B = 20
                PUSHA
                LDWA    0x0000,i
                PUSHA
                CALL    DSUB,i
                ADDSP   4,i
                LDWA    0x0005,i        ; C = 5
                PUSHA
                LDWA    0x0000,i
                PUSHA
                CALL    DSUB,i
                ADDSP   4,i
                POPA
                STWA    0xF040,d
                POPA
                STWA    0xF042,d
                LDBA    0,i
                STBA    pwrOff,d
                """);
        assertEquals(0, mem(s, 0xF040));
        assertEquals(75, mem(s, 0xF042));
    }
}
