package org.eqasim.ile_de_france.drt.mode_choice.utilities.drt_rejection_penalty;

import com.google.inject.Inject;
import org.eqasim.ile_de_france.drt.mode_choice.utilities.DrtVariablesExperienceBasedWithPenaltyRejectionEstimator;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DrtRejectionsLinearPenaltyProvider implements DrtRejectionPenaltyProvider, IterationEndsListener {

    private final static double TARGET_REJECTION_PROBABILITY = 0;
    private final static double INITIAL_REJECTIONS_PENALTY = 0;
    private final static double REJECTIONS_PENALTY_ALPHA = 0.3; //TO TEST 0.01 -> 0.3 -> 1
    private final static int ENABLE_AFTER_ITERATION = -1;
    private final static int MEMORY_SIZE = 10;
    private final static double MIN_REJECTION_RATE_DELTA = 0.02;
    private final static double MIN_REQUESTS_NUMBER_DELTA = 0.02;
    private double rejectionPenalty = INITIAL_REJECTIONS_PENALTY;
    private final DrtVariablesExperienceBasedWithPenaltyRejectionEstimator drtVariablesExperienceBasedWithPenaltyRejectionEstimator;
    private final MatsimServices services;
    private final Map<Integer, Double> penaltiesHistory = new HashMap<>();
    private final Map<Integer, Double> rejectionProbabilitiesHistory = new HashMap<>();
    private final Map<Integer, Double> requestsNumberDeltaHistory = new HashMap<>();
    private final Map<Integer, Double> rejectionRateDeltaHistory = new HashMap<>();
    private final List<Double> lastRejectionRates = new ArrayList<>();
    private final List<Double> lastPenalties = new ArrayList<>();
    private final List<Integer> lastNumberOfRequests = new ArrayList<>();
    private final Map<Integer, Double> numberOfRequestsToRejectionRates = new HashMap<>();


    @Inject
    public DrtRejectionsLinearPenaltyProvider(DrtVariablesExperienceBasedWithPenaltyRejectionEstimator drtVariablesExperienceBasedWithPenaltyRejectionEstimator, MatsimServices services) {
        this.drtVariablesExperienceBasedWithPenaltyRejectionEstimator = drtVariablesExperienceBasedWithPenaltyRejectionEstimator;
        this.services = services;
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        int lastNumberOfRequests = this.drtVariablesExperienceBasedWithPenaltyRejectionEstimator.getRequestsNumber();
        double lastRejectionProbability = this.drtVariablesExperienceBasedWithPenaltyRejectionEstimator.getRejectionProbability();
        this.rejectionProbabilitiesHistory.put(event.getIteration(), lastRejectionProbability);
        if(!this.penaltiesHistory.containsKey(event.getIteration())) {
            this.penaltiesHistory.put(event.getIteration(), INITIAL_REJECTIONS_PENALTY);
        }

        this.lastPenalties.add(this.rejectionPenalty);
        this.lastRejectionRates.add(lastRejectionProbability);
        this.lastNumberOfRequests.add(this.drtVariablesExperienceBasedWithPenaltyRejectionEstimator.getRequestsNumber());
        if(this.lastPenalties.size() > MEMORY_SIZE) {
            this.lastPenalties.remove(0);
            this.lastRejectionRates.remove(0);
            this.lastNumberOfRequests.remove(0);
        }

        if(!this.numberOfRequestsToRejectionRates.containsKey(lastNumberOfRequests) || this.numberOfRequestsToRejectionRates.get(lastNumberOfRequests) > lastRejectionProbability) {
            this.numberOfRequestsToRejectionRates.put(lastNumberOfRequests, lastRejectionProbability);
        }


        double deltaRequests = (this.lastNumberOfRequests.get(0) - this.lastNumberOfRequests.get(this.lastNumberOfRequests.size()-1)) / (double) (this.lastNumberOfRequests.get(0));
        double deltaRejections = this.lastRejectionRates.get(0) - this.lastRejectionRates.get(this.lastRejectionRates.size()-1);

        this.rejectionRateDeltaHistory.put(event.getIteration(), deltaRejections);
        this.requestsNumberDeltaHistory.put(event.getIteration(), deltaRequests);

        if(deltaRequests > MIN_REQUESTS_NUMBER_DELTA && deltaRejections < MIN_REJECTION_RATE_DELTA) {
            System.out.println("Detected useless increase of penalty at iteration " + event.getIteration());
            this.rejectionPenalty = this.lastPenalties.get(0);
        }

        if(!Double.isNaN(lastRejectionProbability) && TARGET_REJECTION_PROBABILITY < lastRejectionProbability && event.getIteration() >= ENABLE_AFTER_ITERATION){
            double delta = TARGET_REJECTION_PROBABILITY - lastRejectionProbability;
            double update = delta * REJECTIONS_PENALTY_ALPHA;
            this.rejectionPenalty += update;
        }

        if(!event.isLastIteration()){
            this.penaltiesHistory.put(event.getIteration()+1, this.rejectionPenalty);
        }


        try {
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.services.getControlerIO().getOutputFilename("drt_rejection_penalties.csv"))));
            writer.write("iteration;penalty;lastRejectionRate;deltaRequests;deltaRejection\n");
            for(Map.Entry<Integer, Double> entry: this.penaltiesHistory.entrySet()){
                writer.write(entry.getKey()+";"+entry.getValue()+";"+this.rejectionProbabilitiesHistory.get(entry.getKey())+";"+this.requestsNumberDeltaHistory.get(entry.getKey())+";"+this.rejectionRateDeltaHistory.get(entry.getKey())+"\n");
            }
            writer.close();
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.services.getControlerIO().getOutputFilename("requests_to_rejections.csv"))));
            writer.write("numberOfRequests;rejectionRate\n");
            List<Integer> keys = new ArrayList<>(this.numberOfRequestsToRejectionRates.keySet());
            for(Object key: keys.stream().sorted().toArray()) {
                Integer k = (Integer) key;
                writer.write(key+";"+this.numberOfRequestsToRejectionRates.get(k)+"\n");
            }
            writer.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


    public double getRejectionPenalty() {
        return this.rejectionPenalty;
    }
}
