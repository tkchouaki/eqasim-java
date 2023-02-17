package org.eqasim.core.analysis.pt;

import org.eqasim.core.components.transit.events.PublicTransitEvent;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.events.handler.GenericEventHandler;
import org.matsim.pt.transitSchedule.api.*;

import java.util.*;

public class PublicTransportStationUsageListener implements GenericEventHandler {
    private final Map<Id<TransitStopFacility>, PublicTransportStationUsageItem> usagesMap = new HashMap<>();
    private final TransitSchedule transitSchedule;
    private final Set<String> modes = new HashSet<>();
    private final IdSet<TransitStopFacility> relevantStops = new IdSet<>(TransitStopFacility.class);

    private final boolean mergeOverlappingStops;

    private final IdMap<TransitStopFacility, TransitStopFacility> facilitiesMap = new IdMap<>(TransitStopFacility.class);

    public PublicTransportStationUsageListener(TransitSchedule transitSchedule) {
        this(transitSchedule, false);
    }

    public PublicTransportStationUsageListener(TransitSchedule transitSchedule, boolean mergeOverlappingStops) {
        this.transitSchedule = transitSchedule;
        this.mergeOverlappingStops = mergeOverlappingStops;
    }

    public PublicTransportStationUsageListener(TransitSchedule transitSchedule, Collection<String> modes) {
        this(transitSchedule, false, modes);
    }

    public PublicTransportStationUsageListener(TransitSchedule transitSchedule, boolean mergeOverlappingStops, Collection<String> modes) {
        this(transitSchedule, mergeOverlappingStops);
        this.modes.addAll(modes);
        if(modes.size() > 0) {
            for(TransitLine transitLine: transitSchedule.getTransitLines().values()) {
                for(TransitRoute transitRoute: transitLine.getRoutes().values()) {
                    if(this.modes.contains(transitRoute.getTransportMode())) {
                        transitRoute.getStops().forEach(stop -> this.relevantStops.add(stop.getStopFacility().getId()));
                    }
                }
            }
        }
    }

    @Override
    public void handleEvent(GenericEvent event) {
        if(event instanceof PublicTransitEvent) {
            PublicTransitEvent publicTransitEvent = (PublicTransitEvent) event;
            Id<TransitStopFacility> accessStopId = publicTransitEvent.getAccessStopId();
            Id<TransitStopFacility> egressStopId = publicTransitEvent.getEgressStopId();
            if(this.modes.size() == 0 || this.relevantStops.contains(accessStopId)) {
                TransitStopFacility accessStop = this.transitSchedule.getFacilities().get(accessStopId);
                if(this.mergeOverlappingStops) {
                    accessStop = this.getMerged(accessStop);
                }
                PublicTransportStationUsageItem.initOrAddAccess(accessStop, this.usagesMap);
            }
            if(this.modes.size() == 0 || this.relevantStops.contains(egressStopId)) {
                TransitStopFacility egressStop = this.transitSchedule.getFacilities().get(egressStopId);
                if(this.mergeOverlappingStops) {
                    egressStop = this.getMerged(egressStop);
                }
                PublicTransportStationUsageItem.initOrAddEgress(egressStop, this.usagesMap);
            }
        }
    }

    private TransitStopFacility getMerged(TransitStopFacility facility) {
        if(!this.facilitiesMap.containsKey(facility.getId())) {
            boolean found = false;
            for(TransitStopFacility otherFacility: this.facilitiesMap.values()) {
                if(facility.getCoord().equals(otherFacility.getCoord())) {
                    found = true;
                    this.facilitiesMap.put(facility.getId(), otherFacility);
                    break;
                }
            }
            if(!found) {
                this.facilitiesMap.put(facility.getId(), facility);
            }
        }
        return this.facilitiesMap.get(facility.getId());
    }

    public Map<Id<TransitStopFacility>, PublicTransportStationUsageItem> getUsagesMap() {
        return this.usagesMap;
    }

    @Override
    public void reset(int iteration) {
        usagesMap.clear();
    }
}
