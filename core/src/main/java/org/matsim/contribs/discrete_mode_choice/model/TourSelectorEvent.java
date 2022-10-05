package org.matsim.contribs.discrete_mode_choice.model;

import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourCandidate;

import java.util.ArrayList;
import java.util.List;

public class TourSelectorEvent extends Event {
    public final static String EVENT_TYPE = "TourSelector";
    private final List<TourCandidate> candidates;
    private final TourCandidate selected;
    private final Person person;
    private final List<List<String>> tourModesExcludedBeforeEstimation;
    private final List<TourCandidate> candidatesExcludedAfterEstimation;

    public TourSelectorEvent(double time, Person person, List<TourCandidate> candidates, TourCandidate selected, List<List<String>> tourModesExcludedBeforeEstimation, List<TourCandidate> candidatesExcludedAfterEstimation) {
        super(time);
        this.candidates = candidates;
        this.selected = selected;
        this.person = person;
        this.tourModesExcludedBeforeEstimation = tourModesExcludedBeforeEstimation;
        this.candidatesExcludedAfterEstimation = candidatesExcludedAfterEstimation;
    }

    public TourCandidate getSelected() {
        return this.selected;
    }

    public List<TourCandidate> getCandidates() {
        return this.candidates;
    }

    public List<List<String>> getTourModesExcludedBeforeEstimation() {
        return this.tourModesExcludedBeforeEstimation;
    }

    public List<TourCandidate> getCandidatesExcludedAfterEstimation() {
        return this.candidatesExcludedAfterEstimation;
    }

    public Person getPerson() {
        return this.person;
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }

}
