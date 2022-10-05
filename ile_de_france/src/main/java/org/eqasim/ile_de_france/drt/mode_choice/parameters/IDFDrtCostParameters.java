package org.eqasim.ile_de_france.drt.mode_choice.parameters;

import org.eqasim.ile_de_france.mode_choice.parameters.IDFCostParameters;

public class IDFDrtCostParameters extends IDFCostParameters {
	public double drtCost_EUR_km;

	public static IDFDrtCostParameters buildDefault() {
		// Copy & paste

		IDFDrtCostParameters parameters = new IDFDrtCostParameters();

		parameters.carCost_EUR_km = 0.15;
		parameters.drtCost_EUR_km = 0.3;

		return parameters;
	}
}
