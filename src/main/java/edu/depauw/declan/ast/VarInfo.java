package edu.depauw.declan.ast;

import edu.depauw.declan.Type;

public class VarInfo {
    public Type type;
    public int slot;
    public boolean isVarParam;

    public VarInfo(Type type, boolean isVarParam) {
        this.type = type;
        this.slot = 0;
        this.isVarParam = isVarParam;
    }

    public boolean isConstant() {
        return false;
    }

    /**
     * Number of consecutive slots this variable occupies (see Type#width).
     * Some VarInfo instances are sentinels with no real type (e.g. the
     * "_return_address" slot TypeChecker reserves in each procedure's
     * frame, see TypeChecker#visitProcedure) and always occupy exactly 1
     * slot.
     */
    public int width() {
        return type == null ? 1 : type.width();
    }

    @Override
    public String toString() {
        return type.toString() + " #" + slot;
    }

    public void setSlot(int slotNumber) {
        this.slot = slotNumber;
    }
}
