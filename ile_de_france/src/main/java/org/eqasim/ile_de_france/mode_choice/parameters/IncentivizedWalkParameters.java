package org.eqasim.ile_de_france.mode_choice.parameters;

public class IncentivizedWalkParameters extends IDFCostParameters{
    public String originActivityTypes = "any";
    public String destinationActivityTypes = "any";
    public String mainModes = "walk";
    public double incentive_EUR_km = 0;
    public double base_incentive = 0;
    public boolean preventProfit = false;

    public static IncentivizedWalkParameters buildDefault() {
        IDFCostParameters idfCostParameters = IDFCostParameters.buildDefault();

        IncentivizedWalkParameters parameters = new IncentivizedWalkParameters();
        parameters.incentive_EUR_km = 0.1;
        parameters.carCost_EUR_km = idfCostParameters.carCost_EUR_km;
        return parameters;
    }
}
