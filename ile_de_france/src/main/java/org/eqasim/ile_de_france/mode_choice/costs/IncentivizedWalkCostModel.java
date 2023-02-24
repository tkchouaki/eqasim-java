package org.eqasim.ile_de_france.mode_choice.costs;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.ile_de_france.mode_choice.parameters.IncentivizedWalkParameters;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.router.MainModeIdentifier;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IncentivizedWalkCostModel extends AbstractCostModel {

    private final IncentivizedWalkParameters incentivizedWalkParameters;
    private final Set<String> originActivityTypes = new HashSet<>();
    private final Set<String> destinationActivityTypes = new HashSet<>();
    private final Set<String> mainModes = new HashSet<>();
    private final MainModeIdentifier mainModeIdentifier;

    @Inject
    public IncentivizedWalkCostModel(IncentivizedWalkParameters incentivizedWalkParameters, MainModeIdentifier mainModeIdentifier) {
        super("walk");
        this.incentivizedWalkParameters = incentivizedWalkParameters;
        this.mainModeIdentifier = mainModeIdentifier;
        if(!this.incentivizedWalkParameters.originActivityTypes.equals("any")) {
            this.originActivityTypes.addAll(List.of(this.incentivizedWalkParameters.destinationActivityTypes.split(",")));
        }
        if(!this.incentivizedWalkParameters.destinationActivityTypes.equals("any")) {
            this.destinationActivityTypes.addAll(List.of(this.incentivizedWalkParameters.destinationActivityTypes.split(",")));
        }
        if(!this.incentivizedWalkParameters.mainModes.equals("any")) {
            this.mainModes.addAll(List.of(this.incentivizedWalkParameters.mainModes.split(",")));
        }
    }

    @Override
    public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        if(this.originActivityTypes.size() > 0 && !this.originActivityTypes.contains(trip.getOriginActivity().getType())) {
            return 0;
        }
        if(this.destinationActivityTypes.size() > 0 && !this.destinationActivityTypes.contains(trip.getDestinationActivity().getType())) {
            return 0;
        }
        String mainMode = this.mainModeIdentifier.identifyMainMode(elements);
        if(this.mainModes.size() > 0) {
            if(!this.mainModes.contains(mainMode)) {
                return 0;
            }
        }
        double walkingDistance = this.getInVehicleDistance_km(elements);
        return - this.incentivizedWalkParameters.base_incentive - walkingDistance * incentivizedWalkParameters.incentive_EUR_km;
    }
}
