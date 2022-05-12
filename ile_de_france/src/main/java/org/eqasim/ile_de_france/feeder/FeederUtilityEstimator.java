package org.eqasim.ile_de_france.feeder;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.PtUtilityEstimator;
import org.eqasim.ile_de_france.drt.mode_choice.parameters.IDFDrtModeParameters;
import org.eqasim.ile_de_france.drt.mode_choice.utilities.DrtUtilityEstimator;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.LinkedList;
import java.util.List;

public class FeederUtilityEstimator implements UtilityEstimator {
	private final DrtUtilityEstimator drtEstimator;
	private final PtUtilityEstimator ptEstimator;
	private final IDFDrtModeParameters parameters;
	private static final boolean FORCE_FEEDER = false;

	@Inject
	public FeederUtilityEstimator(DrtUtilityEstimator drtEstimator, PtUtilityEstimator ptEstimator,
								  IDFDrtModeParameters parameters) {
		this.drtEstimator = drtEstimator;
		this.ptEstimator = ptEstimator;
		this.parameters = parameters;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		String lastMode = "";
		List<PlanElement> currentTrip = new LinkedList<>();
		double totalUtility = 0;

		if(FORCE_FEEDER) {
			return 200;
		}

		for (PlanElement element : elements) {
			if (element instanceof Activity && ((Activity) element).getType().equals("feeder interaction")) {
				if (lastMode.equals(TransportMode.pt)) {
					totalUtility += ptEstimator.estimateUtility(person, trip, currentTrip);
				} else if (lastMode.equals(TransportMode.drt)) {
					totalUtility += drtEstimator.estimateUtility(person, trip, currentTrip);
				}
				currentTrip.clear();
			} else {
				currentTrip.add(element);
				if (element instanceof Leg) {
					Leg leg = (Leg) element;
					if (!leg.getMode().equals(TransportMode.walk)) {
						lastMode = leg.getMode();
					}
				}
			}
		}
		return totalUtility;
	}
}
