package org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing;

import jakarta.validation.constraints.NotNull;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.remoteRebalancingMethods.RemoteRebalancingMethodParams;
import org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.remoteRebalancingMethods.costBasedQLearning.CostBasedQLearningParams;
import org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.remoteRebalancingMethods.simpleQLearning.SimpleQLearningParams;
import org.matsim.contrib.util.ReflectiveConfigGroupWithConfigurableParameterSets;
import org.matsim.core.config.ConfigGroup;

public class RemoteRebalancingParams extends ReflectiveConfigGroupWithConfigurableParameterSets implements RebalancingParams.RebalancingStrategyParams{

    public static final String SET_NAME = "remoteRebalancingStrategyParams";

    public static final String ADDRESS = "address";
    public static final String ADDRESS_EXP = "The address (host + port) of the rebalancing server";

    @NotNull
    private String address = "tcp://localhost:5555";

    @NotNull
    private RemoteRebalancingMethodParams remoteRebalancingMethodParams;

    public RemoteRebalancingParams() {
        super(SET_NAME);
        addDefinition(SimpleQLearningParams.SET_NAME, SimpleQLearningParams::new,
                () -> (ConfigGroup)this.getRemoteRebalancingMethodParams(),
                params -> this.setRemoteRebalancingMethodParams((RemoteRebalancingMethodParams) params));
        addDefinition(CostBasedQLearningParams.SET_NAME, CostBasedQLearningParams::new,
                () -> (ConfigGroup)this.getRemoteRebalancingMethodParams(),
                params -> this.setRemoteRebalancingMethodParams((RemoteRebalancingMethodParams) params));
    }


    /**
     * @return -- {@value #ADDRESS_EXP}
     */
    @StringGetter(ADDRESS)
    public String getAddress() {
        return this.address;
    }

    /**
     * @param address -- {@value #ADDRESS_EXP}
     */
    @StringSetter(ADDRESS)
    public void setAddress(String address) {
        this.address = address;
    }

    public RemoteRebalancingMethodParams getRemoteRebalancingMethodParams() {
        return this.remoteRebalancingMethodParams;
    }

    public void setRemoteRebalancingMethodParams(RemoteRebalancingMethodParams remoteRebalancingMethodParams) {
        this.remoteRebalancingMethodParams = remoteRebalancingMethodParams;
    }
}
