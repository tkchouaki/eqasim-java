package org.eqasim.ile_de_france.drt;

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
import org.eqasim.ile_de_france.drt.mode_choice.utilities.DrtPredictor;
import org.eqasim.ile_de_france.drt.mode_choice.utilities.DrtUtilityEstimator;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFCostParameters;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFModeParameters;
import org.matsim.core.config.CommandLine;

import java.io.File;
import java.util.Map;

public class IDFDrtModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;

	public IDFDrtModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {
		// Configure mode availability
		bindModeAvailability(IDFDrtModeAvailability.NAME).to(IDFDrtModeAvailability.class);

		// Configure choice alternative for DRT
		bindUtilityEstimator("drt").to(DrtUtilityEstimator.class);
		bindCostModel("drt").to(DrtCostModel.class);
		bind(DrtPredictor.class);

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
