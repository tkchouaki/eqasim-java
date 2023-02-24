package org.eqasim.ile_de_france.mode_choice.costs;

import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.ile_de_france.mode_choice.parameters.IncentivizedWalkParameters;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class IncentivizedWalkCostModelAdapter implements CostModel {

    private final IncentivizedWalkCostModel incentivizedWalkCostModel;
    private final CostModel delegate;

    private final IncentivizedWalkParameters incentivizedWalkParameters;

    public IncentivizedWalkCostModelAdapter(IncentivizedWalkCostModel incentivizedWalkCostModel, CostModel delegate, IncentivizedWalkParameters incentivizedWalkParameters) {
        this.incentivizedWalkCostModel = incentivizedWalkCostModel;
        this.delegate = delegate;
        this.incentivizedWalkParameters = incentivizedWalkParameters;
    }

    @Override
    public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        double baseCost = this.delegate.calculateCost_MU(person, trip, elements);
        double incentive = this.incentivizedWalkCostModel.calculateCost_MU(person, trip, elements);
        double total = baseCost + incentive;
        if(this.incentivizedWalkParameters.preventProfit && total <= 0) {
            total = 0;
        }
        return total;
    }
}
