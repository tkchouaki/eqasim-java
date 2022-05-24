package org.eqasim.ile_de_france;

import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.vdf.VDFConfigGroup;
import org.eqasim.vdf.VDFModule;
import org.eqasim.vdf.VDFQSimModule;
import org.matsim.contrib.cba.CbaConfigGroup;
import org.matsim.contrib.cba.CbaModule;


public class IDFConfigurator extends EqasimConfigurator {

    public IDFConfigurator(boolean useVdf, boolean useCba) {
        super(!useVdf);
        if(useVdf) {
            this.configGroups.add(new VDFConfigGroup());
            this.modules.add(new VDFModule());
            this.qsimModules.add(new VDFQSimModule());
        }
        if(useCba) {
            this.modules.add(new CbaModule());
            this.configGroups.add(new CbaConfigGroup());
        }
    }

    public IDFConfigurator() {
        this(false);
    }

    public IDFConfigurator(boolean useVdf) {
        this(useVdf, false);
    }
}
