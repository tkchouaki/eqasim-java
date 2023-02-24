package org.eqasim.ile_de_france.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.WalkVariables;

public class IncentivizedWalkVariables extends WalkVariables {

    final public double monetaryGain;

    public IncentivizedWalkVariables(double travelTime_min, double monetaryGain) {
        super(travelTime_min);
        this.monetaryGain = monetaryGain;
    }
}
