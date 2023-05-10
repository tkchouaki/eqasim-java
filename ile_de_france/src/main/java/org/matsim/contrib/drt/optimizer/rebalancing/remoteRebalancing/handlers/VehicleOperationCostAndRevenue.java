package org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.handlers;

public class VehicleOperationCostAndRevenue {

    public double vehicleKmCost=0;
    public double vehiclePassengerIncome=0;

    public void addKmCost(double cost) {
        this.vehicleKmCost+=cost;
    }

    public void addPassengerIncome(double income) {
        this.vehiclePassengerIncome+=income;
    }

}
