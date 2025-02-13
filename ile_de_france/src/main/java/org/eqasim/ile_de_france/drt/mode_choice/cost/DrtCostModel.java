package org.eqasim.ile_de_france.drt.mode_choice.cost;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.ile_de_france.drt.mode_choice.parameters.IDFDrtCostParameters;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class DrtCostModel extends AbstractCostModel {
	private final IDFDrtCostParameters parameters;

	public DrtCostModel(IDFDrtCostParameters parameters) {
		super("drt");
		this.parameters = parameters;
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		double tripDistance_km = getInVehicleDistance_km(elements);
		return parameters.drtCost_EUR_km * tripDistance_km;
	}
}
