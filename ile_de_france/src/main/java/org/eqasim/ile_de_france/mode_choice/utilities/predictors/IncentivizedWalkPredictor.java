package org.eqasim.ile_de_france.mode_choice.utilities.predictors;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.WalkPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.WalkVariables;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IncentivizedWalkVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class IncentivizedWalkPredictor extends WalkPredictor {

    private final WalkPredictor walkPredictor;
    private final CostModel costModel;

    @Inject
    public IncentivizedWalkPredictor(WalkPredictor walkPredictor, @Named("incentivized_walk") CostModel costModel) {
        this.walkPredictor = walkPredictor;
        this.costModel = costModel;
    }

    @Override
    public WalkVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        WalkVariables walkVariables = this.walkPredictor.predict(person, trip, elements);
        return new IncentivizedWalkVariables(walkVariables.travelTime_min, this.costModel.calculateCost_MU(person, trip, elements));
    }
}
