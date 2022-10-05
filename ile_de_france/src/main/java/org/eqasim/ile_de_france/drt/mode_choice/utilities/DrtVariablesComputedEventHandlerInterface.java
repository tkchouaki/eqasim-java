package org.eqasim.ile_de_france.drt.mode_choice.utilities;

import org.matsim.core.events.handler.EventHandler;

public interface DrtVariablesComputedEventHandlerInterface extends EventHandler {
    void handleEvent(DrtVariablesComputedEvent event);
}
