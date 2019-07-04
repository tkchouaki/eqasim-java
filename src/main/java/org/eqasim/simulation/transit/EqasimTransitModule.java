package org.eqasim.simulation.transit;

import org.eqasim.simulation.transit.connection.DefaultTransitConnectionFinder;
import org.eqasim.simulation.transit.connection.TransitConnectionFinder;
import org.eqasim.simulation.transit.departure.DefaultDepartureFinder;
import org.eqasim.simulation.transit.departure.DepartureFinder;
import org.eqasim.simulation.transit.routing.DefaultEnrichedTransitRouter;
import org.eqasim.simulation.transit.routing.EnrichedTransitRouter;
import org.eqasim.simulation.transit.routing.EnrichedTransitRoutingModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.pt.config.TransitRouterConfigGroup;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;

public class EqasimTransitModule extends AbstractModule {
	@Override
	public void install() {
		bind(TransitRouter.class).to(SwissRailRaptor.class);
		addRoutingModuleBinding("pt").to(EnrichedTransitRoutingModule.class);

		bind(DepartureFinder.class).to(DefaultDepartureFinder.class);
		bind(DefaultTransitConnectionFinder.class).to(DefaultTransitConnectionFinder.class);
	}

	@Provides
	public EnrichedTransitRouter provideEnrichedTransitRouter(TransitRouter delegate, TransitSchedule transitSchedule,
			TransitConnectionFinder connectionFinder, Network network, PlansCalcRouteConfigGroup routeConfig,
			TransitRouterConfigGroup transitConfig) {
		double beelineDistanceFactor = routeConfig.getBeelineDistanceFactors().get("walk");
		double additionalTransferTime = transitConfig.getAdditionalTransferTime();

		return new DefaultEnrichedTransitRouter(delegate, transitSchedule, connectionFinder, network,
				beelineDistanceFactor, additionalTransferTime);
	}

	@Provides
	@Singleton
	public TransitSchedule provideTransitSchedule(Scenario scenario) {
		return scenario.getTransitSchedule();
	}
}
