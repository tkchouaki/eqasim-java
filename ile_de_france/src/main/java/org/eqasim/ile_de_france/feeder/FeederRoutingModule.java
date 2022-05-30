package org.eqasim.ile_de_france.feeder;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.*;

import java.util.*;


//TODO see OutsideConstraint class
//TODO can inject pt & DRT estimators
//TODO LOOK AT SWITCH BETWEEN CAR & PT for FEEDER
//TODO LOOK AT WAITING TIMES FOR DRT
//TODO ALSO REJECTIONS
//TODO Check DRT's min cost flow code for example on gathering drt perf stats
//TODO Gare du nord 5h25



public class FeederRoutingModule implements RoutingModule {
	private final RoutingModule drtRoutingModule;
	private final RoutingModule transitRoutingModule;

	private final PopulationFactory populationFactory;

	private final TransitSchedule schedule;

	private static final boolean FORCE_EGRESS = true;

	private QuadTree<Facility> quadTree;

	public  FeederRoutingModule(RoutingModule feederRoutingModule, RoutingModule transitRoutingModule,
							   PopulationFactory populationFactory, TransitSchedule schedule, Network drtNetwork) {
		this.drtRoutingModule = feederRoutingModule;
		this.transitRoutingModule = transitRoutingModule;
		this.populationFactory = populationFactory;
		this.schedule = schedule;
		double[] bounds = NetworkUtils.getBoundingBox(drtNetwork.getNodes().values());
		quadTree = new QuadTree<>(bounds[0], bounds[1], bounds[2], bounds[3]);
		Set<Id<TransitStopFacility>> processedFacilities = new HashSet<>();

		for(TransitLine transitLine : schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()){
				if(transitRoute.getTransportMode().equals("rail") || transitRoute.getTransportMode().equals("subway")) {
					for(TransitRouteStop transitRouteStop : transitRoute.getStops()) {
						TransitStopFacility transitStopFacility = transitRouteStop.getStopFacility();
						if(!processedFacilities.contains(transitStopFacility.getId())) {
							processedFacilities.add(transitStopFacility.getId());
							Facility interactionFacility = FacilitiesUtils.wrapLink(NetworkUtils.getNearestLink(drtNetwork, transitStopFacility.getCoord()));
							try {
								if (!quadTree.put(transitStopFacility.getCoord().getX(), transitStopFacility.getCoord().getY(), interactionFacility)) {
									System.out.println("Cannot add this stop : " + transitStopFacility.getName());
								}
							} catch (IllegalArgumentException exception) {
								System.out.println("Cannot add this stop because it's out of DRT's network : " + transitStopFacility.getName());
							}
						}
					}
				}
			}
		}
	}

	@Override
	public List<? extends PlanElement> calcRoute(Facility fromFacility, Facility toFacility, double departureTime,
			Person person) {
		Facility accessFacility = this.quadTree.getClosest(fromFacility.getCoord().getX(), fromFacility.getCoord().getY());
		Facility egressFacility = this.quadTree.getClosest(toFacility.getCoord().getX(), toFacility.getCoord().getY());

		List<PlanElement> intermodalRoute = new LinkedList<>();
		List<? extends PlanElement> drtRoute = drtRoutingModule.calcRoute(fromFacility, accessFacility, departureTime, person);
		double accessTime = departureTime;
		if(drtRoute == null) {
			accessFacility = fromFacility;
		}
		else {
			intermodalRoute.addAll(drtRoute);
			for (PlanElement element : intermodalRoute) {
				if (element instanceof Leg) {
					Leg leg = (Leg) element;
					accessTime = Math.max(accessTime, leg.getDepartureTime().seconds());
					accessTime += leg.getTravelTime().seconds();
				}
			}
			Activity accessInteractionActivity = populationFactory.createActivityFromLinkId("feeder interaction", accessFacility.getLinkId());
			accessInteractionActivity.setMaximumDuration(0);
			intermodalRoute.add(accessInteractionActivity);
		}

		List<PlanElement> ptRoute = new LinkedList<>(transitRoutingModule.calcRoute(accessFacility, egressFacility, accessTime, person));
		double egressTime = accessTime;

		for (PlanElement element : ptRoute) {
			if (element instanceof Leg) {
				Leg leg = (Leg) element;
				egressTime = Math.max(egressTime, leg.getDepartureTime().seconds());
				egressTime += leg.getTravelTime().seconds();
			}
		}

		drtRoute = drtRoutingModule.calcRoute(egressFacility, toFacility, egressTime, person);

		if(drtRoute == null) {
			intermodalRoute.addAll(transitRoutingModule.calcRoute(accessFacility, toFacility, accessTime, person));
		} else {
			intermodalRoute.addAll(ptRoute);
			Activity egressInteractionActivity = populationFactory.createActivityFromLinkId("feeder interaction", egressFacility.getLinkId());
			egressInteractionActivity.setMaximumDuration(0);
			intermodalRoute.add(egressInteractionActivity);
			intermodalRoute.addAll(drtRoute);
		}
		return intermodalRoute;
	}
}
