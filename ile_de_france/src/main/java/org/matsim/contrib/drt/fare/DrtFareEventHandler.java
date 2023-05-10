package org.matsim.contrib.drt.fare;

import org.matsim.core.events.handler.EventHandler;

public interface DrtFareEventHandler extends EventHandler {
    public void handleEvent(DrtFareEvent event);
}
