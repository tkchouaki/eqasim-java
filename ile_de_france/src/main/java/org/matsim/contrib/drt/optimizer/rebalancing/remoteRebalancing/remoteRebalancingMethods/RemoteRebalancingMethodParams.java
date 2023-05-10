package org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.remoteRebalancingMethods;

import java.util.Map;

public interface RemoteRebalancingMethodParams {
    public String getRebalancingMethod();
    public Map<String, Object> getRebalancingMethodParams();
}
