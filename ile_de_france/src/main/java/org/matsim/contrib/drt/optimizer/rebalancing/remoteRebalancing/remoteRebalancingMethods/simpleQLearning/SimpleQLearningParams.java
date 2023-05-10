package org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.remoteRebalancingMethods.simpleQLearning;

import jakarta.validation.constraints.NotNull;
import org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.remoteRebalancingMethods.RemoteRebalancingMethodParams;
import org.matsim.contrib.util.ReflectiveConfigGroupWithConfigurableParameterSets;

import java.util.HashMap;
import java.util.Map;

public class SimpleQLearningParams extends ReflectiveConfigGroupWithConfigurableParameterSets implements RemoteRebalancingMethodParams {

    public static final String SET_NAME = "SimpleQLearning";

    public static final String ALPHA = "alpha";
    public static final String ALPHA_EXP = "The alpha parameter of the QLearning";

    public static final String GAMMA = "gamma";
    public static final String GAMMA_EXP = "The gamma parameter of the QLearning";

    public static final String EPSILON = "epsilon";
    public static final String EPSILON_EXP = "The epsilon parameter of the QLearning";

    public static final String DISCRETE_TIME_INTERVAL_LENGTH = "discreteTimeIntervalLength";
    public static final String DISCRETE_TIME_INTERVAL_LENGTH_EXP = "The length of intervals that time will be discretized into";

    @NotNull
    private float alpha;

    @NotNull
    private float gamma;

    @NotNull
    private float epsilon;

    @NotNull
    private int discreteTimeIntervalLength;

    public SimpleQLearningParams() {
        super(SET_NAME);
    }

    protected SimpleQLearningParams(String setName) {
        super(setName);
    }

    @StringGetter(ALPHA)
    public float getAlpha(){
        return this.alpha;
    }

    @StringSetter(ALPHA)
    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    @StringGetter(GAMMA)
    public float getGamma() {
        return gamma;
    }

    @StringSetter(GAMMA)
    public void setGamma(float gamma) {
        this.gamma = gamma;
    }

    @StringGetter(EPSILON)
    public float getEpsilon() {
        return epsilon;
    }

    @StringSetter(EPSILON)
    public void setEpsilon(float epsilon) {
        this.epsilon = epsilon;
    }

    @StringGetter(DISCRETE_TIME_INTERVAL_LENGTH)
    public int getDiscreteTimeIntervalLength() {
        return this.discreteTimeIntervalLength;
    }

    @StringSetter(DISCRETE_TIME_INTERVAL_LENGTH)
    public void setDiscreteTimeIntervalLength(int discreteTimeIntervalLength) {
        this.discreteTimeIntervalLength = discreteTimeIntervalLength;
    }

    @Override
    public String getRebalancingMethod() {
        return SET_NAME;
    }

    @Override
    public Map<String, Object> getRebalancingMethodParams(){
        Map<String, Object> params = new HashMap<>();
        params.put(ALPHA, this.alpha);
        params.put(GAMMA, this.gamma);
        params.put(EPSILON, this.epsilon);
        params.put(DISCRETE_TIME_INTERVAL_LENGTH, this.discreteTimeIntervalLength);
        return params;
    }
}
