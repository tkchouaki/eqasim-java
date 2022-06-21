package org.eqasim.ile_de_france.drt.mode_choice.utilities;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEvent;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEvent;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEventHandler;
import org.matsim.core.controler.MatsimServices;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

public class DrtVariablesDeltaRecorder implements PassengerDroppedOffEventHandler, DrtRequestSubmittedEventHandler, PassengerPickedUpEventHandler, DrtVariablesComputedEventHandlerInterface {

    private final Map<Id<Person>, Double> lastDrtRequestSubmissionTime = new HashMap<>();

    private final Map<Id<Person>, Double> lastPickedUpTime = new HashMap<>();

    private final Map<Id<Person>, Map<Integer, DrtVariables>> cache = new HashMap<>();

    private final Map<Id<Person>, Integer> currentIndices = new HashMap<>();

    private final Map<Id<Person>, Map<Integer, DrtVariables>> observed = new HashMap<>();

    private final MatsimServices matsimServices;

    private boolean cacheFilled = false;

    @Inject
    public DrtVariablesDeltaRecorder(MatsimServices matsimServices) {
        this.matsimServices = matsimServices;
    }

    private void initObservationEntry(Id<Person> personId) {
        if(!this.observed.containsKey(personId)) {
            this.observed.put(personId, new HashMap<>());
        }
    }

    private DrtVariables initNextTripEstimation(Id<Person> personId) {
        if(!this.currentIndices.containsKey(personId)) {
            this.currentIndices.put(personId, -1);
        }
        int i=this.currentIndices.get(personId)+1;
        for(;!this.cache.get(personId).containsKey(i);i++);
        DrtVariables drtVariables = new DrtVariables(-1, -1, -1, -1, -1);
        this.currentIndices.put(personId, i);
        this.observed.get(personId).put(i, drtVariables);
        return drtVariables;
    }

    private DrtVariables getCurrentEstimation(Id<Person> personId) {
        return this.observed.get(personId).get(this.currentIndices.get(personId));
    }

    private void updateCurrentEstimation(Id<Person> personId, DrtVariables drtVariables) {
        this.observed.get(personId).put(this.currentIndices.get(personId), drtVariables);
    }

    @Override
    public void handleEvent(DrtRequestSubmittedEvent drtRequestSubmittedEvent) {
        this.lastDrtRequestSubmissionTime.put(drtRequestSubmittedEvent.getPersonId(), drtRequestSubmittedEvent.getTime());
        if(this.cacheFilled) {
            this.initObservationEntry(drtRequestSubmittedEvent.getPersonId());
            this.initNextTripEstimation(drtRequestSubmittedEvent.getPersonId());
        }
    }

    @Override
    public void handleEvent(PassengerPickedUpEvent passengerPickedUpEvent) {
        if(!this.cacheFilled) {
            return;
        }
        DrtVariables drtVariables = this.getCurrentEstimation(passengerPickedUpEvent.getPersonId());
        double waitingTime = passengerPickedUpEvent.getTime() - this.lastDrtRequestSubmissionTime.get(passengerPickedUpEvent.getPersonId());
        DrtVariables newVariables = new DrtVariables(drtVariables.travelTime_min, drtVariables.cost_MU, drtVariables.euclideanDistance_km, waitingTime, drtVariables.accessEgressTime_min);
        this.updateCurrentEstimation(passengerPickedUpEvent.getPersonId(), newVariables);
        this.lastPickedUpTime.put(passengerPickedUpEvent.getPersonId(), passengerPickedUpEvent.getTime());
    }

    @Override
    public void handleEvent(PassengerDroppedOffEvent passengerDroppedOffEvent) {
        if(!this.cacheFilled) {
            return;
        }
        double travelTime = passengerDroppedOffEvent.getTime() - this.lastPickedUpTime.get(passengerDroppedOffEvent.getPersonId());
        DrtVariables drtVariables = this.getCurrentEstimation(passengerDroppedOffEvent.getPersonId());
        DrtVariables newVariables = new DrtVariables(travelTime, drtVariables.cost_MU, drtVariables.euclideanDistance_km, drtVariables.waitingTime_min, drtVariables.accessEgressTime_min);
        this.updateCurrentEstimation(passengerDroppedOffEvent.getPersonId(), newVariables);
    }


    @Override
    public void reset(int iteration) {
        if(!this.cacheFilled) {
            return;
        }
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.matsimServices.getControlerIO().getIterationFilename(iteration-1, "drtEstimatorDelta.csv"))));
            String delimiter = ";";
            String header = String.join(delimiter, new String[] {
                    "personId",
                    "tripIndex",
                    "observedTravelTime",
                    "observedWaitingTime",
                    "estimatedTravelTime",
                    "estimatedWaitingTime"
            });
            writer.write(header + "\n");
            writer.flush();

            for(Id<Person> personId : this.observed.keySet()) {
                for(Integer tripIndex : this.observed.get(personId).keySet()) {
                    DrtVariables observedVariables = this.observed.get(personId).get(tripIndex);
                    DrtVariables estimatedVariables = this.cache.get(personId).get(tripIndex);
                    String row = String.join(delimiter, new String[] {
                            personId.toString(),
                            tripIndex + "",
                            observedVariables.travelTime_min + "",
                            observedVariables.waitingTime_min + "",
                            estimatedVariables.travelTime_min + "",
                            estimatedVariables.waitingTime_min + ""
                    });
                    writer.write(row + "\n");
                }
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.lastDrtRequestSubmissionTime.clear();
        this.lastPickedUpTime.clear();
        this.observed.clear();
    }


    @Override
    public void handleEvent(DrtVariablesComputedEvent event) {
        if(!this.cache.containsKey(event.getPerson().getId())) {
            this.cache.put(event.getPerson().getId(), new HashMap<>());
        }
        if(this.cache.get(event.getPerson().getId()).containsKey(event.getTrip().getIndex())) {
            // throw new IllegalStateException("Trip index " + event.getTrip().getIndex() + " for person " + event.getPerson().getId().toString() + " evaluated more than once");
        }

        this.cache.get(event.getPerson().getId()).put(event.getTrip().getIndex(), event.getDrtVariables());
        this.cacheFilled = true;
    }
}
