package org.eqasim.ile_de_france.mode_choice;

import com.google.inject.Inject;
import org.matsim.contribs.discrete_mode_choice.model.TourSelectorEvent;
import org.matsim.contribs.discrete_mode_choice.model.TourSelectorEventHandler;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SelectedToursRecorder extends MatsimXmlWriter implements TourSelectorEventHandler, IterationStartsListener, BeforeMobsimListener {

    private List<TourSelectorEvent> tourSelectorEvents = new ArrayList<>();
    private final MatsimServices matsimServices;
    private final EventsManager eventsManager;

    @Inject
    public SelectedToursRecorder(MatsimServices matsimServices, EventsManager eventsManager) {
        this.matsimServices = matsimServices;
        this.eventsManager = eventsManager;
    }

    @Override
    public void handleEvent(TourSelectorEvent tourSelectorEvent) {
        this.tourSelectorEvents.add(tourSelectorEvent);
    }


    private void write(String filePath){
        openFile(filePath);
        writeStartTag("TourSelectorEvents", Collections.emptyList());
        this.tourSelectorEvents.forEach(this::writeTourSelectorEvent);
        writeEndTag("TourSelectorEvents");
        close();
    }

    private void writeTourSelectorEvent(TourSelectorEvent tourSelectorEvent) {
        List<Tuple<String, String>> attributes = new ArrayList<>();
        attributes.add(Tuple.of("person", tourSelectorEvent.getPerson().getId().toString()));
        this.writeStartTag(TourSelectorEvent.EVENT_TYPE, attributes);
        this.writeTourCandidate(tourSelectorEvent.getSelected(), true);
        for(TourCandidate tourCandidate: tourSelectorEvent.getCandidates()) {
            if(tourCandidate.equals(tourSelectorEvent.getSelected())){
                continue;
            }
            this.writeTourCandidate(tourCandidate, false);
        }
        this.writeEndTag(TourSelectorEvent.EVENT_TYPE);
    }

    private void writeTourCandidate(TourCandidate tourCandidate, boolean selected) {
        List<Tuple<String, String>> attributes = new ArrayList<>();
        if(selected) {
            attributes.add(Tuple.of("selected", "selected"));
        }
        attributes.add(Tuple.of("utility", ""+tourCandidate.getUtility()));
        writeStartTag("TourCandidate", attributes);
        tourCandidate.getTripCandidates().forEach(this::writeTripCandidate);
        writeEndTag("TourCandidate");
    }

    private void writeTripCandidate(TripCandidate tripCandidate) {
        List<Tuple<String, String>> attributes = new ArrayList<>();
        attributes.add(Tuple.of("mode", tripCandidate.getMode()));
        attributes.add(Tuple.of("utility", ""+tripCandidate.getUtility()));
        writeStartTag("TripCandidate", attributes);
        writeEndTag("TripCandidate");
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        System.out.println("Starting iteration with " + this.tourSelectorEvents.size() + " stored events");
        this.eventsManager.addHandler(this);
    }

    @Override
    public void notifyBeforeMobsim(BeforeMobsimEvent event) {
        this.write(this.matsimServices.getControlerIO().getIterationFilename(event.getIteration(), "tourSelection.xml"));
        this.tourSelectorEvents.clear();
        this.eventsManager.removeHandler(this);
    }
}
