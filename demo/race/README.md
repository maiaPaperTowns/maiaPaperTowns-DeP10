# Two Threads, One Printer: preemption exercise

Files for an exercise on code that is not safe to interrupt.

- `race_starter.pep` is what students receive. `printLong` and `uPrintLong`
  keep their working variables in static storage (`.BLOCK`), so they only work
  if nothing interrupts them. Students rewrite the two routines between the
  `STUDENT CODE` markers to use a stack frame.
- `race_solution.pep` is the instructor solution. Do not distribute it.

Two threads each print five 32-bit numbers, one per line. A timer interrupt
switches threads every `quantum` user instructions (`quantum:` near the top of
the file), so a switch can land in the middle of `printLong`.

This needs the fancy machine (user mode and the timer at `0xFFF5`):

    run --fancy --bm <object file>

With the starter, a quantum of 0 (no timer) prints everything correctly, and
so do some other quanta; many quanta garble a number because both threads
share the same digit buffer and counter. The two threads' characters always
interleave on the console because they share one output device, so a student
who wants to see the bug clearly should compare each thread's own numbers
(thread 1 prints `2147483647`, `-2147483648`, `0`, `1234567`, `-90`; thread 2
prints `100000`, `-1`, `2000000000`, `7`, `-305419896`). The kernel writes the
running thread's number (1 or 2) at `0xF000` for the test to use.

`RaceExerciseTest` checks that the starter works with no timer, that it fails
for many quanta, that the solution is correct for 38 quanta between 1 and 4001,
and that the two files differ only inside the student block.
