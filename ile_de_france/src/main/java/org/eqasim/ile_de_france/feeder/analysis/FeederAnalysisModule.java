package org.eqasim.ile_de_france.feeder.analysis;

import com.google.inject.Singleton;
import org.matsim.core.controler.AbstractModule;

public class FeederAnalysisModule extends AbstractModule {

    @Override
    public void install() {
        bind(FeederAnalysisListener.class).in(Singleton.class);
        addControlerListenerBinding().to(FeederAnalysisListener.class);
    }
}
