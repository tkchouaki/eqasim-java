package org.eqasim.ile_de_france.drt;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.eqasim.core.analysis.PersonAnalysisFilter;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.ile_de_france.drt.mode_choice.IDFDrtModeAvailability;
import org.eqasim.ile_de_france.drt.mode_choice.cost.DrtCostModel;
import org.eqasim.ile_de_france.drt.mode_choice.parameters.IDFDrtCostParameters;
import org.eqasim.ile_de_france.drt.mode_choice.parameters.IDFDrtModeParameters;
import org.eqasim.ile_de_france.drt.mode_choice.utilities.*;
import org.eqasim.ile_de_france.feeder.FeederConstraint;
import org.eqasim.ile_de_france.feeder.FeederUtilityEstimator;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFCostParameters;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFModeParameters;
import org.matsim.core.config.CommandLine;

import java.io.File;
import java.util.Map;

public class IDFDrtModule extends AbstractEqasimExtension {

	public enum DrtVariablesEstimator { Regular, ExperienceBased, ExperienceBasedWithRejectionPenalty};

	private final CommandLine commandLine;

	private final boolean useFeeder;

	private final DrtVariablesEstimator drtVariablesEstimator;

	public IDFDrtModule(CommandLine commandLine ) {
		this(commandLine, DrtVariablesEstimator.Regular, false);
	}

	public IDFDrtModule(CommandLine commandLine, DrtVariablesEstimator drtVariablesEstimator, boolean useFeeder) {
		this.commandLine = commandLine;
		this.useFeeder = useFeeder;
		this.drtVariablesEstimator = drtVariablesEstimator;
	}

	@Override
	protected void installEqasimExtension() {
		// Configure mode availability
		bindModeAvailability(IDFDrtModeAvailability.NAME).toInstance(new IDFDrtModeAvailability(this.useFeeder));

		// Configure choice alternative for DRT
		bindUtilityEstimator("drt").to(DrtUtilityEstimator.class);
		bindCostModel("drt").to(DrtCostModel.class);
		Provider<DrtVariablesExperienceEstimator> provider = new Provider<DrtVariablesExperienceEstimator>() {
			@Inject
			@Named("drt")
			CostModel costModel;

			@Override
			public DrtVariablesExperienceEstimator get() {
				return new DrtVariablesExperienceEstimator(costModel);
			}
		};
		Provider<DrtVariablesExperienceBasedWithPenaltyRejectionEstimator> penaltyRejectionEstimatorProvider = new Provider<DrtVariablesExperienceBasedWithPenaltyRejectionEstimator>() {
			@Inject
			@Named("drt")
			CostModel costModel;

			@Override
			public DrtVariablesExperienceBasedWithPenaltyRejectionEstimator get() {
				return new DrtVariablesExperienceBasedWithPenaltyRejectionEstimator(costModel);
			}
		};

		switch (drtVariablesEstimator) {
			case Regular:
				bind(DrtPredictorInterface.class).to(DrtPredictor.class);
				break;
			case ExperienceBased:
				bind(DrtVariablesExperienceEstimator.class).toProvider(provider).asEagerSingleton();
				bind(DrtPredictorInterface.class).to(DrtVariablesExperienceEstimator.class).asEagerSingleton();
				addEventHandlerBinding().to(DrtVariablesExperienceEstimator.class).asEagerSingleton();
				break;

			case ExperienceBasedWithRejectionPenalty:
				bind(DrtVariablesExperienceBasedWithPenaltyRejectionEstimator.class).toProvider(penaltyRejectionEstimatorProvider).asEagerSingleton();
				bind(DrtPredictorInterface.class).to(DrtVariablesExperienceBasedWithPenaltyRejectionEstimator.class).asEagerSingleton();
				addEventHandlerBinding().to(DrtVariablesExperienceBasedWithPenaltyRejectionEstimator.class).asEagerSingleton();
		}
		if(useFeeder) {
			bindUtilityEstimator("feeder").to(FeederUtilityEstimator.class);
			bindTripConstraintFactory(FeederConstraint.NAME).to(FeederConstraint.Factory.class);
		}

		// Define filter for trip analysis
		bind(PersonAnalysisFilter.class).to(DrtPersonAnalysisFilter.class);

		// Override parameter bindings
		bind(ModeParameters.class).to(IDFDrtModeParameters.class);
		bind(IDFModeParameters.class).to(IDFDrtModeParameters.class);
		bind(IDFCostParameters.class).to(IDFDrtCostParameters.class);
	}

	@Provides
	@Singleton
	public DrtCostModel provideDrtCostModel(IDFDrtCostParameters parameters) {
		return new DrtCostModel(parameters);
	}

	@Provides
	@Singleton
	public IDFDrtCostParameters provideCostParameters(EqasimConfigGroup config) {
		IDFDrtCostParameters parameters = IDFDrtCostParameters.buildDefault();

		if (config.getCostParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getCostParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("cost-parameter", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Singleton
	public IDFDrtModeParameters provideModeParameters(EqasimConfigGroup config) {
		IDFDrtModeParameters parameters = IDFDrtModeParameters.buildDefault();

		if (config.getModeParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("mode-parameter", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Named("drt")
	public CostModel provideCarCostModel(Map<String, Provider<CostModel>> factory, EqasimConfigGroup config) {
		return getCostModel(factory, config, "drt");
	}

}
