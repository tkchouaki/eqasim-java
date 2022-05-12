package org.eqasim.ile_de_france.drt.mode_choice;

import org.eqasim.ile_de_france.mode_choice.IDFModeAvailability;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;

import java.util.Collection;
import java.util.List;

public class IDFDrtModeAvailability implements ModeAvailability {
	static public final String NAME = "ParisDrtModeAvailability";

	private final ModeAvailability delegate = new IDFModeAvailability();
	private final boolean useFeeder;


	public IDFDrtModeAvailability(boolean useFeeder) {
		this.useFeeder = useFeeder;
	}

	@Override
	public Collection<String> getAvailableModes(Person person, List<DiscreteModeChoiceTrip> trips) {
		Collection<String> modes = delegate.getAvailableModes(person, trips);

		if (modes.contains(TransportMode.walk)) {
			modes.add(this.useFeeder ? "feeder" : "drt");
		}

		return modes;
	}
}
