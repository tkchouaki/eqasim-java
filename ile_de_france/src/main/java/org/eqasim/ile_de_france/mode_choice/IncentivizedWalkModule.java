package org.eqasim.ile_de_france.mode_choice;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.eqasim.core.analysis.trips.TripListener;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.ile_de_france.mode_choice.costs.DecentivizedCarCostModel;
import org.eqasim.ile_de_france.mode_choice.costs.IncentivizedWalkCostModel;
import org.eqasim.ile_de_france.mode_choice.costs.IncentivizedWalkCostModelAdapter;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFCostParameters;
import org.eqasim.ile_de_france.mode_choice.parameters.IncentivizedWalkParameters;
import org.eqasim.ile_de_france.mode_choice.utilities.estimators.IncentivizedWalkUtilityEstimator;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IncentivizedWalkPredictor;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.router.MainModeIdentifier;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Map;

public class IncentivizedWalkModule extends AbstractEqasimExtension {
    @Override
    protected void installEqasimExtension() {
        bind(IncentivizedWalkPredictor.class);
        //bindCostModel("incentivized_walk").to(IncentivizedWalkCostModel.class);
        bindUtilityEstimator("IncentivizedWalkUtilityEstimator").to(IncentivizedWalkUtilityEstimator.class);

        bind(IncentivizedWalkCostModel.class);
        bind(IDFCostParameters.class).to(IncentivizedWalkParameters.class);

        String suffix = "_incentivized_walk";
        String decentivizedCarSuffix = "_decentivized";

        EqasimConfigGroup eqasimConfigGroup = (EqasimConfigGroup) getConfig().getModules().get(EqasimConfigGroup.GROUP_NAME);
        for(String costModel: eqasimConfigGroup.getCostModels().values()) {
            if(costModel.endsWith(suffix)) {
                String baseModel = costModel.substring(0, costModel.length()-suffix.length());
                bindCostModel(costModel).toProvider(new Provider<>() {
                    @Inject
                    Map<String, Provider<CostModel>> factory;

                    @Inject
                    IncentivizedWalkCostModel incentivizedWalkCostModel;

                    @Inject
                    IncentivizedWalkParameters incentivizedWalkParameters;

                    @Override
                    public CostModel get() {
                        return new IncentivizedWalkCostModelAdapter(incentivizedWalkCostModel, factory.get(baseModel).get(), incentivizedWalkParameters);
                    }
                }).asEagerSingleton();
            }
            if(costModel.endsWith(decentivizedCarSuffix)) {
                String baseModel = costModel.substring(0, costModel.length()-decentivizedCarSuffix.length());
                bindCostModel(costModel).toProvider(new Provider<>() {
                    @Inject
                    Map<String, Provider<CostModel>> factory;

                    @Inject
                    IncentivizedWalkCostModel incentivizedWalkCostModel;

                    @Inject
                    TripListener tripListener;

                    @Inject
                    ReplanningContext replanningContext;

                    @Inject
                    OutputDirectoryHierarchy outputDirectoryHierarchy;

                    @Override
                    public CostModel get() {
                        return new DecentivizedCarCostModel(factory.get(baseModel).get(), incentivizedWalkCostModel, tripListener, replanningContext, outputDirectoryHierarchy);
                    }
                }).asEagerSingleton();
            }
        }
    }

    @Provides
    @Singleton
    public IncentivizedWalkParameters provideIncentivizedWalkParameters(EqasimConfigGroup config) throws URISyntaxException {
        IncentivizedWalkParameters incentivizedWalkParameters = IncentivizedWalkParameters.buildDefault();
        if(config.getCostParametersPath() != null) {
            ParameterDefinition.applyFile(new File(ConfigGroup.getInputFileURL(getConfig().getContext(), config.getCostParametersPath()).toURI()), incentivizedWalkParameters);
        }
        return incentivizedWalkParameters;
    }
    @Provides
    @Singleton
    @Named("incentivized_walk")
    public CostModel provideIncentivizedWalkCostModel(IncentivizedWalkParameters incentivizedWalkParameters, MainModeIdentifier mainModeIdentifier) {
         return new IncentivizedWalkCostModel(incentivizedWalkParameters, mainModeIdentifier);
    }
}
