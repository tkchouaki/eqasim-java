package org.matsim.contribs.discrete_mode_choice.model;

import org.matsim.core.events.handler.EventHandler;

public interface ModeChoicePerformedEventHandler extends EventHandler {
    void handleEvent(ModeChoicePerformedEvent event);
}
