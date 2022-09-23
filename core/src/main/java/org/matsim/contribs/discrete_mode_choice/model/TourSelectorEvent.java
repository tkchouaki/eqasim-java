package org.matsim.contribs.discrete_mode_choice.model;

import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourCandidate;

import java.util.List;

public class TourSelectorEvent extends Event {
    public final static String EVENT_TYPE = "TourSelector";
    private final List<TourCandidate> candidates;
    private final TourCandidate selected;
    private final Person person;

    public TourSelectorEvent(double time, Person person, List<TourCandidate> candidates, TourCandidate selected) {
        super(time);
        this.candidates = candidates;
        this.selected = selected;
        this.person = person;
    }

    public TourCandidate getSelected() {
        return this.selected;
    }

    public List<TourCandidate> getCandidates() {
        return this.candidates;
    }

    public Person getPerson() {
        return this.person;
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }

}
