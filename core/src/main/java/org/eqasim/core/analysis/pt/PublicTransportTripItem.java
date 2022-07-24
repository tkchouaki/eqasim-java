package org.eqasim.core.analysis.pt;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class PublicTransportTripItem {
	public Id<Person> personId;
	public int personTripId;

	public Id<TransitStopFacility> accessStopId;
	public Id<TransitStopFacility> egressStopId;

	public Id<TransitLine> transitLineId;
	public Id<TransitRoute> transitRouteId;

	public double distance;
	public double travelTime;
	public double vehicleDepartureTime;
	public String routeMode;
	public String lineName;

	public PublicTransportTripItem(Id<Person> personId, int personTripId, Id<TransitStopFacility> accessStopId,
			Id<TransitStopFacility> egressStopId, Id<TransitLine> transitLineId, Id<TransitRoute> transitRouteId, double distance, double vehicleDepartureTime, double travelTime, String lineName, String routeMode) {
		this.personId = personId;
		this.personTripId = personTripId;

		this.accessStopId = accessStopId;
		this.egressStopId = egressStopId;

		this.transitLineId = transitLineId;
		this.transitRouteId = transitRouteId;

		this.distance = distance;
		this.travelTime = travelTime;
		this.vehicleDepartureTime = vehicleDepartureTime;

		this.lineName = lineName;
		this.routeMode = routeMode;

		if(this.lineName == null)  {
			this.lineName = this.transitLineId.toString();
		}
	}
}