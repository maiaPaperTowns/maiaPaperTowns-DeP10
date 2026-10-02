;=======================================================================
; DADD / DSUB -- 32-bit + 32-bit -> 32-bit, and 32-bit - 32-bit -> 32-bit,
; matching Java/C++ int semantics: silently wrap/truncate on overflow,
; no trap.
;
; Register usage: both routines only touch register A. X is left
; completely untouched, so callers don't need to worry about these
; calls trashing whatever they were keeping in X. (Contrast this with
; the floating-point routines, which use both A and X extensively and
; so save/restore X themselves on entry/exit.)
;
; v2 CONVENTION CHANGE (per Brian, 9/30): the result now overwrites the
; FIRST-PUSHED operand's slot (not the last-pushed one), so that a plain
; ADDSP 4,i right after CALL discards the correct leftover and chains
; cleanly:
;   PUSH A
;   PUSH B
;   CALL DADD      ; computes A + B, result replaces A's slot
;   ADDSP 4,i       ; discards B's now-unused slot
;   PUSH C
;   CALL DADD      ; computes (A+B) + C
;   ADDSP 4,i
;   POPA / POPA    ; final result
;
; For DSUB, push order is left-to-right operand order: PUSH A; PUSH B;
; CALL DSUB computes A - B (not B - A), matching how the result chains.
;
; Stack layout at entry (SP,0 is the return address):
;   SP,2  b high word (last-pushed)     SP,6  a high word (first-pushed)
;   SP,4  b low word                    SP,8  a low word
;
; Result overwrites a's slot (the first-pushed one):
;   SP,6  result high word
;   SP,8  result low word
; b's slot (SP,2/SP,4) is left untouched; caller discards it with
; ADDSP 4,i right after the call.
;
; Caller looks like:
;   LDWA  aLow,i  / PUSHA
;   LDWA  aHigh,i / PUSHA
;   LDWA  bLow,i  / PUSHA
;   LDWA  bHigh,i / PUSHA
;   CALL  DADD,i           ; or DSUB,i
;   ADDSP 4,i               ; discard b's now-unused slot
;   POPA
;   STWA  resultHigh,d
;   POPA
;   STWA  resultLow,d
;=======================================================================

DADD:
        LDWA    8,s             ; a low  (first-pushed, deep)
        ADDA    4,s             ; + b low (last-pushed, shallow); C = carry out
        STWA    8,s             ; result low -> a's slot
        BRC     _DAddCarryIn
        LDWA    6,s             ; a high
        ADDA    2,s             ; + b high (no carry-in)
        STWA    6,s             ; result high -> a's slot
        RET
_DAddCarryIn:
        LDWA    6,s             ; a high
        ADDA    2,s             ; + b high
        ADDA    1,i             ; + carry-in from the low word
        STWA    6,s             ; result high -> a's slot
        RET

DSUB:
        LDWA    8,s             ; a low
        SUBA    4,s             ; - b low; C=1 means "no borrow needed" (Issue #11 fix),
        STWA    8,s             ; result low -> a's slot    C=0 means a borrow was needed
        BRC     _DSubNoBorrow
        LDWA    6,s             ; a high
        SUBA    2,s             ; - b high
        SUBA    1,i             ; - the borrow from the low word
        STWA    6,s             ; result high -> a's slot
        RET
_DSubNoBorrow:
        LDWA    6,s             ; a high
        SUBA    2,s             ; - b high (no borrow-in)
        STWA    6,s             ; result high -> a's slot
        RET
