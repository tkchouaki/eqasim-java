package org.eqasim.ile_de_france.drt;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.transit.EqasimTransitQSimModule;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.ile_de_france.IDFConfigurator;
import org.eqasim.ile_de_france.drt.analysis.DvrpAnalsisModule;
import org.eqasim.ile_de_france.drt.mode_choice.IDFDrtModeAvailability;
import org.eqasim.ile_de_france.drt.rejections.RejectionConstraint;
import org.eqasim.ile_de_france.drt.rejections.RejectionModule;
import org.eqasim.ile_de_france.feeder.FeederModule;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.drt.optimizer.insertion.DrtInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.insertion.SelectiveInsertionSearchParams;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import java.nio.file.Path;;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class RunDrtSimulation {
    public static void main(String[] args) throws CommandLine.ConfigurationException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("config-path").allowOptions("drt-vehicles-path", "replace-trips-mode", "replace-probability", "use-feeder") //
                .allowPrefixes("mode-choice-parameter", "cost-parameter") //
                .build();

        IDFConfigurator configurator = new IDFConfigurator(false);
        String configPath = cmd.getOptionStrict("config-path");
        boolean useFeeder = cmd.hasOption("use-feeder") && Boolean.parseBoolean(cmd.getOptionStrict("use-feeder"));
        Double replaceProbability = cmd.hasOption("replace-probability") ? Double.parseDouble(cmd.getOptionStrict("replace-probability")) : 1;
        Random random = new Random(1234);
        Config config = null;
        ConfigGroup[] configGroups = configurator.getConfigGroups();
        if(cmd.hasOption("drt-vehicles-path")) {
            config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), configGroups);
        } else {
            ConfigGroup[] newConfigGroups = new ConfigGroup[configGroups.length+1];
            for(int i=0; i<configGroups.length; i++) {
                newConfigGroups[i] = configGroups[i];
            }
            newConfigGroups[configGroups.length] = new MultiModeDrtConfigGroup();
            config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), newConfigGroups);
        }

        { // Configure DVRP
            DvrpConfigGroup dvrpConfig = new DvrpConfigGroup();
            config.addModule(dvrpConfig);
        }
        cmd.applyConfiguration(config);

        MultiModeDrtConfigGroup multiModeDrtConfig = null;

        if(cmd.hasOption("drt-vehicles-path")){ // Configure DRT
            String drtVehiclesPath = cmd.getOptionStrict("drt-vehicles-path");
            String relativeDrtVehiclePath = Path.of(configPath).getParent().relativize(Path.of(drtVehiclesPath)).toString();
            multiModeDrtConfig = new MultiModeDrtConfigGroup();
            config.addModule(multiModeDrtConfig);

            DrtConfigGroup drtConfig = new DrtConfigGroup();
            drtConfig.setMode("drt");
            drtConfig.setOperationalScheme(DrtConfigGroup.OperationalScheme.door2door);
            drtConfig.setStopDuration(15.0);
            drtConfig.setMaxWaitTime(600.0);
            drtConfig.setMaxTravelTimeAlpha(1.5);
            drtConfig.setMaxTravelTimeBeta(300.0);
            drtConfig.setVehiclesFile(relativeDrtVehiclePath);

            DrtInsertionSearchParams searchParams = new SelectiveInsertionSearchParams();
            drtConfig.addDrtInsertionSearchParams(searchParams);

            multiModeDrtConfig.addDrtConfig(drtConfig);
            DrtConfigs.adjustMultiModeDrtConfig(multiModeDrtConfig, config.planCalcScore(), config.plansCalcRoute());

            // Additional requirements
            config.qsim().setStartTime(0.0);
            config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
        }
        else {
            if(config.getModules().get(MultiModeDrtConfigGroup.GROUP_NAME) == null) {
                throw new IllegalStateException("When drt-vehicles-path argument is not passed, MultiModeDrt module should be specified in the configuration");
            }
            multiModeDrtConfig = (MultiModeDrtConfigGroup) config.getModules().get(MultiModeDrtConfigGroup.GROUP_NAME);
        }

        for(DrtConfigGroup drtConfigGroup : multiModeDrtConfig.getModalElements()) {
            drtConfigGroup.setNumberOfThreads(config.global().getNumberOfThreads());
        }

        if(cmd.hasOption("drt-vehicles-path")){ // Add the DRT mode to the choice model
            DiscreteModeChoiceConfigGroup dmcConfig = DiscreteModeChoiceConfigGroup.getOrCreate(config);

            // Add DRT to the available modes
            dmcConfig.setModeAvailability(IDFDrtModeAvailability.NAME);

            // Add DRT to cached modes
            Set<String> cachedModes = new HashSet<>();
            cachedModes.addAll(dmcConfig.getCachedModes());
            cachedModes.add("drt");
            dmcConfig.setCachedModes(cachedModes);

            // Set up choice model
            EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);
            eqasimConfig.setCostModel("drt", "drt");
            eqasimConfig.setEstimator("drt", "drt");

            // Add rejection constraint
            if (cmd.getOption("use-rejection-constraint").map(Boolean::parseBoolean).orElse(false)) {
                Set<String> tripConstraints = new HashSet<>(dmcConfig.getTripConstraints());
                tripConstraints.add(RejectionConstraint.NAME);
                dmcConfig.setTripConstraints(tripConstraints);
            }

            // Set analysis interval
            eqasimConfig.setTripAnalysisInterval(1);
        }

        if(cmd.hasOption("drt-vehicles-path")){ // Set up some defaults for MATSim scoring
            PlanCalcScoreConfigGroup.ModeParams modeParams = new PlanCalcScoreConfigGroup.ModeParams("drt");
            config.planCalcScore().addModeParams(modeParams);
        }

        Scenario scenario = ScenarioUtils.createScenario(config);
        configurator.configureScenario(scenario);

        { // Add DRT route factory
            scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DrtRoute.class,
                    new DrtRouteFactory());
        }

        ScenarioUtils.loadScenario(scenario);

        if(cmd.hasOption("replace-trips-mode")) {
            String mode = cmd.getOptionStrict("replace-trips-mode");
            for(Person person : scenario.getPopulation().getPersons().values()) {
                for(Plan plan : person.getPlans()) {
                    for(PlanElement planElement : plan.getPlanElements()) {
                        if(random.nextDouble() < replaceProbability) {
                            if(planElement instanceof Leg) {
                                Leg leg = (Leg) planElement;
                                if(leg.getMode().equals(mode)) {
                                    leg.setRoute(null);
                                    leg.setMode("drt");
                                }
                            }
                        }
                    }
                }
            }
        }

        Controler controller = new Controler(scenario);
        configurator.configureController(controller);
        controller.addOverridingModule(new EqasimAnalysisModule());
        controller.addOverridingModule(new EqasimModeChoiceModule());
        controller.addOverridingModule(new IDFModeChoiceModule(cmd));
        { // Configure controller for DRT
            controller.addOverridingModule(new DvrpModule());
            controller.addOverridingModule(new MultiModeDrtModule());

            MultiModeDrtConfigGroup finalMultiModeDrtConfig = multiModeDrtConfig;
            Config finalConfig = config;
            controller.configureQSimComponents(components -> {
                DvrpQSimComponents.activateAllModes(finalMultiModeDrtConfig).configure(components);

                // Need to re-do this as now it is combined with DRT
                EqasimTransitQSimModule.configure(components, finalConfig);
            });
        }

        { // Add overrides for Corsica + DRT
            controller.addOverridingModule(new IDFDrtModule(cmd, useFeeder));
            controller.addOverridingModule(new RejectionModule(Arrays.asList("drt")));
            controller.addOverridingModule(new DvrpAnalsisModule());
            if(useFeeder) {
                controller.addOverridingModule(new FeederModule(null, scenario.getTransitSchedule()));
            }
        }

        controller.run();
    }
}
