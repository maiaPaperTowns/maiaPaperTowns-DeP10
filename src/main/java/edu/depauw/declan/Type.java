package edu.depauw.declan;

public enum Type {
    BOOLEAN, INTEGER, REAL, LONGINT;

    /**
     * Number of 16-bit words a variable of this type occupies, both as a
     * global and as a local (stack) slot. Everything is 1 word today;
     * LONGINT is the first type that needs 2 (it is a 32-bit value built
     * on the DADD/DSUB/DDiv/UDDiv library). Scope/VarInfo slot allocation
     * and the Pep10/Pep10X codegen must agree with this value: codegen for
     * LONGINT loads/stores/refs does not exist yet (see
     * compiler_longint_project_scope.md), so declaring a LONGINT variable
     * will reserve the right number of slots but won't generate working
     * assembly until that codegen is written.
     */
    public int width() {
        switch (this) {
        case LONGINT:
            return 2;
        default:
            return 1;
        }
    }
}
