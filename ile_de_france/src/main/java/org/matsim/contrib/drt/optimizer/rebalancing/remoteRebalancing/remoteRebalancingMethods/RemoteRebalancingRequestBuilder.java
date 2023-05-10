package org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.remoteRebalancingMethods;

import org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.RemoteRebalancingConnectionManager.RemoteRebalancingRequest;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.List;
import java.util.Map;

public abstract class RemoteRebalancingRequestBuilder {
    protected abstract RemoteRebalancingRequest buildRemoteRebalancingRequest(List<DvrpVehicle> vehicles, double time);

    public final RemoteRebalancingRequest getRemoteRebalancingRequest(List<DvrpVehicle> vehicles, double time) {
        return this.buildRemoteRebalancingRequest(vehicles, time);
    }

    public abstract Map<String, Object> getInitializationData();

}
