package org.matsim.contribs.discrete_mode_choice.model;

import org.matsim.core.events.handler.EventHandler;

public interface TourSelectorEventHandler extends EventHandler {
   void handleEvent(TourSelectorEvent tourSelectorEvent);
}
