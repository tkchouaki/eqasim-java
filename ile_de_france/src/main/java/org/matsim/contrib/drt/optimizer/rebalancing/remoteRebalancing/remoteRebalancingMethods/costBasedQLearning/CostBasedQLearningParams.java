package org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.remoteRebalancingMethods.costBasedQLearning;

import jakarta.validation.constraints.NotNull;
import org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.remoteRebalancingMethods.simpleQLearning.SimpleQLearningParams;

import java.util.Map;

public class CostBasedQLearningParams extends SimpleQLearningParams {
    public static final String SET_NAME = "CostBasedQLearning";

    public static final String VEHICLE_KMS_COEFFICIENT = "vehicleKmsCoefficient";
    public static final String VEHICLE_KMS_COEFFICIENT_EXP = "vehicle¨kilometers coefficient in the reward formula";

    public static final String PASSENGERS_REVENUE_COEFFICIENT = "passengersRevenueCoefficient";
    public static final String PASSENGERS_REVENUE_COEFFICIENT_EXP = "The coefficient, in the reward formula, of revenue generated for the drt service by the passengers";

    @NotNull
    private double vehicleKmsCoefficient;
    @NotNull
    private double passengersRevenueCoefficient;

    public CostBasedQLearningParams() {
        super(SET_NAME);
    }

    @StringGetter(ALPHA)
    public float getAlpha(){
        return super.getAlpha();
    }

    @StringSetter(ALPHA)
    public void setAlpha(float alpha) {
        super.setAlpha(alpha);
    }

    @StringGetter(GAMMA)
    public float getGamma() {
        return super.getGamma();
    }

    @StringSetter(GAMMA)
    public void setGamma(float gamma) { super.setGamma(gamma); }

    @StringGetter(EPSILON)
    public float getEpsilon() {
        return super.getEpsilon();
    }

    @StringSetter(EPSILON)
    public void setEpsilon(float epsilon) {
        super.setEpsilon(epsilon);
    }

    @StringGetter(DISCRETE_TIME_INTERVAL_LENGTH)
    public int getDiscreteTimeIntervalLength() {
        return super.getDiscreteTimeIntervalLength();
    }

    @StringSetter(DISCRETE_TIME_INTERVAL_LENGTH)
    public void setDiscreteTimeIntervalLength(int discreteTimeIntervalLength) {
        super.setDiscreteTimeIntervalLength(discreteTimeIntervalLength);
    }

    @StringGetter(VEHICLE_KMS_COEFFICIENT)
    public double getVehicleKmsCoefficient() {
        return this.vehicleKmsCoefficient;
    }

    @StringSetter(VEHICLE_KMS_COEFFICIENT)
    public void setVehicleKmsCoefficient(double vehicleKmsCoefficient) {
        this.vehicleKmsCoefficient =  vehicleKmsCoefficient;
    }

    @StringGetter(PASSENGERS_REVENUE_COEFFICIENT)
    public double getPassengersRevenueCoefficient() {
        return this.passengersRevenueCoefficient;
    }

    @StringSetter(PASSENGERS_REVENUE_COEFFICIENT)
    public void setPassengersRevenueCoefficient(double passengersRevenueCoefficient) {
        this.passengersRevenueCoefficient = passengersRevenueCoefficient;
    }

    @Override
    public String getRebalancingMethod() {
        return SET_NAME;
    }

    @Override
    public Map<String, Object> getRebalancingMethodParams(){
        Map<String, Object> params = super.getRebalancingMethodParams();
        params.put(VEHICLE_KMS_COEFFICIENT, this.vehicleKmsCoefficient);
        params.put(PASSENGERS_REVENUE_COEFFICIENT, this.passengersRevenueCoefficient);
        return params;
    }
}
