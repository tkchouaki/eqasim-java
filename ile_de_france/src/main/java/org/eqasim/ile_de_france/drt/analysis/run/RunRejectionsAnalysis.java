package org.eqasim.ile_de_france.drt.analysis.run;

import org.eqasim.ile_de_france.drt.analysis.passengers.PassengerAnalysisListener;
import org.eqasim.ile_de_france.drt.analysis.passengers.PassengerAnalysisWriter;
import org.eqasim.ile_de_france.drt.analysis.rejections.RejectionsAnalysisListener;
import org.eqasim.ile_de_france.drt.analysis.utils.LinkFinder;
import org.eqasim.ile_de_france.drt.analysis.utils.VehicleRegistry;
import org.eqasim.ile_de_france.feeder.analysis.utils.DvrpTaskStartedEventDetector;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.events.handler.GenericEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.CommandLine;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class RunRejectionsAnalysis {

    public static void main(String[] args) throws CommandLine.ConfigurationException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("events-path", "network-path", "output-path") //
                .build();

        String eventsPath = cmd.getOptionStrict("events-path");
        String networkPath = cmd.getOptionStrict("network-path");
        String outputPath = cmd.getOptionStrict("output-path");

        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(networkPath);

        //LinkFinder linkFinder = new LinkFinder(network);
        VehicleRegistry vehicleRegistry = new VehicleRegistry();

        EventsManager eventsManager = EventsUtils.createEventsManager();
        eventsManager.addHandler(vehicleRegistry);
        eventsManager.addHandler(new DvrpTaskStartedEventDetector(eventsManager));
        eventsManager.addHandler(new GenericEventHandler() {
            @Override
            public void handleEvent(GenericEvent event) {
                if(event.getEventType().equals(PassengerRequestRejectedEvent.EVENT_TYPE)) {
                    eventsManager.processEvent(PassengerRequestRejectedEvent.convert(event));
                }
            }
        });
        eventsManager.addHandler(new RejectionsAnalysisListener(network, outputPath));

        eventsManager.initProcessing();
        new MatsimEventsReader(eventsManager).readFile(eventsPath);
        eventsManager.finishProcessing();
    }
}
