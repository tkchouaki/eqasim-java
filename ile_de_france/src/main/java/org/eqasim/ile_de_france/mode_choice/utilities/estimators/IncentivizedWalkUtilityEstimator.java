package org.eqasim.ile_de_france.mode_choice.utilities.estimators;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.WalkUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.variables.WalkVariables;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IncentivizedWalkPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IncentivizedWalkVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class IncentivizedWalkUtilityEstimator extends WalkUtilityEstimator {

    @Inject
    public IncentivizedWalkUtilityEstimator(ModeParameters parameters, IncentivizedWalkPredictor predictor) {
        super(parameters, predictor);
    }

    protected double estimateMonetaryGainUtility(IncentivizedWalkVariables variables) {
        return - this.getParameters().betaCost_u_MU * variables.monetaryGain;
    }

    @Override
    public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        WalkVariables variables = this.getPredictor().predictVariables(person, trip, elements);
        assert variables instanceof IncentivizedWalkVariables;
        double utility = 0.0;
        utility += estimateConstantUtility();
        utility += estimateTravelTimeUtility(variables);
        utility += estimateMonetaryGainUtility((IncentivizedWalkVariables) variables);
        return utility;
    }
}
