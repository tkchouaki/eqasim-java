package org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.remoteRebalancingMethods.simpleQLearning;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.handlers.PassengersCounter;
import org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.remoteRebalancingMethods.RemoteRebalancingRequestBuilder;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;


public class SimpleQLearningModule extends AbstractDvrpModeModule {

    public SimpleQLearningModule(String mode) {
        super(mode);
    }

    @Override
    public void install() {
        bindModal(PassengersCounter.class).toInstance(new PassengersCounter());
        addEventHandlerBinding().to(modalKey(PassengersCounter.class));

        bindModal(RemoteRebalancingRequestBuilder.class).toProvider(modalProvider(instanceGetter -> {
            Network network = instanceGetter.getModal(Network.class);
            DrtZonalSystem zonalSystem = instanceGetter.getModal(DrtZonalSystem.class);
            PassengersCounter passengersCounter = instanceGetter.getModal(PassengersCounter.class);
            return new SimpleQLearningRequestBuilder(network, zonalSystem, passengersCounter);
        }));
    }
}
