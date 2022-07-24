package org.eqasim.ile_de_france.drt.mode_choice.utilities;

import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class DrtVariablesExperienceBasedWithPenaltyRejectionEstimator extends DrtVariablesExperienceEstimator implements PassengerRequestRejectedEventHandler {

    private double requestsNumber=0;
    private double rejectionsNumber=0;

    public DrtVariablesExperienceBasedWithPenaltyRejectionEstimator(CostModel costModel) {
        super(costModel);
    }

    @Override
    public DrtVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements){
        if(requestsNumber == 0) {
            return this.delegate(person, trip, elements);
        }
        double rejectionProbability = this.getRejectionProbability();
        double waitingTimeExpectation = this.getExpectedWaitingTime();
        double travelTimeExpectation = this.getExpectedTravelTime();
        double cost_MU = this.getCostModel().calculateCost_MU(person, trip, elements);
        double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);
        DrtVariables result = new DrtVariables(travelTimeExpectation * (1 + rejectionProbability) , cost_MU, euclideanDistance_km, waitingTimeExpectation * (1 + rejectionProbability), 0);
        return result;
    }


    @Override
    public void handleEvent(PassengerRequestRejectedEvent passengerRequestRejectedEvent) {
        this.requestsNumber++;
        this.rejectionsNumber++;
    }

    public void reset(int iteration) {
        super.reset(iteration);
        this.requestsNumber = 0;
        this.rejectionsNumber = 0;
    }

    public void handleEvent(PassengerDroppedOffEvent passengerDroppedOffEvent) {
        this.requestsNumber++;
        super.handleEvent(passengerDroppedOffEvent);
    }

    public double getRejectionProbability() {
        return this.rejectionsNumber / this.requestsNumber;
    }
}
