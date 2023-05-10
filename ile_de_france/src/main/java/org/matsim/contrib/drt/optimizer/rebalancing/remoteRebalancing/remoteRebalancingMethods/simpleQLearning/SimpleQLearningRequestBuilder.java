package org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.remoteRebalancingMethods.simpleQLearning;


import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZone;
import org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.RemoteRebalancingConnectionManager.IndividualRebalancingRequest;
import org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.RemoteRebalancingConnectionManager.RemoteRebalancingRequest;
import org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.handlers.PassengersCounter;
import org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.remoteRebalancingMethods.RemoteRebalancingRequestBuilder;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.network.NetworkUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleQLearningRequestBuilder extends RemoteRebalancingRequestBuilder {

    private final DrtZonalSystem zonalSystem;
    private final PassengersCounter passengersCounter;
    private final Network network;

    public static class SimpleQLearningIndividualRebalancingRequest extends IndividualRebalancingRequest{

        public static final String CURRENT_ZONE_ENTRY = "currentZoneId";
        public static final String PASSENGERS_NUMBER_ENTRY = "passengersNumber";

        public SimpleQLearningIndividualRebalancingRequest(String id, String currentZoneId, Integer passengersNumber) {
            super(id);
            this.put(CURRENT_ZONE_ENTRY, currentZoneId);
            this.put(PASSENGERS_NUMBER_ENTRY, passengersNumber);
        }
    }

    public SimpleQLearningRequestBuilder(Network network, DrtZonalSystem zonalSystem, PassengersCounter passengersCounter) {
        this.network = network;
        this.zonalSystem = zonalSystem;
        this.passengersCounter = passengersCounter;
    }

    @Override
    protected RemoteRebalancingRequest buildRemoteRebalancingRequest(List<DvrpVehicle> vehicles, double time) {
        RemoteRebalancingRequest remoteRebalancingRequest = new RemoteRebalancingRequest();

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
            Integer passengersNumber = this.passengersCounter.getPassengersAndReset(vehicleId);
            remoteRebalancingRequest.vehicles.add(new SimpleQLearningIndividualRebalancingRequest(vehicleId, currentZone, passengersNumber));
        });
        return remoteRebalancingRequest;
    }

    public static void putZonesData(Map<String, Object> targetMap, Network network, DrtZonalSystem zonalSystem){
        List<Map<String, String>> zonesList = new ArrayList<>();
        targetMap.put("zones", zonesList);
        for (DrtZone zone : zonalSystem.getZones().values()) {
            Map<String, String> zoneEntry = new HashMap<>();
            zoneEntry.put("zoneId", zone.getId());
            zoneEntry.put("centroidId", NetworkUtils.getNearestLink(network, zone.getCentroid()).getId().toString());
            zonesList.add(zoneEntry);
        }
    }

    @Override
    public Map<String, Object> getInitializationData() {
        Map<String, Object> result = new HashMap<>();
        putZonesData(result, this.network, this.zonalSystem);
        return result;
    }
}
