package org.eqasim.ile_de_france.drt.mode_choice.utilities;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.contrib.dvrp.passenger.*;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DrtVariablesExperienceEstimator implements DrtPredictorInterface, PassengerRequestRejectedEventHandler, PassengerDroppedOffEventHandler, DrtRequestSubmittedEventHandler, PassengerPickedUpEventHandler {
    private double requestsNumber=0;
    private double rejectionsNumber=0;
    private double droppedOffNumber=0;
    private double totalWaitingTime=0;
    private double totalTravelTime=0;
    private Map<Id<Person>, Double> lastDrtRequestSubmissionTime = new HashMap<>();
    private Map<Id<Person>, Double> lastPickedUpTime = new HashMap<>();
    private CostModel costModel;
    private DrtPredictor drtPredictor;

    @Inject
    public DrtVariablesExperienceEstimator(@Named("drt") CostModel costModel) {
        this.costModel = costModel;
        this.drtPredictor = new DrtPredictor(this.costModel);
    }

    @Override
    public void reset(int iteration){
        this.requestsNumber = 0;
        this.rejectionsNumber = 0;
        this.droppedOffNumber = 0;
        this.totalTravelTime = 0;
        this.totalWaitingTime = 0;
        this.lastPickedUpTime.clear();;
        this.lastDrtRequestSubmissionTime.clear();
    }

    @Override
    public DrtVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {


        // Define default values (for first iteration) of if no observation
        // 1-Average on other zones & time bins
        // 2-Use the old predictor as a fallback
        // Remain as simple as possible - Start with fixed values & without distinguishing per zones

        if(requestsNumber == 0) {
            return this.drtPredictor.predict(person, trip, elements);
        }
        double rejectionProbability = rejectionsNumber/requestsNumber;
        double waitingTimeExpectation = totalWaitingTime / droppedOffNumber;
        double travelTimeExpectation = totalTravelTime / droppedOffNumber; // For this one, build estimations in a zone matrix
        double cost_MU = costModel.calculateCost_MU(person, trip, elements);
        double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);
        return new DrtVariables(travelTimeExpectation / rejectionProbability, cost_MU, euclideanDistance_km, waitingTimeExpectation / rejectionProbability, 0);
    }

    @Override
    public void handleEvent(PassengerRequestRejectedEvent passengerRequestRejectedEvent) {
        this.requestsNumber++;
        this.rejectionsNumber++;
    }

    @Override
    public void handleEvent(PassengerDroppedOffEvent passengerDroppedOffEvent) {
        this.requestsNumber++;
        this.droppedOffNumber++;
        this.totalTravelTime += this.lastPickedUpTime.get(passengerDroppedOffEvent.getPersonId());
    }

    @Override
    public void handleEvent(DrtRequestSubmittedEvent drtRequestSubmittedEvent) {
        this.lastDrtRequestSubmissionTime.put(drtRequestSubmittedEvent.getPersonId(), drtRequestSubmittedEvent.getTime());
    }

    @Override
    public void handleEvent(PassengerPickedUpEvent passengerPickedUpEvent) {
        this.totalWaitingTime += passengerPickedUpEvent.getTime() - this.lastDrtRequestSubmissionTime.get(passengerPickedUpEvent.getPersonId());
        this.lastPickedUpTime.put(passengerPickedUpEvent.getPersonId(), passengerPickedUpEvent.getTime());
    }
}
