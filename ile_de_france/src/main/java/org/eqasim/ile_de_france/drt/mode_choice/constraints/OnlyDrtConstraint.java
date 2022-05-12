package org.eqasim.ile_de_france.drt.mode_choice.constraints;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.constraints.AbstractTripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.RoutedTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.core.router.TripStructureUtils;

import java.util.Collection;
import java.util.List;

public class OnlyDrtConstraint extends AbstractTripConstraint {
    public static final String NAME = "OnlyDrtConstraint";

    @Override
    public boolean validateAfterEstimation(DiscreteModeChoiceTrip trip, TripCandidate candidate,
                                           List<TripCandidate> previousCandidates) {
        if (candidate.getMode().equals("drt")) {
            RoutedTripCandidate routedCandidate = (RoutedTripCandidate) candidate;

            for (Leg leg : TripStructureUtils.getLegs(routedCandidate.getRoutedPlanElements())) {
                if (leg.getMode().equals("drt")) {
                    return true;
                }
            }

            return false;
        }

        return true;
    }

    public static class Factory implements TripConstraintFactory {
        @Override
        public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> planTrips,
                                               Collection<String> availableModes) {
            return new OnlyDrtConstraint();
        }
    }
}
