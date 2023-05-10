package org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.remoteRebalancingMethods.costBasedQLearning;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.handlers.VehiclesCostAndRevenueCounter;
import org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.remoteRebalancingMethods.RemoteRebalancingRequestBuilder;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;

public class CostBasedQLearningModule extends AbstractDvrpModeModule {

    public CostBasedQLearningModule(String mode) {
        super(mode);
    }

    @Override
    public void install() {
        installQSimModule(new AbstractDvrpModeQSimModule(this.getMode()) {
            @Override
            protected void configureQSim() {
                bindModal(VehiclesCostAndRevenueCounter.class).toProvider(modalProvider(getter ->
                        new VehiclesCostAndRevenueCounter(getter.getModal(Network.class), getter.getModal(Fleet.class))
                )).asEagerSingleton();

                addEventHandlerBinding().to(modalKey(VehiclesCostAndRevenueCounter.class));

                bindModal(RemoteRebalancingRequestBuilder.class).toProvider(modalProvider(instanceGetter -> {
                    Network network = instanceGetter.getModal(Network.class);
                    DrtZonalSystem zonalSystem = instanceGetter.getModal(DrtZonalSystem.class);
                    VehiclesCostAndRevenueCounter costAndRevenueCounter = instanceGetter.getModal(VehiclesCostAndRevenueCounter.class);
                    return new CostBasedQLearningRequestBuilder(network, zonalSystem, costAndRevenueCounter);
                })).asEagerSingleton();
            }
        });

    }
}
