package org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZone;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.remoteRebalancingMethods.RemoteRebalancingRequestBuilder;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.ReplanningContext;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;


public class RemoteRebalancingStrategy implements RebalancingStrategy {

    private boolean startedIteration;

    private final RemoteRebalancingConnectionManager connectionManager;
    private final RemoteRebalancingRequestBuilder requestBuilder;
    private final Network network;
    private final DrtZonalSystem drtZonalSystem;
    private final OutputDirectoryHierarchy outputDirectoryHierarchy;
    private final ReplanningContext replanningContext;

    public RemoteRebalancingStrategy(RemoteRebalancingConnectionManager connectionManager, RemoteRebalancingRequestBuilder requestBuilder, Network network) {
        this(connectionManager, requestBuilder, network, null, null, null);
    }

    public RemoteRebalancingStrategy(RemoteRebalancingConnectionManager connectionManager, RemoteRebalancingRequestBuilder requestBuilder, Network network, DrtZonalSystem zonalSystem, OutputDirectoryHierarchy outputDirectoryHierarchy, ReplanningContext replanningContext) {
        this.connectionManager = connectionManager;
        this.requestBuilder = requestBuilder;
        this.startedIteration = false;
        this.network = network;
        this.drtZonalSystem = null;
        this.outputDirectoryHierarchy = outputDirectoryHierarchy;
        this.replanningContext = replanningContext;
    }

    private void sendChargers() {
        /*
        if (this.chargingInfrastructure == null) {
            return;
        }
        RemoteRebalancingConnectionManager.ChargersInformation chargersInformation = new RemoteRebalancingConnectionManager.ChargersInformation();
        for (Map.Entry<Id<Charger>, Charger> entry : this.chargingInfrastructure.getChargers().entrySet()) {
            chargersInformation.chargersInformation.add(new RemoteRebalancingConnectionManager.SingleChargerInformation(entry.getKey().toString(), entry.getValue().getLink().getId().toString()));
        }
        this.connectionManager.sendChargers(chargersInformation);*/
    }

    public void reset(int iteration) {

    }

    @Override
    public List<Relocation> calcRelocations(Stream<? extends DvrpVehicle> rebalancableVehicles, double time) {

        this.connectionManager.sendFirstConnection(this.requestBuilder);

        Map<String, DvrpVehicle> stringsToVehicles = new HashMap<>();
        List<DvrpVehicle> vehicles = new ArrayList<>();
        rebalancableVehicles.forEach(dvrpVehicle -> {
            vehicles.add(dvrpVehicle);
            stringsToVehicles.put(dvrpVehicle.getId().toString(), dvrpVehicle);
        });

        RemoteRebalancingConnectionManager.RemoteRebalancingAnswer relocationsData = connectionManager.sendRebalancingRequest(requestBuilder.getRemoteRebalancingRequest(vehicles, time));

        List<Relocation> result = new ArrayList<>();
        relocationsData.relocations.forEach(relocationData -> {
            result.add(new Relocation(stringsToVehicles.get(relocationData.vehicleId), network.getLinks().get(Id.createLinkId(relocationData.linkId))));
        });

        /*
        if (drtZonalSystem != null) {
            Map<String, Integer> numberOfVehiclesPerZone = new HashMap<>();
            for (RemoteRebalancingConnectionManager.RemoteRebalancingRelocation relocation : relocationsData.relocations) {
                DrtZone drtZone = this.drtZonalSystem.getZoneForLinkId(Id.createLinkId(relocation.linkId));
                numberOfVehiclesPerZone.put(drtZone.getId(), numberOfVehiclesPerZone.getOrDefault(drtZone.getId(), 0)+1);
            }
            String path = this.outputDirectoryHierarchy.getIterationFilename(replanningContext.getIteration(), String.format("drt_rebalancing_%f.csv", time));
            try {
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path)));
                writer.write("zoneId;nVehicles\n");
                for(String zoneId: numberOfVehiclesPerZone.keySet()) {
                    writer.write(String.format("%s;%d\n", zoneId, numberOfVehiclesPerZone.get(zoneId)));
                }
                writer.close();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        */
        return result;
        /*RemoteRebalancingConnectionManager.RemoteRebalancingRequest remoteRebalancingRequest = new RemoteRebalancingConnectionManager.RemoteRebalancingRequest();

        //The reblancableVehicles object is a Steam that i can loop over only once
        //We need to store the ID -> Vehicle correspondence to retrieve the vehicles needed for the Relocation objects
        Map<String, DvrpVehicle> stringsToVehicles = new HashMap<>();

        remoteRebalancingRequest.time = time;
        rebalancableVehicles.forEach(dvrpVehicle -> {
            //Retrieve vehicle information
            String vehicleId = dvrpVehicle.getId().toString();
            String startingLinkId = dvrpVehicle.getStartLink().getId().toString();
            String currentZone = "";
            //A rebalancable vehicle is normally in a StayTask
            Task task = dvrpVehicle.getSchedule().getCurrentTask();
            if (task instanceof StayTask) {
                StayTask stayTask = (StayTask) task;
                currentZone = this.zonalSystem.getZoneForLinkId(stayTask.getLink().getId()).getId();<w<w
            }
            //If the vehicle does not have a battery, we consider its SoC to be 1
            //Be careful, this implementation does not allow to detect a missing ev declaration for an algorithm that actually considers electric vehicles
            double soc = 1;
            if (dvrpVehicle instanceof EvDvrpVehicle) {
                ElectricVehicle ev = ((EvDvrpVehicle) dvrpVehicle).getElectricVehicle();
                soc = ev.getBattery().getSoc() / ev.getBattery().getCapacity();
            }

            Integer passengersNumber = this.passengersCounter.getPassengersAndReset(vehicleId);
            VehicleOperationCostAndRevenue costAndIncome = this.vehiclesCostAndRevenueCounter.getVehicleOperationIncomeAndCost(dvrpVehicle.getId());
            if (costAndIncome == null) {
                costAndIncome = new VehicleOperationCostAndRevenue();
                costAndIncome.vehicleKmCost = 0;
                costAndIncome.vehiclePassengerIncome = 0;
            }

            remoteRebalancingRequest.vehicles.add(new RemoteRebalancingConnectionManager.IndividualRebalancingRequest(vehicleId, startingLinkId, currentZone, passengersNumber, costAndIncome, soc));
            //We mark the ID -> Vehicle correspondence to retrieve the DvrpVehicle object later
            stringsToVehicles.put(dvrpVehicle.getId().toString(), dvrpVehicle);
        });

        */
    }
}
