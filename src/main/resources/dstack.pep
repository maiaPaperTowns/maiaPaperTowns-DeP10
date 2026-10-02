;=======================================================================
; dstack.pep -- caller-side stack manipulation macros, single-word and
; double-word (32-bit). Per Prof. Howard: these should not need to know
; what is semantically on the stack, just where the boundary between
; words (or 32-bit pairs of words) falls, so a caller can set up or tear
; down the stack around a CALL without hand-tracking SP offsets or the
; return address.
;
; These are preprocessor macros (.DEFMACRO), NOT callable routines, so
; unlike daddsub.pep/ddiv32.pep they must be pulled in with .INCLUDELIB
; BEFORE first use, not after: macro definitions have to be seen by the
; preprocessor before the @NAME invocation that expands them, while a
; callable routine only has to exist somewhere the assembler can see it
; (conventionally after the halt, so execution doesn't fall into it).
;
; 32-bit values on the stack are always high word at the lower offset,
; low word at the next word up (SP,0 = high, SP,2 = low for the
; topmost value), matching the convention used throughout DADD/DSUB/
; DMUL/DDiv/UDDiv.
;
; None of these preserve NZVC in any meaningful way; they're pure data
; movement, not arithmetic, so don't rely on the flags after using one.
;=======================================================================

; ---- single-word (16-bit) ----

.DEFMACRO DUP, 0
; Duplicates the top word on the stack. Before: SP,0 = v. After:
; SP,0 = v, SP,2 = v (the original v, now one slot deeper).
        SUBSP   2,i
        LDWA    2,s
        STWA    0,s
.ENDMACRO

.DEFMACRO DROP, 0
; Discards the top word on the stack.
        ADDSP   2,i
.ENDMACRO

.DEFMACRO SWAP, 0
; Swaps the top two words on the stack. Before: SP,0 = a, SP,2 = b.
; After: SP,0 = b, SP,2 = a.
        LDWA    0,s
        LDWX    2,s
        STWA    2,s
        STWX    0,s
.ENDMACRO

; ---- double-word (32-bit) ----

.DEFMACRO DDUP, 0
; Duplicates the top 32-bit value on the stack. Before: SP,0/SP,2 =
; v's high/low words. After: SP,0/SP,2 = v's high/low, SP,4/SP,6 =
; the original v's high/low, now one 32-bit slot deeper.
        SUBSP   4,i
        LDWA    4,s
        STWA    0,s
        LDWA    6,s
        STWA    2,s
.ENDMACRO

.DEFMACRO DDROP, 0
; Discards the top 32-bit value on the stack.
        ADDSP   4,i
.ENDMACRO

.DEFMACRO DSWAP, 0
; Swaps the top two 32-bit values on the stack. Before: SP,0/SP,2 = a's
; high/low, SP,4/SP,6 = b's high/low. After: SP,0/SP,2 = b's high/low,
; SP,4/SP,6 = a's high/low.
        LDWA    0,s
        LDWX    4,s
        STWA    4,s
        STWX    0,s
        LDWA    2,s
        LDWX    6,s
        STWA    6,s
        STWX    2,s
.ENDMACRO
