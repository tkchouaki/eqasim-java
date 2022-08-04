package org.eqasim.ile_de_france.feeder.analysis.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.events.handler.GenericEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.schedule.*;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.vrpagent.TaskStartedEvent;
import org.matsim.core.api.experimental.events.EventsManager;

import java.util.HashMap;
import java.util.Map;

public class DvrpTaskStartedEventDetector implements GenericEventHandler {

    private final EventsManager eventsManager;
    private final Map<String, DrtTaskType> taskTypeMap = new HashMap<>();
    public DvrpTaskStartedEventDetector(EventsManager eventsManager) {
        this.eventsManager = eventsManager;
        taskTypeMap.put(EmptyVehicleRelocator.RELOCATE_VEHICLE_TASK_TYPE.name(), EmptyVehicleRelocator.RELOCATE_VEHICLE_TASK_TYPE);
        taskTypeMap.put(DrtDriveTask.TYPE.name(), DrtDriveTask.TYPE);
        taskTypeMap.put(DrtStayTask.TYPE.name(), DrtStayTask.TYPE);
        taskTypeMap.put(DrtStopTask.TYPE.name(), DrtStopTask.TYPE);
    }

    @Override
    public void handleEvent(GenericEvent event) {
        if(event.getEventType().equals(TaskStartedEvent.EVENT_TYPE)) {
            double time = event.getTime();
            String dvrpMode = event.getAttributes().get("dvrpMode");
            Id<DvrpVehicle> vehicleId = Id.create(event.getAttributes().get("dvrpVehicle"), DvrpVehicle.class);
            Id<Person> personId = Id.create(event.getAttributes().get("person"), Person.class);
            Task.TaskType taskType = taskTypeMap.get(event.getAttributes().get("taskType"));
            if(taskType == null) {
                throw new IllegalStateException("No TaskType object found for " + event.getAttributes().get("taskType"));
            }
            int taskIndex = Integer.parseInt(event.getAttributes().get("taskIndex"));
            Id<Link> linkId = Id.createLinkId(event.getAttributes().get("link"));
            eventsManager.processEvent(new TaskStartedEvent(time, dvrpMode, vehicleId, personId, taskType, taskIndex, linkId));
        }
    }
}
