package org.matsim.contribs.discrete_mode_choice.modules;

import com.google.inject.Inject;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.utils.ModeChoiceInTheLoopChecker;
import org.matsim.contribs.discrete_mode_choice.replanning.DiscreteModeChoiceStrategyProvider;
import org.matsim.contribs.discrete_mode_choice.replanning.NonSelectedPlanSelector;
import org.matsim.contribs.discrete_mode_choice.replanning.time_interpreter.TimeInterpreterModule;
import org.matsim.core.controler.AbstractModule;


public class EventFiringDiscreteModeChoiceModule extends AbstractModule {

    public static final String STRATEGY_NAME = "DiscreteModeChoice";

    @Inject
    private DiscreteModeChoiceConfigGroup dmcConfig;

    @Override
    public void install() {
        addPlanStrategyBinding(STRATEGY_NAME).toProvider(DiscreteModeChoiceStrategyProvider.class);

        if (getConfig().strategy().getPlanSelectorForRemoval().equals(NonSelectedPlanSelector.NAME)) {
            bindPlanSelectorForRemoval().to(NonSelectedPlanSelector.class);
        }

        if (dmcConfig.getEnforceSinglePlan()) {
            addControlerListenerBinding().to(ModeChoiceInTheLoopChecker.class);
        }

        install(new EventFiringModelModule());
        install(new TimeInterpreterModule());
    }
}


