package org.matsim.contrib.drt.fare;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.core.api.internal.HasVehicleId;
import org.matsim.vehicles.Vehicle;

public class DrtFareEvent extends Event implements HasPersonId, HasVehicleId {

    private final Id<Person> personId;
    private final double amount;
    private final String transactionPartner;
    private final Id<DvrpVehicle> vehicleId;

    public DrtFareEvent(double time, Id<Person> agentId, double amount,
                        String transactionPartner, Id<DvrpVehicle> vehicleId) {
        super(time);
        this.personId = agentId;
        this.amount = amount;
        this.transactionPartner = transactionPartner;
        this.vehicleId = vehicleId;
    }

    @Override
    public String getEventType() {
        return "DrtFareEvent";
    }

    @Override
    public Id<Person> getPersonId() {
        return this.personId;
    }

    @Override
    public Id<Vehicle> getVehicleId() {
        return Id.createVehicleId(this.vehicleId);
    }

    public double getAmount(){
        return amount;
    }
}
