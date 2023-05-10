package org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.handlers;

import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEvent;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEventHandler;

import java.util.HashMap;
import java.util.Map;

public class PassengersCounter implements PassengerDroppedOffEventHandler {

    private Map<String, Integer> passengersCounts;

    public PassengersCounter() {
        this.passengersCounts = new HashMap<>();
    }

    @Override
    public void handleEvent(PassengerDroppedOffEvent event) {
        String vehicleId = event.getVehicleId().toString();
        this.passengersCounts.put(vehicleId, this.passengersCounts.getOrDefault(vehicleId, 0) + 1);
    }

    @Override
    public void reset(int iteration) {
        this.passengersCounts = new HashMap<>();
    }

    public Integer getPassengersAndReset(String vehicleId) {
        Integer value = this.passengersCounts.getOrDefault(vehicleId, 0);
        this.passengersCounts.put(vehicleId, 0);
        return value;
    }
}
