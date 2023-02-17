package org.eqasim.ile_de_france.drt;

import org.eqasim.ile_de_france.drt.mode_choice.utilities.drt_rejection_penalty.DrtRejectionPenaltyProviderConfigGroup;
import org.matsim.contrib.util.ReflectiveConfigGroupWithConfigurableParameterSets;

import javax.annotation.Nullable;

public class IDFDrtConfigGroup extends ReflectiveConfigGroupWithConfigurableParameterSets {
    public static final String GROUP_NAME = "IDFDrtModule";

    public static final String USE_FEEDER = "useFeeder";




    public IDFDrtConfigGroup() {
        super(GROUP_NAME);
        addDefinition(DrtRejectionPenaltyProviderConfigGroup.SET_NAME, DrtRejectionPenaltyProviderConfigGroup::new, () -> drtRejectionPenaltyProvider,
                params -> drtRejectionPenaltyProvider = (DrtRejectionPenaltyProviderConfigGroup) params);

    }

    private boolean useFeeder = false;

    @Nullable
    DrtRejectionPenaltyProviderConfigGroup drtRejectionPenaltyProvider;


    @StringGetter(USE_FEEDER)
    public boolean isUsingFeeder() {
        return this.useFeeder;
    }

    @StringSetter(USE_FEEDER)
    public void setUsingFeeder(boolean useFeeder) {
        this.useFeeder = useFeeder;
    }

    @Nullable
    public DrtRejectionPenaltyProviderConfigGroup getDrtRejectionPenaltyProviderConfig() {
        return drtRejectionPenaltyProvider;
    }
}

