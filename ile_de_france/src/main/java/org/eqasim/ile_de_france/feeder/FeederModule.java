package org.eqasim.ile_de_france.feeder;

import com.google.common.base.Verify;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.PtUtilityEstimator;
import org.eqasim.ile_de_france.drt.mode_choice.utilities.DrtPredictor;
import org.eqasim.ile_de_france.drt.mode_choice.utilities.DrtPredictorInterface;
import org.eqasim.ile_de_france.drt.mode_choice.utilities.DrtUtilityEstimator;
import org.eqasim.ile_de_france.drt.mode_choice.utilities.DrtVariablesExperienceEstimator;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.optimizer.rebalancing.demandestimator.PreviousIterationDRTDemandEstimator;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpMode;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.RoutingModule;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.HashMap;
import java.util.Map;

public class FeederModule extends AbstractEqasimExtension {
	public final static String FEEDER_MODE = "feeder";

	private final Id<Link> interactionLinkId;
	private final String areaPath;
	private final TransitSchedule schedule;

	public FeederModule(String areaPath, Id<Link> interactionLinkId) {
		this(areaPath, interactionLinkId, null);
	}

	public FeederModule(String areaPath, TransitSchedule transitSchedule) {
		this(areaPath, null, transitSchedule);
	}

	private FeederModule(String areaPath, Id<Link> interactionLinkId, TransitSchedule transitSchedule) {
		this.areaPath = areaPath;
		this.interactionLinkId = interactionLinkId;
		this.schedule = transitSchedule;
	}

	@Override
	protected void installEqasimExtension() {
		addRoutingModuleBinding(FEEDER_MODE).to(FeederRoutingModule.class);

		bindTripConstraintFactory(FeederConstraint.NAME).to(FeederConstraint.Factory.class);
		bindUtilityEstimator(FEEDER_MODE).to(FeederUtilityEstimator.class);

		bind(DrtUtilityEstimator.class);
		bind(PtUtilityEstimator.class);
		bind(DrtPredictorInterface.class).to(DrtVariablesExperienceEstimator.class);
	}
	/*
	@Provides
	private DrtVariablesExperienceEstimator provideDrtPredictorInterface(@DvrpMode("drt") DrtZonalSystem drtZonalSystem) {
		return null;
	}*/

	@Provides
	private FeederRoutingModule provideFeederRoutingModule(@Named("drt") RoutingModule drtRoutingModule,
			@DvrpMode("drt") Network drtNetwork, @Named("pt") RoutingModule ptRoutingModule, Population population) {

		return new FeederRoutingModule(drtRoutingModule, ptRoutingModule, population.getFactory(), this.schedule, drtNetwork);
	}

	@Provides
	@Singleton
	private FeederConstraint.Factory provideFeederConstraintFactory(TransitSchedule schedule, @DvrpMode("drt") Network drtNetwork) {
		Link interactionLink = NetworkUtils.getNearestLink(drtNetwork, new Coord(645531.00004, 6847391.0046));
		return new FeederConstraint.Factory(FacilitiesUtils.wrapLink(interactionLink), schedule);
	}

	@Provides
	@Singleton
	private FeederArea provideFeederArea() {
		//return new FeederArea(ConfigGroup.getInputFileURL(getConfig().getContext(), areaPath));
		return null;
	}
}
