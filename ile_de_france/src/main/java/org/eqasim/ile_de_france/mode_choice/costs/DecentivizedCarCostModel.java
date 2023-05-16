package org.eqasim.ile_de_france.mode_choice.costs;

import org.eqasim.core.analysis.trips.TripItem;
import org.eqasim.core.analysis.trips.TripListener;
import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.replanning.ReplanningContext;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DecentivizedCarCostModel extends AbstractCostModel implements IterationEndsListener {

    private final CostModel delegate;
    private final IncentivizedWalkCostModel incentivizedWalkCostModel;
    private final TripListener tripListener;

    private double extraCost;

    private final ReplanningContext replanningContext;

    private final OutputDirectoryHierarchy outputDirectoryHierarchy;

    private int lastIteration;

    private final Map<Integer, Double> extraCostHistory;

    public DecentivizedCarCostModel(CostModel delegate, IncentivizedWalkCostModel incentivizedWalkCostModel, TripListener tripListener, ReplanningContext replanningContext, OutputDirectoryHierarchy outputDirectoryHierarchy) {
        super("car");
        this.delegate = delegate;
        this.incentivizedWalkCostModel = incentivizedWalkCostModel;
        this.tripListener = tripListener;
        this.extraCost = 0;
        this.replanningContext = replanningContext;
        this.outputDirectoryHierarchy = outputDirectoryHierarchy;
        this.extraCostHistory = new HashMap<>();
    }

    @Override
    public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        this.update();
        double baseCost = this.delegate.calculateCost_MU(person, trip, elements);
        double extraCost = this.extraCost * this.getInVehicleDistance_km(elements);
        return baseCost + extraCost;
    }

    synchronized private void update() {
        if(this.replanningContext.getIteration() > this.lastIteration) {
            this.lastIteration = replanningContext.getIteration();
            this.updateExtraCost();
            this.writeToFile();
        }
    }

    private void updateExtraCost() {
        double totalPaid = 0;
        double totalCarDistance = 0;
        for(TripItem tripItem: this.tripListener.getTripItems()) {
            double paid = - this.incentivizedWalkCostModel.getIncentive(tripItem);
            totalPaid += paid;
            if(tripItem.mode.equals("car")) {
                totalCarDistance += tripItem.vehicleDistance / 1000;
            }
        }
        if(totalCarDistance != 0) {
            this.extraCostHistory.put(this.lastIteration-1, extraCost);
            extraCost = totalPaid / totalCarDistance;
            this.extraCostHistory.put(this.lastIteration, extraCost);
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        this.updateExtraCost();
    }

    private void writeToFile() {
        try {
            String outputPath = this.outputDirectoryHierarchy.getOutputFilename("decentivizedWalk.csv");
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));
            writer.write( "personId;tripId;mode\n");
            writer.flush();
            for(Object iteration: this.extraCostHistory.keySet().stream().sorted().toArray()) {
                writer.write(iteration+";"+this.extraCostHistory.get(iteration)+"\n");
                writer.flush();
            }
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
