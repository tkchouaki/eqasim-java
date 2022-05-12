package org.eqasim.core.analysis.pt;

import java.io.IOException;
import java.util.Collection;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

public class RunPublicTransportTripAnalysis {
	static public void main(String[] args) throws IOException, ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("events-path", "output-path", "schedule-path") //
				.build();

		String outputPath = cmd.getOptionStrict("output-path");
		String eventsPath = cmd.getOptionStrict("events-path");

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		new TransitScheduleReader(scenario).readFile(cmd.getOptionStrict("schedule-path"));
		TransitSchedule schedule = scenario.getTransitSchedule();


		PublicTransportTripListener tripListener = new PublicTransportTripListener(schedule);
		PublicTransportTripReader reader = new PublicTransportTripReader(tripListener);
		Collection<PublicTransportTripItem> trips = reader.readTrips(eventsPath);

		new PublicTransportTripWriter(trips).write(outputPath);
	}
}
