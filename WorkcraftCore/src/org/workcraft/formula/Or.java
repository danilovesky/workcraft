package org.workcraft.formula;

import org.workcraft.formula.visitors.BooleanVisitor;

public class Or extends BinaryBooleanFormula {

    public Or(BooleanFormula x, BooleanFormula y) {
        super(x, y);
    }

    @Override
    public <T> T accept(BooleanVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
