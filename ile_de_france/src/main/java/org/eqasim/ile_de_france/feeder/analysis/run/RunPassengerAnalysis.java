package org.eqasim.ile_de_france.feeder.analysis.run;

import org.eqasim.ile_de_france.drt.analysis.utils.VehicleRegistry;
import org.eqasim.ile_de_france.feeder.analysis.passengers.FeederTripSequenceListener;
import org.eqasim.ile_de_france.feeder.analysis.passengers.FeederTripSequenceWriter;
import org.eqasim.ile_de_france.feeder.analysis.utils.DvrpTaskStartedEventDetector;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.CommandLine;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

import java.io.File;
import java.io.IOException;


public class RunPassengerAnalysis {

    private final FeederTripSequenceListener feederTripSequenceListener;
    private final String outputPath;

    public RunPassengerAnalysis(Network network, String outputPath) {
        this(new FeederTripSequenceListener(new VehicleRegistry(), network), outputPath);
    }

    public RunPassengerAnalysis(FeederTripSequenceListener feederTripSequenceListener, String outputPath) {
        this.feederTripSequenceListener = feederTripSequenceListener;
        this.outputPath = outputPath;
    }

    public void prepare(EventsManager eventsManager) {
        eventsManager.addHandler(this.feederTripSequenceListener);
        eventsManager.addHandler(new DvrpTaskStartedEventDetector(eventsManager));
    }

    public void writeAnalysis() throws IOException {
        new FeederTripSequenceWriter(this.feederTripSequenceListener).writeTripItems(new File(this.outputPath));
    }

    static public void main(String[] args) throws CommandLine.ConfigurationException, IOException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("events-path", "network-path", "output-path") //
                .build();

        String eventsPath = cmd.getOptionStrict("events-path");
        String networkPath = cmd.getOptionStrict("network-path");
        String outputPath = cmd.getOptionStrict("output-path");

        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(networkPath);

        RunPassengerAnalysis analysis = new RunPassengerAnalysis(network, outputPath);
        EventsManager eventsManager = EventsUtils.createEventsManager();
        analysis.prepare(eventsManager);


        eventsManager.initProcessing();
        new MatsimEventsReader(eventsManager).readFile(eventsPath);
        eventsManager.finishProcessing();

        analysis.writeAnalysis();
    }
}
