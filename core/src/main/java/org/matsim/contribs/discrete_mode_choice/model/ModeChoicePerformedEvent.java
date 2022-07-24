package org.matsim.contribs.discrete_mode_choice.model;

import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

import java.util.List;

public class ModeChoicePerformedEvent extends Event {
    public static final String EVENT_TYPE="ModeChoicePerformedEvent";

    private final Person person;
    private final List<DiscreteModeChoiceTrip> trips;
    private final List<TripCandidate> modeChoiceResult;

    public ModeChoicePerformedEvent(Person person, List<DiscreteModeChoiceTrip> trips, List<TripCandidate> modeChoiceResult) {
        super(-1);
        this.person = person;
        this.trips = trips;
        this.modeChoiceResult = modeChoiceResult;
    }

    public Person getPerson() {
        return person;
    }

    public List<DiscreteModeChoiceTrip> getTrips() {
        return trips;
    }

    public List<TripCandidate> getModeChoiceResult() {
        return modeChoiceResult;
    }


    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }
}
