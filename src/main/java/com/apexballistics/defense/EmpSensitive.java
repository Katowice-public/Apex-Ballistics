package com.apexballistics.defense;

public interface EmpSensitive {
    void disableFor(int ticks);

    boolean isEmpDisabled();
}
