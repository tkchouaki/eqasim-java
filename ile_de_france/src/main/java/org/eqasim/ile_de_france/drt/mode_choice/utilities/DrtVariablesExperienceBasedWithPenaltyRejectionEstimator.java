package org.eqasim.ile_de_france.drt.mode_choice.utilities;

import org.apache.commons.io.FileUtils;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.controler.MatsimServices;
import org.matsim.vehicles.Vehicle;

import java.io.*;
import java.util.List;
import java.util.Map;

public class DrtVariablesExperienceBasedWithPenaltyRejectionEstimator extends DrtVariablesExperienceEstimator implements PassengerRequestRejectedEventHandler, LinkEnterEventHandler {

    private int requestsNumber=0;
    private int rejectionsNumber=0;
    private final MatsimServices matsimServices;
    private final IdMap<Vehicle, Id<Link>> vehiclesPosition = new IdMap<>(Vehicle.class);

    public DrtVariablesExperienceBasedWithPenaltyRejectionEstimator(CostModel costModel, MatsimServices matsimServices) {
        super(costModel);
        this.matsimServices = matsimServices;
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
        File folderPath = new File(this.matsimServices.getControlerIO().getIterationFilename(this.getIteration(), "drt_rejections"));
        File f = new File(folderPath, passengerRequestRejectedEvent.getRequestId().toString()+".csv");
        try {
            FileUtils.forceMkdir(folderPath);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f.getPath())));
            writer.write("x;y;id;kind\n");
            for(Map.Entry<Id<Vehicle>, Id<Link>> entry: this.vehiclesPosition.entrySet()) {
                writer.write(";;"+entry.getKey().toString()+"vehicle");
            }
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        return (double) this.rejectionsNumber / this.requestsNumber;
    }

    public int getRequestsNumber() {
        return this.requestsNumber;
    }

    public int getRejectionsNumber() {
        return this.rejectionsNumber;
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        if(event.getVehicleId().toString().startsWith("vehicle_drt")) {
            this.vehiclesPosition.put(event.getVehicleId(), event.getLinkId());
        }
    }
}
