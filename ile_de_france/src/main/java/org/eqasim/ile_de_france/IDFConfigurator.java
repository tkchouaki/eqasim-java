package org.eqasim.ile_de_france;

import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.vdf.VDFModule;
import org.eqasim.vdf.VDFQSimModule;

import java.util.Arrays;

public class IDFConfigurator extends EqasimConfigurator {

    public IDFConfigurator(boolean useVdf) {
        super(!useVdf);
        if(useVdf) {
            this.modules.add(new VDFModule());
            this.qsimModules.add(new VDFQSimModule());
        }
    }

    public IDFConfigurator() {
        this(false);
    }
}
