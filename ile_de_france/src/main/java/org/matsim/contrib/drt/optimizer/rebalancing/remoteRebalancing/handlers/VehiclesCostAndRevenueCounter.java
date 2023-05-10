package org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.handlers;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.fare.DrtFareEvent;
import org.matsim.contrib.drt.fare.DrtFareEventHandler;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.vehicles.Vehicle;

public class VehiclesCostAndRevenueCounter implements LinkLeaveEventHandler, DrtFareEventHandler {

    private final Network network;
    private final Fleet fleet;
    private IdMap<Vehicle, VehicleOperationCostAndRevenue> counts = new IdMap<>(Vehicle.class);

    public VehiclesCostAndRevenueCounter(Network network, Fleet fleet) {
        this.network = network;
        this.fleet = fleet;
    }

    public VehicleOperationCostAndRevenue getVehicleOperationIncomeAndCost(Id<DvrpVehicle> vehicleId) {
        Id<Vehicle> id = Id.create(vehicleId, Vehicle.class);
        return this.counts.getOrDefault(id, null);
    }


    @Override
    public void handleEvent(LinkLeaveEvent event) {
        Link link = this.network.getLinks().get(event.getVehicleId());
        if(!this.fleet.getVehicles().containsKey(Id.create(event.getVehicleId(), DvrpVehicle.class))){
            return;
        }
        if(!this.counts.containsKey(event.getVehicleId())) {
            this.counts.put(event.getVehicleId(), new VehicleOperationCostAndRevenue());
        }
        this.counts.get(event.getVehicleId()).addKmCost(link.getLength());
    }


    /**
     * Clears the cost and revenue counts at the end of an iteration
     * Useful in case this listener is used in the global MATSim scope and not in the QSim scope
     * @param iteration
     */
    @Override
    public void reset(int iteration){
        this.counts.clear();
    }

    @Override
    public void handleEvent(DrtFareEvent event) {
        Id<Vehicle> vehicleId = event.getVehicleId();
        if(!this.fleet.getVehicles().containsKey(Id.create(vehicleId, DvrpVehicle.class))){
            return;
        }
        if(!this.counts.containsKey(vehicleId)) {
            this.counts.put(vehicleId, new VehicleOperationCostAndRevenue());
        }
        this.counts.get(vehicleId).addPassengerIncome(event.getAmount());
    }
}
