package org.eqasim.ile_de_france.drt.mode_choice;

import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.eqasim.core.simulation.mode_choice.epsilon.EpsilonAdapter;
import org.eqasim.core.simulation.mode_choice.epsilon.EpsilonProvider;
import org.eqasim.ile_de_france.drt.mode_choice.utilities.DrtUtilityEstimator;
import org.eqasim.ile_de_france.mode_choice.epsilon.EpsilonModule;

public class DrtEpsilonModule extends EpsilonModule {

    @Override
    public void installEqasimExtension() {
        super.installEqasimExtension();
        bind(DrtUtilityEstimator.class);
        bindUtilityEstimator("epsilon_drt").to(Key.get(EpsilonAdapter.class, Names.named("epsilon_drt")));
    }

    @Provides
    @Named("epsilon_drt")
    EpsilonAdapter provideEpsilonDrtEstimator(DrtUtilityEstimator delegate, EpsilonProvider epsilonProvider) {
        return new EpsilonAdapter("drt", delegate, epsilonProvider);
    }
}
