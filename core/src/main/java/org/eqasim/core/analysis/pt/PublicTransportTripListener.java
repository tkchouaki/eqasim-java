package org.eqasim.core.analysis.pt;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.eqasim.core.components.transit.events.PublicTransitEvent;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.GenericEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.handler.AgentWaitingForPtEventHandler;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

public class PublicTransportTripListener implements PersonDepartureEventHandler, ActivityStartEventHandler,
		GenericEventHandler, AgentWaitingForPtEventHandler {
	final private Collection<PublicTransportTripItem> trips = new LinkedList<>();
	final private Map<Id<Person>, Integer> tripIndices = new HashMap<>();
	final private Map<Id<Person>, Double> lastDepartureTime = new HashMap<>();
	final private TransitSchedule schedule;

	public PublicTransportTripListener(TransitSchedule schedule) {
		this.schedule = schedule;
	}

	public Collection<PublicTransportTripItem> getTripItems() {
		return trips;
	}

	@Override
	public void reset(int iteration) {
		trips.clear();
		tripIndices.clear();
	}

	public void handleEvent(PublicTransitEvent event) {
		TransitLine transitLine= this.schedule.getTransitLines().get(event.getTransitLineId());
		TransitRoute transitRoute = transitLine.getRoutes().get(event.getTransitRouteId());
		trips.add(new PublicTransportTripItem(event.getPersonId(), //
				tripIndices.get(event.getPersonId()), //
				event.getAccessStopId(), //
				event.getEgressStopId(), //
				event.getTransitLineId(), //
				event.getTransitRouteId(),
				event.getTravelDistance(),
				event.getVehicleDepartureTime(),
				event.getTime() - this.lastDepartureTime.get(event.getPersonId()),transitLine.getName(), transitRoute.getTransportMode()));
	}

	@Override
	public void handleEvent(GenericEvent event) {
		if (event instanceof PublicTransitEvent) {
			handleEvent((PublicTransitEvent) event);
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (event.getActType().contains("interaction")) {
			tripIndices.computeIfPresent(event.getPersonId(), (k, v) -> v - 1);
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (!tripIndices.containsKey(event.getPersonId())) {
			tripIndices.put(event.getPersonId(), 0);
		} else {
			tripIndices.compute(event.getPersonId(), (k, v) -> v + 1);
		}
		this.lastDepartureTime.put(event.getPersonId(), event.getTime());
	}

	@Override
	public void handleEvent(AgentWaitingForPtEvent event) {
		throw new RuntimeException(
				"So far, this analysis tool only works with schedule-based simulation. Your simulation input simulated public transport in the QSim.");
	}
}