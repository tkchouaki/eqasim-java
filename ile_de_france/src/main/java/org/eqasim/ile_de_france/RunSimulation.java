package org.eqasim.ile_de_france;

import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.eqasim.ile_de_france.mode_choice.epsilon.EpsilonModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class RunSimulation {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path").allowOptions("use-vdf", "use-cba", "use-epsilon") //
				.allowPrefixes("mode-choice-parameter", "cost-parameter") //
				.build();
		boolean useVdf = cmd.hasOption("use-vdf") && Boolean.parseBoolean(cmd.getOptionStrict("use-vdf"));
		boolean useCba = cmd.hasOption("use-cba") && Boolean.parseBoolean(cmd.getOptionStrict("use-cba"));
		boolean useEpsilon = cmd.hasOption("use-epsilon") && Boolean.parseBoolean(cmd.getOptionStrict("use-epsilon"));
		IDFConfigurator configurator = new IDFConfigurator(useVdf, useCba);
		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), configurator.getConfigGroups());
		cmd.applyConfiguration(config);
		Scenario scenario = ScenarioUtils.createScenario(config);
		configurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);

		Controler controller = new Controler(scenario);
		configurator.configureController(controller);
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new IDFModeChoiceModule(cmd));
		if (useVdf) {
			config.qsim().setStorageCapFactor(100000);
			config.qsim().setFlowCapFactor(100000);
			config.qsim().setStuckTime(24.0 * 3600.0);
			config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.queue);
		}
		if(useEpsilon) {
			controller.addOverridingModule(new EpsilonModule());
		}
		controller.run();
	}
}