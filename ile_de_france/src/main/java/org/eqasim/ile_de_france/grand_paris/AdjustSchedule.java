package org.eqasim.ile_de_france.grand_paris;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;


import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.*;


public class AdjustSchedule {
    private static final String CSV_SEPARATOR = ";";
    private static final String FACILITIES_CSV_STOP_ID_COLUMN = "stop_id";
    private static final String FACILITIES_CSV_STOP_NAME_COLUMN = "stop_name";
    private static final String TRAVEL_TIMES_CSV_FROM_STOP_NAME_COLUMN = "from_name";
    private static final String TRAVEL_TIMES_CSV_TO_STOP_NAME_COLUMN = "to_name";
    private static final String TRAVEL_TIMES_CSV_TO_LINE_ID_COLUMN = "line_id";
    private static final String TRAVEL_TIMES_CSV_TRAVEL_TIME_COLUMN = "travel_time";
    private static final String FREQUENCIES_CSV_LINE_ID_COLUMN = "line_id";
    private static final String FREQUENCIES_CSV_FREQUENCY_COLUMN = "frequency";
    private static final Double MAX_DEPARTURE_TIME = 24.0 * 3600;
    private static final String DEFAULT_SPEED_KM_H = "40";

    public static TransitStopFacility getStopFacilityFromMapOrFallbackTransitLine(String stopFacilityName, Map<String, TransitStopFacility> map, TransitSchedule schedule, String lineId, Map<String, Id<TransitLine>> fallbacksMap) {
        if (!map.containsKey(stopFacilityName)) {
            if (!fallbacksMap.containsKey(lineId)) {
                throw new IllegalStateException("Cannot find stop '" + stopFacilityName + "' in default map and no fallback transit line is provided for line '" + lineId + "'");
            }
            Id<TransitLine> transitLineId = fallbacksMap.get(lineId);
            if (!schedule.getTransitLines().containsKey(transitLineId)) {
                throw new IllegalStateException("The fallback transit line '" + transitLineId + "' specified for line '" + lineId + "' cannot be found in the scedule");
            }
            TransitLine transitLine = schedule.getTransitLines().get(transitLineId);
            for (TransitRoute route : transitLine.getRoutes().values()) {
                for (TransitRouteStop stop : route.getStops()) {
                    if (stop.getStopFacility().getName().equals(stopFacilityName)) {
                        return stop.getStopFacility();
                    }
                }
            }
            throw new IllegalStateException("The fallback transit line '" + transitLineId + "' specified for line '" + lineId + "' Has no route containing a stop with the name '" + stopFacilityName + "'");
        } else {
            return map.get(stopFacilityName);
        }
    }

    static public void main(String[] args) throws ConfigurationException, NumberFormatException, IOException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("schedule-path", "network-path", "facilities-path", "travel-times-path", "frequencies-path", "vehicles-path",
                        "output-schedule-path", "output-vehicles-path", "output-network-path") //
                .allowOptions("fallback-speed", "override-travel-times")
                .build();

        double fallbackSpeed = Double.parseDouble(cmd.getOption("fallback-speed").orElse(DEFAULT_SPEED_KM_H));
        boolean overrideTravelTimes = cmd.hasOption("override-travel-times") && Boolean.parseBoolean(cmd.getOptionStrict("override-travel-times"));

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);

        new TransitScheduleReader(scenario).readFile(cmd.getOptionStrict("schedule-path"));
        TransitSchedule schedule = scenario.getTransitSchedule();

        new MatsimNetworkReader(scenario.getNetwork()).readFile(cmd.getOptionStrict("network-path"));
        Network network = scenario.getNetwork();

        Map<String, TransitStopFacility> facilitiesByName = new HashMap<>();

        Vehicles transitVehicles = VehicleUtils.createVehiclesContainer();
        new MatsimVehicleReader(transitVehicles).readFile(cmd.getOptionStrict("vehicles-path"));

        Id<VehicleType> subwayVehicleTypeId = Id.create("Subway", VehicleType.class);
        assert (transitVehicles.getVehicleTypes().containsKey(subwayVehicleTypeId));
        VehicleType subwayVehicleType = transitVehicles.getVehicleTypes().get(subwayVehicleTypeId);


        {
            // Set up stop facilities
            // Reading facilities from CSV
            // Creating stop facilities, adding them to the schedule, & to the network by creating a node on each new facility
            // Why create a link from & to the facility node
            // Ignoring previously existing facilities
            String facilitiesPath = cmd.getOptionStrict("facilities-path");
            String line;
            List<String> header = null;

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(facilitiesPath)));

            while ((line = reader.readLine()) != null) {
                List<String> row = Arrays.asList(line.split(CSV_SEPARATOR));

                if (header == null) {
                    header = row;
                } else {
                    String code = row.get(header.indexOf(FACILITIES_CSV_STOP_ID_COLUMN));

                    Coord coord = new Coord(Double.parseDouble(row.get(header.indexOf("x"))),
                            Double.parseDouble(row.get(header.indexOf("y"))));

                    TransitStopFacility facility = schedule.getFactory().createTransitStopFacility(Id.create("GPE:" + code, TransitStopFacility.class), coord, false);
                    facility.setName(row.get(header.indexOf(FACILITIES_CSV_STOP_NAME_COLUMN)));
                    schedule.addStopFacility(facility);
                    facilitiesByName.put(facility.getName(), facility);

                    Node stopNode = network.getFactory().createNode(Id.createNodeId("GPE:" + code), facility.getCoord());
                    network.addNode(stopNode);

                    Link stopLink = network.getFactory().createLink(Id.createLinkId("GPE:" + code), stopNode, stopNode);
                    network.addLink(stopLink);

                    facility.setLinkId(stopLink.getId());
                }
            }

            reader.close();
        }

        Map<String, List<TransitStopFacility>> routes = new HashMap<>();
        Map<String, Map<Id<TransitStopFacility>, Map<Id<TransitStopFacility>, Double>>> otherTravelTimes = new HashMap<>();
        Map<String, Id<TransitLine>> lineStopsFallbacks = new HashMap<>();
        lineStopsFallbacks.put("14", Id.create("IDFM:C01384", TransitLine.class));
        {
            // Set up lines
            // Reading the the
            String routesPath = cmd.getOptionStrict("travel-times-path");
            String line;
            List<String> header = null;

            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(routesPath)));

            while ((line = reader.readLine()) != null) {
                List<String> row = Arrays.asList(line.split(CSV_SEPARATOR));

                if (header == null) {
                    header = row;
                } else {
                    String transitLine = row.get(header.indexOf(TRAVEL_TIMES_CSV_TO_LINE_ID_COLUMN));
                    List<TransitStopFacility> stops = routes.computeIfAbsent(transitLine, n -> new LinkedList<>());
                    Map<Id<TransitStopFacility>, Map<Id<TransitStopFacility>, Double>> lineTravelTimes = otherTravelTimes.computeIfAbsent(transitLine, n -> new HashMap<>());
                    String fromName = row.get(header.indexOf(TRAVEL_TIMES_CSV_FROM_STOP_NAME_COLUMN));
                    String toName = row.get(header.indexOf(TRAVEL_TIMES_CSV_TO_STOP_NAME_COLUMN));
                    TransitStopFacility fromStop = getStopFacilityFromMapOrFallbackTransitLine(fromName, facilitiesByName, schedule, transitLine, lineStopsFallbacks);
                    TransitStopFacility toStop = getStopFacilityFromMapOrFallbackTransitLine(toName, facilitiesByName, schedule, transitLine, lineStopsFallbacks);
                    double travelTime = Double.parseDouble(row.get(header.indexOf(TRAVEL_TIMES_CSV_TRAVEL_TIME_COLUMN)));
                    if(travelTime < 0 || overrideTravelTimes) {
                        double euclideanDistance_km = CoordUtils.calcEuclideanDistance(fromStop.getCoord(), toStop.getCoord()) * 1e-3;
                        travelTime = 3600 * euclideanDistance_km / fallbackSpeed;
                    }
                    lineTravelTimes.computeIfAbsent(fromStop.getId(), n -> new HashMap<>()).put(toStop.getId(), travelTime);
                    lineTravelTimes.computeIfAbsent(toStop.getId(), n -> new HashMap<>()).put(fromStop.getId(), travelTime);
                    if (stops.size() == 0) {
                        stops.add(fromStop);
                    } else if (!fromName.equals(stops.get(stops.size() - 1).getName())) {
                        throw new IllegalStateException("The travel times file must specify the stops in an ordered way, the from_name of each line must be equal to the to_name of the previous line that has the same line id");
                    }
                    stops.add(toStop);
                }
            }
            reader.close();
        }

        Map<String, Double> frequencies = new HashMap<>();


        { // Set up departures
            String routesPath = cmd.getOptionStrict("frequencies-path");
            String line;
            List<String> header = null;

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(routesPath)));

            while ((line = reader.readLine()) != null) {
                List<String> row = Arrays.asList(line.split(CSV_SEPARATOR));

                if (header == null) {
                    header = row;
                } else {
                    String transitLine = row.get(header.indexOf(FREQUENCIES_CSV_LINE_ID_COLUMN));
                    double frequency = Double.parseDouble(row.get(header.indexOf(FREQUENCIES_CSV_FREQUENCY_COLUMN)));
                    frequencies.put(transitLine, frequency);
                }
            }

            reader.close();
        }

        {//Creating the transit lines (and overriding the ones with fallbacks)
            for (String line : routes.keySet()) {
                TransitLine transitLine = schedule.getFactory().createTransitLine(Id.create("GPE:" + line, TransitLine.class));
                schedule.addTransitLine(transitLine);
                List<TransitRouteStop> forwardStops = new LinkedList<>();
                List<TransitRouteStop> backwardStops = new LinkedList<>();
                double forwardTime = 0;
                double backwardTime = 0;

                List<Id<Link>> forwardLinkIds = new LinkedList<>();
                List<Id<Link>> backwardLinkIds = new LinkedList<>();

                for (int i = 0; i < routes.get(line).size(); i++) {
                    TransitStopFacility forwardStop = routes.get(line).get(i);
                    TransitStopFacility backwardStop = routes.get(line).get(routes.get(line).size() - 1 - i);

                    if (forwardStops.size() > 0) {
                        forwardTime += otherTravelTimes.get(line).get(forwardStops.get(forwardStops.size() - 1).getStopFacility().getId()).get(forwardStop.getId());
                        backwardTime += otherTravelTimes.get(line).get(backwardStops.get(backwardStops.size() - 1).getStopFacility().getId()).get(backwardStop.getId());

                        Id<Link> previousForwardLinkId = forwardLinkIds.get(forwardLinkIds.size() - 1);
                        Id<Link> previousBackwardLinkId = backwardLinkIds.get(backwardLinkIds.size() - 1);

                        Link forwardLink = network.getFactory().createLink( //
                                Id.createLinkId("GPE:" + line + ":forward:" + i), //
                                network.getLinks().get(previousForwardLinkId).getToNode(), //
                                network.getLinks().get(forwardStop.getLinkId()).getFromNode());

                        Link backwardLink = network.getFactory().createLink( //
                                Id.createLinkId("GPE:" + line + ":backward:" + i), //
                                network.getLinks().get(previousBackwardLinkId).getToNode(), //
                                network.getLinks().get(backwardStop.getLinkId()).getFromNode());

                        network.addLink(forwardLink);
                        network.addLink(backwardLink);

                        forwardLinkIds.add(forwardLink.getId());
                        forwardLinkIds.add(forwardStop.getLinkId());

                        backwardLinkIds.add(backwardLink.getId());
                        backwardLinkIds.add(backwardStop.getLinkId());

                    } else {
                        forwardLinkIds.add(forwardStop.getLinkId());
                        backwardLinkIds.add(backwardStop.getLinkId());
                    }

                    TransitRouteStop forwardTransitRouteStop = schedule.getFactory().createTransitRouteStop(forwardStop, forwardTime, forwardTime);
                    TransitRouteStop backwardTransitRouteStop = schedule.getFactory().createTransitRouteStop(backwardStop, backwardTime, backwardTime);
                    forwardStops.add(forwardTransitRouteStop);
                    backwardStops.add(backwardTransitRouteStop);
                }

                LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

                NetworkRoute forwardNetworkRoute = (NetworkRoute) routeFactory.createRoute(forwardLinkIds.get(0),
                        forwardLinkIds.get(forwardLinkIds.size() - 1));
                forwardNetworkRoute.setLinkIds(forwardLinkIds.get(0), forwardLinkIds.subList(1, forwardLinkIds.size() - 1),
                        forwardLinkIds.get(forwardLinkIds.size() - 1));

                NetworkRoute backwardNetworkRoute = (NetworkRoute) routeFactory.createRoute(backwardLinkIds.get(0),
                        backwardLinkIds.get(backwardLinkIds.size() - 1));
                backwardNetworkRoute.setLinkIds(backwardLinkIds.get(0),
                        backwardLinkIds.subList(1, backwardLinkIds.size() - 1),
                        backwardLinkIds.get(backwardLinkIds.size() - 1));

                TransitRoute forwardRoute = schedule.getFactory().createTransitRoute(
                        Id.create("GPE:" + line + ":forward", TransitRoute.class), forwardNetworkRoute, forwardStops,
                        "subway");

                TransitRoute backwardRoute = schedule.getFactory().createTransitRoute(
                        Id.create("GPE:" + line + ":backward", TransitRoute.class), backwardNetworkRoute, backwardStops,
                        "subway");
                transitLine.addRoute(forwardRoute);
                transitLine.addRoute(backwardRoute);

                for (double departureTime = 0; departureTime < MAX_DEPARTURE_TIME; departureTime += frequencies.get(line)) {
                    Id<Departure> forwardDepartureId = Id.create("GPE:" + line + ":forward:" + Time.writeTime(departureTime), Departure.class);
                    Id<Departure> backwardDepartureId = Id.create("GPE:" + line + ":backward:" + Time.writeTime(departureTime), Departure.class);

                    Departure forwardDeparture = schedule.getFactory().createDeparture(forwardDepartureId, departureTime);
                    Departure backwardDeparture = schedule.getFactory().createDeparture(backwardDepartureId, departureTime);

                    Vehicle forwardVehicle = transitVehicles.getFactory().createVehicle(Id.createVehicleId(forwardDepartureId.toString()), subwayVehicleType);
                    Vehicle backwardVehicle = transitVehicles.getFactory().createVehicle(Id.createVehicleId(backwardDepartureId.toString()), subwayVehicleType);

                    forwardDeparture.setVehicleId(forwardVehicle.getId());
                    backwardDeparture.setVehicleId(backwardVehicle.getId());

                    transitVehicles.addVehicle(forwardVehicle);
                    transitVehicles.addVehicle(backwardVehicle);

                    forwardRoute.addDeparture(forwardDeparture);
                    backwardRoute.addDeparture(backwardDeparture);
                }
                if (lineStopsFallbacks.containsKey(line)) {
                    schedule.removeTransitLine(schedule.getTransitLines().get(lineStopsFallbacks.get(line)));
                }
            }
        }

        // Remove unused stop facilities and their related transfer times
        // First find the stop facilities that do not appear in any transitRoute
        Set<TransitStopFacility> transitStopFacilities = new HashSet<>(schedule.getFacilities().values());

        //At the same time find the vehicles that do not appear in any departure
        Set<Id<Vehicle>> vehiclesIds = new HashSet<>(transitVehicles.getVehicles().keySet());

        for (TransitLine transitLine : schedule.getTransitLines().values()) {
            for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
                for (TransitRouteStop transitRouteStop : transitRoute.getStops()) {
                    transitStopFacilities.remove(transitRouteStop.getStopFacility());
                }
                for (Departure departure : transitRoute.getDepartures().values()) {
                    vehiclesIds.remove(departure.getVehicleId());
                }
            }
        }
        for (TransitStopFacility transitStopFacility : transitStopFacilities) {
            schedule.removeStopFacility(transitStopFacility);
            // find the transfer times related to the current stop facility
            MinimalTransferTimes.MinimalTransferTimesIterator iter = schedule.getMinimalTransferTimes().iterator();
            List<Map.Entry<Id<TransitStopFacility>, Id<TransitStopFacility>>> transferTimesToRemove = new ArrayList<>();
            while (iter.hasNext()) {
                iter.next();
                Id<TransitStopFacility> fromStopId = iter.getFromStopId();
                Id<TransitStopFacility> toStopId = iter.getToStopId();
                if (transitStopFacility.getId().equals(fromStopId) || transitStopFacility.getId().equals(toStopId)) {
                    transferTimesToRemove.add(new AbstractMap.SimpleEntry<>(fromStopId, toStopId));
                }
            }
            for (Map.Entry<Id<TransitStopFacility>, Id<TransitStopFacility>> entry : transferTimesToRemove) {
                schedule.getMinimalTransferTimes().remove(entry.getKey(), entry.getValue());
            }
        }
        for (Id<Vehicle> vehicleId : vehiclesIds) {
            transitVehicles.removeVehicle(vehicleId);
        }

        new TransitScheduleWriter(schedule).writeFile(cmd.getOptionStrict("output-schedule-path"));
        new MatsimVehicleWriter(transitVehicles).writeFile(cmd.getOptionStrict("output-vehicles-path"));
        new NetworkWriter(network).write(cmd.getOptionStrict("output-network-path"));
    }
}
