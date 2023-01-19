package org.matsim.contribs.discrete_mode_choice.model;

import org.matsim.core.events.handler.EventHandler;

public interface NoTourFoundForPersonEventHandler extends EventHandler {

    void handleEvent(NoTourFoundForPersonEvent event);
}
