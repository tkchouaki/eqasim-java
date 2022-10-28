package org.eqasim.ile_de_france.grand_paris;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;


import org.apache.log4j.Logger;
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

    private static final Logger log = Logger.getLogger(AdjustSchedule.class);

    private static final String CSV_SEPARATOR = ";";
    private static final String FACILITIES_CSV_STOP_ID_COLUMN = "stop_id";
    private static final String FACILITIES_CSV_STOP_NAME_COLUMN = "stop_name";
    private static final String TRAVEL_TIMES_CSV_FROM_STOP_NAME_COLUMN = "from_name";
    private static final String TRAVEL_TIMES_CSV_TO_STOP_NAME_COLUMN = "to_name";
    private static final String TRAVEL_TIMES_CSV_TO_LINE_ID_COLUMN = "line_id";
    private static final String TRAVEL_TIMES_CSV_TRAVEL_TIME_COLUMN = "travel_time";
    private static final String FREQUENCIES_CSV_LINE_ID_COLUMN = "line_id";
    private static final String FREQUENCIES_CSV_FREQUENCY_COLUMN = "frequency";
    private static final String FREQUENCIES_CSV_MODE_COLUMN = "mode";
    private static final Double MAX_DEPARTURE_TIME = 24.0 * 3600;
    private static final String DEFAULT_SPEED_KM_H = "40";

    /**
     * Retrieves a TransitStopFacility object that has a given name under the context of a given TransitLine id follwoing the logic below:
     * - If a TransitStopFacility with the given name has been added into the schedule during the AdjustSchedule process, it is returned
     * - If not, the given *lineId* should have a fallback TransitLine Id of a TransitLine that exists in the schedule. The TransitStopFacilities that appear in the routes of the line are then searched to find one with the given name.
     *      - If it exists, the TransitStopFacility is returned
     *      - If a problem occurs, an exception is raised
     * @param stopFacilityName The name of the desired TransitStopFacility
     * @param map A map name->TransitStopFacility of the facilities that have been added to the schedule in the AdjustSchedule process
     * @param schedule
     * @param lineId The Id of the TransitLine
     * @param fallbacksMap A map name->Id<TransitLine>
     * @return A TransitStopFacility with the given name
     */
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

    public static NetworkRoute createNetworkRoute(LinkNetworkRouteFactory linkNetworkRouteFactory, List<Id<Link>> linksIds) {
        NetworkRoute networkRoute = (NetworkRoute) linkNetworkRouteFactory.createRoute(linksIds.get(0),
                linksIds.get(linksIds.size() - 1));
        networkRoute.setLinkIds(linksIds.get(0), linksIds.subList(1, linksIds.size() - 1),
                linksIds.get(linksIds.size() - 1));
        return networkRoute;
    }

    public static List<TransitRoute> splitTransitRoute(TransitRoute transitRoute, Collection<String> borders, TransitSchedule schedule, Network network) {
        List<TransitRoute> newTransitRoutes = new ArrayList<>();
        List<TransitRouteStop> transitRouteStops = transitRoute.getStops();
        int i;
        int j=0;
        boolean included=true;
        List<TransitRouteStop> currentTransitRouteStops = new ArrayList<>();
        List<Id<Link>> currentLinkIds = new ArrayList<>();
        double lastArrivalOffset = 0;
        double lastDepartureOffset = 0;
        LinkNetworkRouteFactory linkNetworkRouteFactory = new LinkNetworkRouteFactory();
        for(i=0;i<transitRouteStops.size();i++) {
            if(transitRoute.getRoute() != null) {
                Link transitStopFacilityLink = network.getLinks().get(transitRouteStops.get(i).getStopFacility().getLinkId());
                for(;j<transitRoute.getRoute().getLinkIds().size() && !transitStopFacilityLink.getToNode().getId().equals(network.getLinks().get(transitRoute.getRoute().getLinkIds().get(j)).getFromNode().getId());j++) {
                    currentLinkIds.add(transitRoute.getRoute().getLinkIds().get(i));
                }
            }
            if(borders.contains(transitRouteStops.get(i).getStopFacility().getName())) {
                included = !included;
                if (!included) {
                    if(currentTransitRouteStops.size() > 0) {
                        TransitRouteStop transitRouteStop = transitRouteStops.get(i);
                        transitRouteStop = schedule.getFactory().createTransitRouteStop(transitRouteStop.getStopFacility(), transitRouteStop.getArrivalOffset().orElse(0)-lastArrivalOffset, transitRouteStop.getDepartureOffset().orElse(0)-lastDepartureOffset);
                        currentTransitRouteStops.add(transitRouteStop);
                        TransitRoute currentTransitRoute = schedule.getFactory().createTransitRoute(Id.create(transitRoute.getId().toString()+"_"+newTransitRoutes.size(), TransitRoute.class), createNetworkRoute(linkNetworkRouteFactory, currentLinkIds), currentTransitRouteStops, transitRoute.getTransportMode());
                        for(Departure departure: transitRoute.getDepartures().values()) {
                            Departure newDeparture = schedule.getFactory().createDeparture(Id.create(departure.getId().toString()+"_"+newTransitRoutes.size(), Departure.class), departure.getDepartureTime()+lastDepartureOffset);
                            currentTransitRoute.addDeparture(newDeparture);
                        }
                        newTransitRoutes.add(currentTransitRoute);
                        currentTransitRouteStops = new ArrayList<>();
                    }
                } else {
                    lastDepartureOffset = transitRouteStops.get(i).getDepartureOffset().orElse(0);
                    lastArrivalOffset = transitRouteStops.get(i).getDepartureOffset().orElse(0);
                    currentLinkIds = new ArrayList<>();
                    if(transitRoute.getRoute() != null) {
                        currentLinkIds.add(transitRoute.getRoute().getLinkIds().get(j));
                    }
                }
                continue;
            }
            if(included) {
                TransitRouteStop transitRouteStop = transitRouteStops.get(i);
                transitRouteStop = schedule.getFactory().createTransitRouteStop(transitRouteStop.getStopFacility(), transitRouteStop.getArrivalOffset().orElse(0)-lastArrivalOffset, transitRouteStop.getDepartureOffset().orElse(0)-lastDepartureOffset);
                currentTransitRouteStops.add(transitRouteStop);
            }
        }
        if(currentTransitRouteStops.size() > 0) {
            TransitRoute currentTransitRoute = schedule.getFactory().createTransitRoute(Id.create(transitRoute.getId().toString()+"_"+newTransitRoutes.size(), TransitRoute.class), createNetworkRoute(linkNetworkRouteFactory, currentLinkIds), currentTransitRouteStops, transitRoute.getTransportMode());
            for(Departure departure: transitRoute.getDepartures().values()) {
                Departure newDeparture = schedule.getFactory().createDeparture(Id.create(departure.getId().toString()+"_"+newTransitRoutes.size(), Departure.class), departure.getDepartureTime()+lastDepartureOffset);
                currentTransitRoute.addDeparture(newDeparture);
            }
            newTransitRoutes.add(currentTransitRoute);
        }
        return newTransitRoutes;
    }

    static public void main(String[] args) throws ConfigurationException, NumberFormatException, IOException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("schedule-path", "network-path", "facilities-path", "travel-times-path", "frequencies-path", "vehicles-path",
                        "output-schedule-path", "output-vehicles-path", "output-network-path") //
                .allowOptions("fallback-speed", "override-travel-times", "mapping")
                .build();

        double fallbackSpeed = Double.parseDouble(cmd.getOption("fallback-speed").orElse(DEFAULT_SPEED_KM_H));
        boolean overrideTravelTimes = cmd.hasOption("override-travel-times") && Boolean.parseBoolean(cmd.getOptionStrict("override-travel-times"));
        boolean doMapping = ! cmd.hasOption("mapping") || Boolean.parseBoolean(cmd.getOptionStrict("mapping"));

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);

        new TransitScheduleReader(scenario).readFile(cmd.getOptionStrict("schedule-path"));
        TransitSchedule schedule = scenario.getTransitSchedule();

        new MatsimNetworkReader(scenario.getNetwork()).readFile(cmd.getOptionStrict("network-path"));
        Network network = scenario.getNetwork();

        //To store the names of TransitStopFacility objects that are added to the schedule during the process
        Map<String, TransitStopFacility> facilitiesByName = new HashMap<>();

        Vehicles transitVehicles = VehicleUtils.createVehiclesContainer();
        new MatsimVehicleReader(transitVehicles).readFile(cmd.getOptionStrict("vehicles-path"));

        Id<VehicleType> subwayVehicleTypeId = Id.create("Subway", VehicleType.class);
        assert (transitVehicles.getVehicleTypes().containsKey(subwayVehicleTypeId));
        VehicleType subwayVehicleType = transitVehicles.getVehicleTypes().get(subwayVehicleTypeId);

        Set<String> ptModeSet = new HashSet<>();
        ptModeSet.add("pt");


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
                    if (doMapping) {
                        // In case we do the mapping, we create a node and a link for each new TransitStopFacility
                        Node stopNode = network.getFactory().createNode(Id.createNodeId("GPE:" + code), facility.getCoord());
                        network.addNode(stopNode);

                        Link stopLink = network.getFactory().createLink(Id.createLinkId("GPE:" + code), stopNode, stopNode);
                        stopLink.setAllowedModes(ptModeSet);
                        network.addLink(stopLink);

                        facility.setLinkId(stopLink.getId());
                    }
                }
            }
            reader.close();
        }

        //A map of (transit line name) -> (a list of the TransitStopFacility objects of the stops of the line (ordered))
        Map<String, List<TransitStopFacility>> routes = new HashMap<>();
        //For each transit line, stores the travel time between each two consecutive stops of the line. Note that if the travel time a->b is stored, b->a isn't.
        Map<String, Map<Id<TransitStopFacility>, Map<Id<TransitStopFacility>, Double>>> otherTravelTimes = new HashMap<>();

        Map<String, Id<TransitLine>> lineStopsFallbacks = new HashMap<>();
        //Line 14 is redefined
        lineStopsFallbacks.put("14", Id.create("IDFM:C01384", TransitLine.class));
        {
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
                    Map<Id<TransitStopFacility>, Map<Id<TransitStopFacility>, Double>> lineTravelTimes = otherTravelTimes.computeIfAbsent(transitLine, n -> new HashMap<>());
                    String fromName = row.get(header.indexOf(TRAVEL_TIMES_CSV_FROM_STOP_NAME_COLUMN));
                    String toName = row.get(header.indexOf(TRAVEL_TIMES_CSV_TO_STOP_NAME_COLUMN));
                    //Let's retrieve the TransitStopFacility objects either from the recently added ones or from existing ones if we are overriding the line
                    TransitStopFacility fromStop = getStopFacilityFromMapOrFallbackTransitLine(fromName, facilitiesByName, schedule, transitLine, lineStopsFallbacks);
                    TransitStopFacility toStop = getStopFacilityFromMapOrFallbackTransitLine(toName, facilitiesByName, schedule, transitLine, lineStopsFallbacks);

                    double travelTime = Double.parseDouble(row.get(header.indexOf(TRAVEL_TIMES_CSV_TRAVEL_TIME_COLUMN)));
                    if(travelTime < 0 || overrideTravelTimes) {
                        double euclideanDistance_km = CoordUtils.calcEuclideanDistance(fromStop.getCoord(), toStop.getCoord()) * 1e-3;
                        travelTime = 3600 * euclideanDistance_km / fallbackSpeed;
                    }
                    lineTravelTimes.computeIfAbsent(fromStop.getId(), n -> new HashMap<>()).put(toStop.getId(), travelTime);
                    lineTravelTimes.computeIfAbsent(toStop.getId(), n -> new HashMap<>()).put(fromStop.getId(), travelTime);
                    //We build the stop sequence for the line (we suppose that the stops are mentioned in the right order in the file)
                    List<TransitStopFacility> stops = routes.computeIfAbsent(transitLine, n -> new LinkedList<>());
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

        Map<String, Integer> frequencies = new HashMap<>();
        Map<String, String> modes = new HashMap<>();


        {
            // Reading the frequencies and transport modes for each line
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
                    int frequency = 3600 / Integer.parseInt(row.get(header.indexOf(FREQUENCIES_CSV_FREQUENCY_COLUMN)));
                    String mode = row.get(header.indexOf(FREQUENCIES_CSV_MODE_COLUMN));
                    frequencies.put(transitLine, frequency);
                    modes.put(transitLine, mode);
                }
            }

            reader.close();
        }

        {
            //Creating the transit lines (and overriding the ones with fallbacks)
            for (String line : routes.keySet()) {
                Id<TransitLine> transitLineId = Id.create("GPE:" + line, TransitLine.class);
                if (lineStopsFallbacks.containsKey(line)) {
                    schedule.removeTransitLine(schedule.getTransitLines().get(lineStopsFallbacks.get(line)));
                }
                TransitLine transitLine = schedule.getFactory().createTransitLine(transitLineId);
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

                        if (doMapping) {
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

                            forwardLink.setAllowedModes(ptModeSet);
                            backwardLink.setAllowedModes(ptModeSet);

                            network.addLink(forwardLink);
                            network.addLink(backwardLink);

                            forwardLinkIds.add(forwardLink.getId());
                            forwardLinkIds.add(forwardStop.getLinkId());

                            backwardLinkIds.add(backwardLink.getId());
                            backwardLinkIds.add(backwardStop.getLinkId());
                        }

                    } else {
                        if(doMapping) {
                            forwardLinkIds.add(forwardStop.getLinkId());
                            backwardLinkIds.add(backwardStop.getLinkId());
                        }
                    }

                    TransitRouteStop forwardTransitRouteStop = schedule.getFactory().createTransitRouteStop(forwardStop, forwardTime, forwardTime);
                    TransitRouteStop backwardTransitRouteStop = schedule.getFactory().createTransitRouteStop(backwardStop, backwardTime, backwardTime);
                    forwardStops.add(forwardTransitRouteStop);
                    backwardStops.add(backwardTransitRouteStop);
                }

                LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

                NetworkRoute forwardNetworkRoute = null;
                NetworkRoute backwardNetworkRoute = null;
                if(doMapping) {
                    forwardNetworkRoute = createNetworkRoute(routeFactory, forwardLinkIds);
                    backwardNetworkRoute = createNetworkRoute(routeFactory, backwardLinkIds);
                }


                TransitRoute forwardRoute = schedule.getFactory().createTransitRoute(
                        Id.create("GPE:" + line + ":forward", TransitRoute.class), forwardNetworkRoute, forwardStops,
                        modes.get(line));
                TransitRoute backwardRoute = schedule.getFactory().createTransitRoute(
                        Id.create("GPE:" + line + ":backward", TransitRoute.class), backwardNetworkRoute, backwardStops,
                        modes.get(line));
                forwardRoute.setTransportMode(modes.get(line));
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
            }
        }

        {
            String rightBorder = "Savigny-sur-Orge";
            String leftBorder = "Massy - Palaiseau";
            List<String> borders = new ArrayList<>();
            borders.add(rightBorder);
            borders.add(leftBorder);
            Id<TransitLine> transitLineId = Id.create("IDFM:C01727", TransitLine.class);
            VehicleType vehicleType = null;

             if(schedule.getTransitLines().containsKey(transitLineId)) {
                TransitLine transitLine = schedule.getTransitLines().get(transitLineId);
                Set<TransitRoute> transitRoutesToRemove = new HashSet<>();
                List<TransitRoute> transitRoutesToAdd = new ArrayList<>();
                for (TransitRoute transitRoute: transitLine.getRoutes().values()) {
                    boolean containsLeftBorder = false;
                    boolean containsRightBorder = false;
                    for(TransitRouteStop transitRouteStop: transitRoute.getStops()) {
                        if(transitRouteStop.getStopFacility().getName().equals(leftBorder)) {
                            containsLeftBorder = true;
                        } else if(transitRouteStop.getStopFacility().getName().equals(rightBorder)) {
                            containsRightBorder = true;
                        }
                    }
                    if(containsRightBorder && containsLeftBorder) {
                        List<TransitRoute> newTransitRoutes = splitTransitRoute(transitRoute, borders, schedule, network);
                        if(newTransitRoutes.size() != 1) {
                            transitRoutesToRemove.add(transitRoute);
                            transitRoutesToAdd.addAll(newTransitRoutes);
                            if(vehicleType==null) {
                                for(Departure departure: transitRoute.getDepartures().values()){
                                    vehicleType = transitVehicles.getVehicles().get(departure.getVehicleId()).getType();
                                    break;
                                }
                            }
                        }
                    }
                }
                transitRoutesToRemove.forEach(transitLine::removeRoute);
                for(TransitRoute transitRoute: transitRoutesToAdd) {
                    for(Departure departure: transitRoute.getDepartures().values()) {
                        Vehicle vehicle = transitVehicles.getFactory().createVehicle(Id.createVehicleId(departure.getId().toString()), vehicleType);
                        transitVehicles.addVehicle(vehicle);
                    }
                }
                transitRoutesToAdd.forEach(transitLine::addRoute);
            }
        }

        // Remove unused stop facilities and their related transfer times
        // First find the stop facilities that do not appear in any transitRoute
        Set<TransitStopFacility> transitStopFacilitiesToRemove = new HashSet<>(schedule.getFacilities().values());

        //At the same time find the vehicles that do not appear in any departure
        Set<Id<Vehicle>> vehiclesIdsToRemove = new HashSet<>(transitVehicles.getVehicles().keySet());

        for (TransitLine transitLine : schedule.getTransitLines().values()) {
            for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
                for (TransitRouteStop transitRouteStop : transitRoute.getStops()) {
                    transitStopFacilitiesToRemove.remove(transitRouteStop.getStopFacility());
                }
                for (Departure departure : transitRoute.getDepartures().values()) {
                    vehiclesIdsToRemove.remove(departure.getVehicleId());
                }
            }
        }
        for (TransitStopFacility transitStopFacility : transitStopFacilitiesToRemove) {
            log.info(String.format("Removing transit stop facility %s[%s]", transitStopFacility.getId().toString(), transitStopFacility.getName()));
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
        for (Id<Vehicle> vehicleId : vehiclesIdsToRemove) {
            log.info(String.format("Removing vehicle %s", vehicleId.toString()));
            transitVehicles.removeVehicle(vehicleId);
        }

        new TransitScheduleWriter(schedule).writeFile(cmd.getOptionStrict("output-schedule-path"));
        new MatsimVehicleWriter(transitVehicles).writeFile(cmd.getOptionStrict("output-vehicles-path"));
        new NetworkWriter(network).write(cmd.getOptionStrict("output-network-path"));
    }
}
