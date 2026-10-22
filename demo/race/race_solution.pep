; Two Threads, One Printer: a preemption exercise.
;
; Two user programs, A and B, each print a list of 32-bit numbers, one per
; line, with printLong. A timer interrupt switches between them every
; "quantum" user instructions, wherever they happen to be, even in the
; middle of printLong. The numbers each program prints should come out
; whole, no matter where the switches land.
;
; This program needs the "fancy machine" (supervisor/user modes and a timer).
; From a terminal:   run --fancy --bm <object file>
;
; How the kernel works
; --------------------
; The machine counts user-mode instructions down in the word at 0xFFF5. When
; it reaches zero it traps to the handler like a system call does, with
; opcode byte 0 and operand 1 in the trap frame (an invalid instruction is
; opcode byte = the bad opcode, operand 2). The handler saves the running
; thread, picks the other one, sets the timer again and SRETs into it.
; SCALL 1 = EXIT. The kernel also writes the number of the running thread
; (1 or 2) at 0xF000 so a test can tell whose output is whose.
;
; What to do
; ----------
; printLong and uPrintLong between the STUDENT CODE markers keep their
; working variables in static storage (.BLOCK). Both threads share those
; words, so a switch in the middle of a print lets the other thread
; overwrite them. Run the program with a few different quanta, find a
; garbled number, then rewrite the two routines so that all working
; variables live in a stack frame (SUBSP on entry, ADDSP before RET).
; Remember that every PUSHA moves SP, so ",s" offsets move with it.
;
; Process table entry (14 bytes):
;     0 state    1 ready, 2 running, 3 done
;     2 saved SP (address of the process's saved trap frame)
;     4 saved H
;     6 next     the other process's table entry
;     8 top of the process's user stack
;    10 top of the process's system stack
;    12 thread number (1 or 2)

        BR      main

READY:    .EQUATE 1
RUNNING:  .EQUATE 2
DONE:     .EQUATE 3

stState:  .EQUATE 0
stSP:     .EQUATE 2
stH:      .EQUATE 4
stNext:   .EQUATE 6
stUser:   .EQUATE 8
stSys:    .EQUATE 10
stId:     .EQUATE 12

quantum:  .EQUATE 40            ; user instructions between timer traps (0 = no timer)

cur:      .WORD   0             ; address of the running process's table entry
tmpEntry: .WORD   0
tmpPcb:   .WORD   0

pcbA:     .WORD   0
          .WORD   0
          .WORD   0
          .WORD   pcbB
          .WORD   userAT
          .WORD   sysAT
          .WORD   1
pcbB:     .WORD   0
          .WORD   0
          .WORD   0
          .WORD   pcbA
          .WORD   userBT
          .WORD   sysBT
          .WORD   2

mainS:    .BLOCK  16
mainT:    .WORD   0
userA:    .BLOCK  128
userAT:   .WORD   0
userB:    .BLOCK  128
userBT:   .WORD   0
sysA:     .BLOCK  64
sysAT:    .WORD   0
sysB:     .BLOCK  64
sysBT:    .WORD   0

;----------------------------------------------------------------------
; Trap handler. The frame's opcode byte is at 9,s and its operand at 10,s.
;----------------------------------------------------------------------
handler:  LDWA    0,i
          LDBA    9,s
          BRNE    scall         ; a nonzero opcode byte is a system call
          LDWA    10,s          ; opcode 0: a trap from the machine itself
          CPWA    1,i
          BREQ    yield         ; 1 = timer: time for the other thread
          BR      exit          ; 2 = invalid instruction: end this thread
scall:    LDWA    10,s
          CPWA    1,i
          BREQ    exit          ; SCALL 1 = EXIT
          LDWA    0x0BAD,i      ; anything else: record it and stop
          STWA    0xF004,d
          BR      halt

; yield: save this thread, choose the next one, resume it.
yield:    MOVSPA                ; A = address of the frame the trap just pushed
          LDWX    cur,d
          STWA    stSP,x
          SWAPHA                ; traps do not save H, so we do
          STWA    stH,x
          LDWA    READY,i
          STWA    stState,x
          LDWX    stNext,x      ; X = the other thread
          LDWA    stState,x
          CPWA    READY,i
          BREQ    chosen
          LDWX    cur,d         ; the other one is done: keep running this one
chosen:   STWX    cur,d
          LDWA    RUNNING,i
          STWA    stState,x
          BR      resume

; exit: this thread is finished. Run the other one if it can run, else stop.
exit:     LDWX    cur,d
          LDWA    DONE,i
          STWA    stState,x
          LDWX    stNext,x
          LDWA    stState,x
          CPWA    READY,i
          BRNE    halt
          STWX    cur,d
          LDWA    RUNNING,i
          STWA    stState,x
          BR      resume

; resume: restore H and SP from cur's table entry, start the timer, SRET into it
resume:   LDWX    cur,d
          LDWA    stId,x
          STWA    0xF000,d      ; which thread is running now
          LDWA    stH,x
          SWAPHA                ; H = saved H
          LDWA    stSP,x
          MOVASP                ; SP = the frame to return from
          LDWA    quantum,i
          STWA    0xFFF5,d      ; the timer counts user-mode instructions only
          SRET

halt:     LDBA    0,i
          STBA    pwrOff,d

;----------------------------------------------------------------------
; create: A = entry point, X = process table entry.
; Builds the first frame at the top of the process's system stack.
;----------------------------------------------------------------------
create:   STWA    tmpEntry,d
          STWX    tmpPcb,d
          LDWA    stSys,x
          SUBA    12,i
          STWA    stSP,x
          LDWX    stSP,x
          LDWA    tmpEntry,d
          STWA    5,x           ; frame: return PC = entry point
          LDWX    tmpPcb,d
          LDWA    stUser,x
          LDWX    stSP,x
          STWA    7,x           ; frame: user SP = top of user stack
          LDWX    tmpPcb,d
          LDWA    READY,i
          STWA    stState,x
          RET

main:     LDWA    mainT,i
          MOVASP
          LDWA    handler,i
          STWA    0xFFF7,d      ; install the trap handler
          LDWA    progA,i
          LDWX    pcbA,i
          CALL    create
          LDWA    progB,i
          LDWX    pcbB,i
          CALL    create
          LDWA    pcbA,i        ; A runs first
          STWA    cur,d
          LDWX    pcbA,i
          LDWA    RUNNING,i
          STWA    stState,x
          BR      resume

;----------------------------------------------------------------------
; The two user programs: print five numbers each, one per line.
;----------------------------------------------------------------------
progA:
          LDWA    0xFFFF,i
          PUSHA
          LDWA    0x7FFF,i
          PUSHA
          CALL    printLong,i
          ADDSP   4,i
          LDBA    '\n',i
          STBA    charOut,d
          LDWA    0x0000,i
          PUSHA
          LDWA    0x8000,i
          PUSHA
          CALL    printLong,i
          ADDSP   4,i
          LDBA    '\n',i
          STBA    charOut,d
          LDWA    0x0000,i
          PUSHA
          LDWA    0x0000,i
          PUSHA
          CALL    printLong,i
          ADDSP   4,i
          LDBA    '\n',i
          STBA    charOut,d
          LDWA    0xD687,i
          PUSHA
          LDWA    0x0012,i
          PUSHA
          CALL    printLong,i
          ADDSP   4,i
          LDBA    '\n',i
          STBA    charOut,d
          LDWA    0xFFA6,i
          PUSHA
          LDWA    0xFFFF,i
          PUSHA
          CALL    printLong,i
          ADDSP   4,i
          LDBA    '\n',i
          STBA    charOut,d
          SCALL   1,i           ; EXIT

progB:
          LDWA    0x86A0,i
          PUSHA
          LDWA    0x0001,i
          PUSHA
          CALL    printLong,i
          ADDSP   4,i
          LDBA    '\n',i
          STBA    charOut,d
          LDWA    0xFFFF,i
          PUSHA
          LDWA    0xFFFF,i
          PUSHA
          CALL    printLong,i
          ADDSP   4,i
          LDBA    '\n',i
          STBA    charOut,d
          LDWA    0x9400,i
          PUSHA
          LDWA    0x7735,i
          PUSHA
          CALL    printLong,i
          ADDSP   4,i
          LDBA    '\n',i
          STBA    charOut,d
          LDWA    0x0007,i
          PUSHA
          LDWA    0x0000,i
          PUSHA
          CALL    printLong,i
          ADDSP   4,i
          LDBA    '\n',i
          STBA    charOut,d
          LDWA    0xA988,i
          PUSHA
          LDWA    0xEDCB,i
          PUSHA
          CALL    printLong,i
          ADDSP   4,i
          LDBA    '\n',i
          STBA    charOut,d
          SCALL   1,i           ; EXIT

;----------------------------------------------------------------------
; printLong and uPrintLong. Calling convention (the same as the int32
; library): push the value's low word, then its high word, CALL, then
; ADDSP 4,i.  printLong prints a signed value, uPrintLong an unsigned one.
; Nothing is returned.
;----------------------------------------------------------------------
; ---- BEGIN STUDENT CODE
; uPrintLong frame (SP-relative right after SUBSP 16):
;   0..9 digit buffer (least significant digit first), 10 digit count,
;   12 current value high, 14 current value low
ulDigits: .EQUATE 0
ulCount:  .EQUATE 10
ulCurHi:  .EQUATE 12
ulCurLo:  .EQUATE 14
ulFrame:  .EQUATE 16

ulValHi: .EQUATE 2      ; offset from SP at uPrintLong entry: value hi word
ulValLo: .EQUATE 4      ; value lo word
ulArgHi: .EQUATE 18     ; the same operands once the frame is allocated
ulArgLo: .EQUATE 20

uPrintLong:
        LDWA    ulValHi,s
        ORA     ulValLo,s
        BRNE    ulNonzero
        LDBA    '0',i
        STBA    charOut,d
        RET

ulNonzero:
        SUBSP   ulFrame,i
        LDWA    ulArgHi,s
        STWA    ulCurHi,s
        LDWA    ulArgLo,s
        STWA    ulCurLo,s
        LDWX    0,i
        STWX    ulCount,s

ulLoop:
        LDWA    ulCurHi,s
        ORA     ulCurLo,s
        BREQ    ulPrint

        ; push current value (low, high) and divisor 10 (low, high).
        ; Each PUSHA moves SP down 2, so the frame fields are read at the
        ; offsets they have at that moment (curLo at 14, curHi then at 14).
        LDWA    ulCurLo,s
        PUSHA
        LDWA    14,s            ; ulCurHi, 2 bytes further from SP now
        PUSHA
        LDWA    10,i
        PUSHA
        LDWA    0,i
        PUSHA
        CALL    UDDiv,i
        ; the 8 bytes pushed above are still on the stack, results in place:
        ; 0 remHi, 2 remLo, 4 quoHi, 6 quoLo.  The frame is 8 bytes further.
        LDWA    4,s
        STWA    20,s            ; ulCurHi + 8
        LDWA    6,s
        STWA    22,s            ; ulCurLo + 8
        LDWA    2,s             ; remainder, always below 10
        ADDA    '0',i
        LDWX    18,s            ; ulCount + 8
        STBA    8,sx            ; ulDigits + 8, indexed by the count
        ADDX    1,i
        STWX    18,s
        ADDSP   8,i

        BR      ulLoop

ulPrint:
        LDWX    ulCount,s
        SUBX    1,i
ulPrintLoop:
        BRLT    ulDone
        LDBA    ulDigits,sx
        STBA    charOut,d
        SUBX    1,i
        BR      ulPrintLoop
ulDone:
        ADDSP   ulFrame,i
        RET

slValHi: .EQUATE 2
slValLo: .EQUATE 4

printLong:
        ; No cached copy of the operand is needed: each PUSHA moves SP, so
        ; the operand is re-read at its adjusted offset every time.
        LDWA    slValHi,s
        BRLT    slNegative
        ; non-negative: pass the operand straight to uPrintLong
        ; (push low, then high, matching its calling convention)
        LDWA    slValLo,s
        PUSHA
        LDWA    4,s             ; slValHi, 2 bytes further from SP now
        PUSHA
        CALL    uPrintLong,i
        ADDSP   4,i
        RET

slNegative:
        LDBA    '-',i
        STBA    charOut,d
        ; compute 0 - value using the already-verified DSUB routine,
        ; which correctly handles the INT32_MIN edge case: negating
        ; 0x80000000 wraps back to 0x80000000, i.e. 2147483648
        ; unsigned, which is exactly the right magnitude to print.
        LDWA    0,i
        PUSHA                   ; a low
        PUSHA                   ; a high
        LDWA    8,s             ; slValLo, 4 bytes further from SP now
        PUSHA
        LDWA    8,s             ; slValHi, 6 bytes further from SP now
        PUSHA
        CALL    DSUB,i
        ADDSP   4,i
        ; magnitude (low, high) now on top of stack; just pass straight
        ; through to uPrintLong using the same convention
        CALL    uPrintLong,i
        ADDSP   4,i
        RET
; ---- END STUDENT CODE


        .INCLUDELIB "int32"      ; UDDiv and DSUB
        .END
