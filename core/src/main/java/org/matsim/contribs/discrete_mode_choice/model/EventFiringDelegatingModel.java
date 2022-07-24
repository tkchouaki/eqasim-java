package org.matsim.contribs.discrete_mode_choice.model;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.core.api.experimental.events.EventsManager;

import java.util.List;
import java.util.Random;

public class EventFiringDelegatingModel implements DiscreteModeChoiceModel{

    private final EventsManager eventsManager;
    private final DiscreteModeChoiceModel delegate;

    public EventFiringDelegatingModel(EventsManager eventsManager, DiscreteModeChoiceModel delegate) {
        this.eventsManager = eventsManager;
        this.delegate = delegate;
    }

    @Override
    public List<TripCandidate> chooseModes(Person person, List<DiscreteModeChoiceTrip> trips, Random random) throws NoFeasibleChoiceException {
        List<TripCandidate> result = delegate.chooseModes(person, trips, random);
        eventsManager.processEvent(new ModeChoicePerformedEvent(person, trips, result));
        return result;
    }
}
