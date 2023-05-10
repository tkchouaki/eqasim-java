package org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.handlers;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEvent;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEventHandler;

import java.util.HashMap;
import java.util.Map;

public class PassengersWaitTimesPerVehicleCounter implements DrtRequestSubmittedEventHandler, PassengerPickedUpEventHandler {

    private final String mode;
    private final Map<Id<DvrpVehicle>, Double> waitTimes;
    private final Map<Id<Request>, DrtRequestSubmittedEvent> requestSubmittedEvents;

    public PassengersWaitTimesPerVehicleCounter(String mode) {
        this.mode = mode;
        this.waitTimes = new HashMap<>();
        this.requestSubmittedEvents = new HashMap<>();
    }

    @Override
    public void handleEvent(DrtRequestSubmittedEvent event) {
        if(!event.getMode().equals(this.mode)) {
            return;
        }
        this.requestSubmittedEvents.put(event.getRequestId(), event);
    }

    @Override
    public void handleEvent(PassengerPickedUpEvent event) {
        if(!event.getMode().equals(this.mode)) {
            return;
        }
        if(this.requestSubmittedEvents.containsKey(event.getRequestId())) {
            Double requestWaitTime = event.getTime() - this.requestSubmittedEvents.get(event.getRequestId()).getTime();
            this.waitTimes.put(event.getVehicleId(), this.waitTimes.getOrDefault(event.getVehicleId(), 0.0) + requestWaitTime);
            this.requestSubmittedEvents.remove(event.getRequestId());
        }
    }

    @Override
    public void reset(int iteration) {
        this.waitTimes.clear();
        this.requestSubmittedEvents.clear();
    }

    public double getVehicleTotalWaitTimeAndReset(Id<DvrpVehicle> vehicleId) {
        double waitTime = this.waitTimes.getOrDefault(vehicleId, 0.0);
        this.waitTimes.remove(vehicleId);
        return waitTime;
    }
}
