package org.eqasim.ile_de_france.drt.analysis;

import com.google.inject.Singleton;
import org.matsim.core.controler.AbstractModule;

public class DvrpAnalsisModule extends AbstractModule {
	@Override
	public void install() {
		bind(DvrpAnalysisListener.class).in(Singleton.class);
		addControlerListenerBinding().to(DvrpAnalysisListener.class);
	}
}
