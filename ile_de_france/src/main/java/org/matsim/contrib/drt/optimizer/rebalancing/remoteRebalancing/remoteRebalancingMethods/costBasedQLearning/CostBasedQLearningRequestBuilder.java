package org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.remoteRebalancingMethods.costBasedQLearning;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.RemoteRebalancingConnectionManager;
import org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.handlers.VehicleOperationCostAndRevenue;
import org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.handlers.VehiclesCostAndRevenueCounter;
import org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.remoteRebalancingMethods.RemoteRebalancingRequestBuilder;
import org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.remoteRebalancingMethods.simpleQLearning.SimpleQLearningRequestBuilder;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.remoteRebalancingMethods.simpleQLearning.SimpleQLearningRequestBuilder.SimpleQLearningIndividualRebalancingRequest.CURRENT_ZONE_ENTRY;

public class CostBasedQLearningRequestBuilder extends RemoteRebalancingRequestBuilder {

    private final Network network;
    private final DrtZonalSystem zonalSystem;
    private final VehiclesCostAndRevenueCounter costAndRevenueCounter;

    public static class CostBasedQLearningIndividualRebalancingRequest extends RemoteRebalancingConnectionManager.IndividualRebalancingRequest{

        public static final String VEHICLE_KM_COST_ENTRY = "vehicleKmCost";
        public static final String VEHICLE_REVENUE_ENTRY = "vehicleRevenue";

        public CostBasedQLearningIndividualRebalancingRequest(String id, String currentZoneId, VehicleOperationCostAndRevenue operationCostAndRevenue) {
            super(id);
            this.put(CURRENT_ZONE_ENTRY, currentZoneId);
            this.put(VEHICLE_KM_COST_ENTRY, operationCostAndRevenue.vehicleKmCost);
            this.put(VEHICLE_REVENUE_ENTRY, operationCostAndRevenue.vehiclePassengerIncome);
        }
    }

    public CostBasedQLearningRequestBuilder(Network network, DrtZonalSystem drtZonalSystem, VehiclesCostAndRevenueCounter costAndRevenueCounter) {
        this.network = network;
        this.zonalSystem = drtZonalSystem;
        this.costAndRevenueCounter = costAndRevenueCounter;
    }

    @Override
    protected RemoteRebalancingConnectionManager.RemoteRebalancingRequest buildRemoteRebalancingRequest(List<DvrpVehicle> vehicles, double time) {
        RemoteRebalancingConnectionManager.RemoteRebalancingRequest remoteRebalancingRequest = new RemoteRebalancingConnectionManager.RemoteRebalancingRequest();

        remoteRebalancingRequest.time = time;
        vehicles.forEach(dvrpVehicle -> {
            //Retrieve vehicle information
            String vehicleId = dvrpVehicle.getId().toString();
            String currentZone = "";
            //A rebalancable vehicle is normally in a StayTask
            Task task = dvrpVehicle.getSchedule().getCurrentTask();
            if (task instanceof StayTask) {
                StayTask stayTask = (StayTask) task;
                currentZone = this.zonalSystem.getZoneForLinkId(stayTask.getLink().getId()).getId();
            }
            VehicleOperationCostAndRevenue costAndRevenue = this.costAndRevenueCounter.getVehicleOperationIncomeAndCost(dvrpVehicle.getId());
            if (costAndRevenue == null) {
                costAndRevenue = new VehicleOperationCostAndRevenue();
                costAndRevenue.vehicleKmCost = 0;
                costAndRevenue.vehiclePassengerIncome = 0;
            }
            remoteRebalancingRequest.vehicles.add(new CostBasedQLearningIndividualRebalancingRequest(vehicleId, currentZone, costAndRevenue));
        });
        return remoteRebalancingRequest;
    }

    @Override
    public Map<String, Object> getInitializationData() {
        Map<String, Object> result = new HashMap<>();
        SimpleQLearningRequestBuilder.putZonesData(result, this.network, this.zonalSystem);
        return result;
    }
}
