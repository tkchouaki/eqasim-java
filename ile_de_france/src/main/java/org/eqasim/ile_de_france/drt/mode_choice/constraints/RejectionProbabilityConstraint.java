package org.eqasim.ile_de_france.drt.mode_choice.constraints;

import com.google.inject.Inject;
import org.eqasim.ile_de_france.drt.mode_choice.utilities.DrtVariablesExperienceBasedWithPenaltyRejectionEstimator;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.constraints.AbstractTripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;

import java.util.Collection;
import java.util.List;
import java.util.Random;

public class RejectionProbabilityConstraint extends AbstractTripConstraint {
    public static final String NAME = "DrtRejectionProbability";

    private DrtVariablesExperienceBasedWithPenaltyRejectionEstimator drtVariablesExperienceBasedWithPenaltyRejectionEstimator;
    private Random random;

    /* TODO
        - Objective is to have a the same decision of each agent if the rejection probability remains the same
        - At the beginning of the simulation, generate a random number for each agent that will be used for the choice
     */

    @Inject
    public RejectionProbabilityConstraint(Random random, DrtVariablesExperienceBasedWithPenaltyRejectionEstimator drtVariablesExperienceBasedWithPenaltyRejectionEstimator) {
        this.drtVariablesExperienceBasedWithPenaltyRejectionEstimator = drtVariablesExperienceBasedWithPenaltyRejectionEstimator;
        this.random = random;
    }

    public boolean validateBeforeEstimation(DiscreteModeChoiceTrip trip, String mode, List<String> previousModes) {
        return this.random.nextDouble() >= this.drtVariablesExperienceBasedWithPenaltyRejectionEstimator.getRejectionProbability();
    }



    static public class Factory implements TripConstraintFactory {

        private RejectionProbabilityConstraint rejectionProbabilityConstraint;


        @Override
        public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> planTrips, Collection<String> availableModes) {
            return null;
        }
    }
}
