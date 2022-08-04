package org.eqasim.ile_de_france.feeder.analysis.passengers;

import org.eqasim.core.components.transit.events.PublicTransitEvent;
import org.eqasim.core.components.transit.events.PublicTransitEventMapper;
import org.eqasim.ile_de_france.drt.analysis.utils.VehicleRegistry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.List;

public class FeederTripSequenceListener implements PersonDepartureEventHandler, PersonArrivalEventHandler,
        PersonEntersVehicleEventHandler, ActivityEndEventHandler, GenericEventHandler, ActivityStartEventHandler {

    public static final String PERSON_ID_TO_DEBUG = "5282332";
    public static final int ITERATION_TO_DEBUG = 61;

    private static int currentIteration = -1;

    private final PublicTransitEventMapper publicTransitEventMapper = new PublicTransitEventMapper();
    private final VehicleRegistry vehicleRegistry;
    private final Network network;
    private final IdMap<Person, FeederTripSequenceItem> currentItems = new IdMap<>(Person.class);
    private final IdMap<Person, ActivityEndEvent> lastNonInteractionActivity = new IdMap<>(Person.class);
    private final IdMap<Person, List<String>> interactionActivitiesSequences = new IdMap<>(Person.class);
    private final IdMap<Person, Id<Vehicle>> lastPersonVehicles = new IdMap<>(Person.class);
    private final String drtMode = "drt";
    private final String ptMode ="pt";
    private final List<FeederTripSequenceItem> itemsList = new ArrayList<>();



    public FeederTripSequenceListener(VehicleRegistry vehicleRegistry, Network network) {
        this.vehicleRegistry = vehicleRegistry;
        this.network = network;
    }

    @Override
    public void handleEvent(ActivityEndEvent event) {
        List<String> personInteractionActivitiesSequence = interactionActivitiesSequences.get(event.getPersonId());
        if(personInteractionActivitiesSequence == null) {
            personInteractionActivitiesSequence = new ArrayList<>();
            interactionActivitiesSequences.put(event.getPersonId(), personInteractionActivitiesSequence);
        }
        if(event.getActType().endsWith(" interaction")) {
            personInteractionActivitiesSequence.add(event.getActType());
        } else {
            personInteractionActivitiesSequence.clear();
            lastNonInteractionActivity.put(event.getPersonId(), event);
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        FeederTripSequenceItem currentPersonItem = currentItems.get(event.getPersonId());
        if(event.getLegMode().equals(drtMode)) {
            if(currentPersonItem != null) {
                if(!interactionActivitiesSequences.get(event.getPersonId()).contains("feeder interaction")) {
                    throw new IllegalStateException("Found a drt trip for a person with an existing trip before but no feeder interaction activity");
                }
                if(currentPersonItem.egressTransitLineId == null) {
                    //throw new IllegalStateException("Found a drt trip for person " + event.getPersonId().toString() + "with an existing trip but without pt info. This means that two drt sub-trips appear one after the other");
                }
                // So this drt trip will be considered to be the egress one
                currentPersonItem.egressDepartureTime = event.getTime();
            } else {
                currentPersonItem = new FeederTripSequenceItem();
                currentPersonItem.personId = event.getPersonId();
                currentPersonItem.accessDepartureTime = event.getTime();
                currentPersonItem.originLink = network.getLinks().get(this.lastNonInteractionActivity.get(event.getPersonId()).getLinkId());
                currentItems.put(event.getPersonId(), currentPersonItem);

            }
        } else if (event.getLegMode().equals(ptMode)) {
            if(interactionActivitiesSequences.get(event.getPersonId()).contains("feeder interaction") && currentPersonItem == null) {
                throw new IllegalStateException("Found a pt trip following a feeder interaction activity but without a drt trip before");
            }
            if(currentPersonItem == null) {
                currentPersonItem = new FeederTripSequenceItem();
                currentPersonItem.personId = event.getPersonId();
                currentPersonItem.originLink = network.getLinks().get(lastNonInteractionActivity.get(event.getPersonId()).getLinkId());
                currentPersonItem.originLink = network.getLinks().get(this.lastNonInteractionActivity.get(event.getPersonId()).getLinkId());
                currentItems.put(event.getPersonId(), currentPersonItem);
            }
            currentPersonItem.ptDepartureTime = event.getTime();
        }
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        FeederTripSequenceItem currentPersonItem = currentItems.get(event.getPersonId());
        if(currentPersonItem == null && event.getLegMode().equals(drtMode)) {
            throw new IllegalStateException("Drt trip ending detected but no item initialized for the person");
        }
        if(currentPersonItem == null) {
            return;
        }
        if(event.getLegMode().equals(ptMode)) {
            currentPersonItem.ptArrivalTime = event.getTime();
        } else if(event.getLegMode().equals(drtMode)) {
            Id<Vehicle> lastPersonVehicle = this.lastPersonVehicles.get(event.getPersonId());
            if(lastPersonVehicle == null) {
                throw new IllegalStateException("A DRT trip ended and no drt vehicle recorded for the person");
            }
            if(Double.isNaN(currentPersonItem.accessDepartureTime) && Double.isNaN(currentPersonItem.egressDepartureTime)) {
                throw new IllegalStateException("Drt trip end detected while no access or egress departure time is set");
            }
            if(!Double.isNaN(currentPersonItem.egressDepartureTime)) {
                currentPersonItem.egressArrivalTime = event.getTime();
                currentPersonItem.egressVehicleId = lastPersonVehicle;
            }
            else if(!Double.isNaN(currentPersonItem.accessDepartureTime)) {
                currentPersonItem.accessArrivalTime = event.getTime();
                currentPersonItem.accessVehicleId = lastPersonVehicle;
            }
        }
    }


    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        if(vehicleRegistry.isFleet(event.getVehicleId())) {
            this.lastPersonVehicles.put(event.getPersonId(), event.getVehicleId());
        }
    }

    @Override
    public void handleEvent(GenericEvent event) {
        if(!event.getEventType().equals(PublicTransitEvent.TYPE)) {
            return;
        }
        PublicTransitEvent transitEvent = this.publicTransitEventMapper.apply(event);
        FeederTripSequenceItem personItem = this.currentItems.get(transitEvent.getPersonId());
        if(personItem != null) {
            if(personItem.accessTransitStopId == null) {
                personItem.accessTransitStopId = transitEvent.getAccessStopId();
                personItem.accessTransitRouteId = transitEvent.getTransitRouteId();
                personItem.accessTransitLineId = transitEvent.getTransitLineId();
            }
            personItem.egressTransitStopId = transitEvent.getEgressStopId();
            personItem.egressTransitRouteId = transitEvent.getTransitRouteId();
            personItem.egressTransitLineId = transitEvent.getTransitLineId();
        }

    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        if(event.getActType().endsWith(" interaction")) {
            return;
        }
        Id<Person> personId = event.getPersonId();
        FeederTripSequenceItem item = currentItems.remove(personId);
        if(interactionActivitiesSequences.containsKey(personId) && !interactionActivitiesSequences.get(personId).contains("feeder interaction")) {
            return;
        }
        if(item == null) {
            return;
        }
        if (Double.isNaN(item.accessDepartureTime) && Double.isNaN(item.egressDepartureTime)) {
            throw new IllegalStateException("Encountered a Feeder Sequence with no drt trips");
        }
        item.destinationLink = network.getLinks().get(event.getLinkId());
        //There seems to be a bug with two drt trips following each other with no PT in the middle
        if(item.egressTransitLineId != null) {
            itemsList.add(item);
        }

    }

    public static boolean breakpoint(Id<Person> personId) {
        return (personId == null || personId.toString().equals(PERSON_ID_TO_DEBUG)) && (ITERATION_TO_DEBUG < 0 || currentIteration == ITERATION_TO_DEBUG);
    }

    @Override
    public void reset(int iteration) {
        currentIteration = iteration;
        this.currentItems.clear();
        this.itemsList.clear();
        this.interactionActivitiesSequences.clear();
        this.lastPersonVehicles.clear();
        this.lastNonInteractionActivity.clear();
    }

    public List<FeederTripSequenceItem> getItemsList() {
        return new ArrayList<>(this.itemsList);
    }
}
