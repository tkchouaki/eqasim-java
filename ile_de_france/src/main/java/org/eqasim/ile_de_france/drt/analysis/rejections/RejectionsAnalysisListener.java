package org.eqasim.ile_de_france.drt.analysis.rejections;

import org.apache.commons.io.FileUtils;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.contrib.dvrp.vrpagent.TaskStartedEvent;
import org.matsim.contrib.dvrp.vrpagent.TaskStartedEventHandler;
import org.matsim.vehicles.Vehicle;

import java.io.*;
import java.util.Map;

public class RejectionsAnalysisListener implements PassengerRequestRejectedEventHandler, LinkEnterEventHandler, PersonDepartureEventHandler, TaskStartedEventHandler {

    private final String basePath;
    private final IdMap<Vehicle, Id<Link>> vehiclesPosition = new IdMap<>(Vehicle.class);
    private final IdMap<Person, Id<Link>> personsPosition = new IdMap<>(Person.class);
    private final IdMap<Vehicle, String> vehiclesTasks = new IdMap<>(Vehicle.class);
    private final Network network;

    public RejectionsAnalysisListener(Network network, String basePath) {
        this.basePath = basePath;
        this.network = network;
    }

    @Override
    public void handleEvent(PassengerRequestRejectedEvent event) {
        File baseDirectory = new File(basePath);
        File f = new File(basePath, event.getRequestId().toString()+".csv");
        try {
            FileUtils.forceMkdir(baseDirectory);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f.getPath())));
            writer.write("x;y;id;kind;other\n");
            for(Map.Entry<Id<Vehicle>, Id<Link>> entry: this.vehiclesPosition.entrySet()) {
                Coord coord = network.getLinks().get(entry.getValue()).getCoord();
                writer.write(coord.getX()+";"+coord.getY()+";"+entry.getKey().toString()+";vehicle;"+this.vehiclesTasks.get(entry.getKey())+"\n");
            }
            Coord personCoord = network.getLinks().get(personsPosition.get(event.getPersonId())).getCoord();
            writer.write(personCoord.getX()+";"+personCoord.getY()+";"+event.getPersonId()+";person;"+event.getCause()+"\n");
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        if(event.getVehicleId().toString().startsWith("vehicle_drt")) {
            this.vehiclesPosition.put(event.getVehicleId(), event.getLinkId());
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if(event.getLegMode().equals("drt")){
            this.personsPosition.put(event.getPersonId(), event.getLinkId());
        }
    }

    @Override
    public void handleEvent(TaskStartedEvent event) {
        this.vehiclesTasks.put(Id.createVehicleId(event.getDvrpVehicleId()), event.getTaskType().name());
        this.vehiclesPosition.put(Id.createVehicleId(event.getDvrpVehicleId()), event.getLinkId());
    }

}
