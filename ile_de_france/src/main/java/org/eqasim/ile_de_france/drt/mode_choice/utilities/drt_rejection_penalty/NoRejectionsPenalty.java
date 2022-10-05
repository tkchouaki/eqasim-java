package org.eqasim.ile_de_france.drt.mode_choice.utilities.drt_rejection_penalty;

public class NoRejectionsPenalty implements DrtRejectionPenaltyProvider{
    @Override
    public double getRejectionPenalty() {
        return 0;
    }
}
