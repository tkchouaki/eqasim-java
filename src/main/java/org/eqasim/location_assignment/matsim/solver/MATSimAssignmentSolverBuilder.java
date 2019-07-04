package org.eqasim.location_assignment.matsim.solver;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.eqasim.location_assignment.algorithms.DiscretizerSolver;
import org.eqasim.location_assignment.algorithms.GravityAngularLocationSolver;
import org.eqasim.location_assignment.algorithms.SamplingFeasibleDistanceSolver;
import org.eqasim.location_assignment.algorithms.ThresholdObjectiveFunction;
import org.eqasim.location_assignment.algorithms.angular.AngularTailSolver;
import org.eqasim.location_assignment.algorithms.gravity.GravityChainSolver;
import org.eqasim.location_assignment.algorithms.gravity.initial.GravityInitialLocationGenerator;
import org.eqasim.location_assignment.algorithms.gravity.initial.LateralDeviationGenerator;
import org.eqasim.location_assignment.assignment.LocationAssignmentSolver;
import org.eqasim.location_assignment.assignment.LocationAssignmentSolverBuilder;
import org.eqasim.location_assignment.assignment.discretization.DiscreteLocationSolver;
import org.eqasim.location_assignment.assignment.distance.FeasibleDistanceSolver;
import org.eqasim.location_assignment.assignment.objective.LocationAssignmentObjectiveFunction;
import org.eqasim.location_assignment.assignment.relaxation.RelaxedLocationSolver;
import org.eqasim.location_assignment.matsim.setup.MATSimDiscretizationThresholdProvider;
import org.eqasim.location_assignment.matsim.setup.MATSimDiscretizerProvider;
import org.eqasim.location_assignment.matsim.setup.MATSimDistanceSamplerProvider;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;

public class MATSimAssignmentSolverBuilder {
	private LocationAssignmentObjectiveFunction objectiveFunction = null;
	private FeasibleDistanceSolver feasibleDistanceSolver = null;
	private RelaxedLocationSolver relaxedLocationSolver = null;
	private DiscreteLocationSolver discreteLocationSolver = null;

	private MATSimDiscretizationThresholdProvider discretizationThresholdProvider;
	private MATSimDistanceSamplerProvider distanceSamplerProvider;
	private MATSimDiscretizerProvider discretizerProvider = null;

	private int maximumDiscretizationIterations = 1000;
	private int maximumGravityIterations = 1000;
	private int maximumFeasibleDistanceSamples = 1000;

	private boolean useIterativeFeasibleSolutions = true;

	private double lateralDeviationStd = 10.0;
	private double gravityConvergenceThreshold = 10.0;
	private double gravityGain = 0.1;

	private Random random = new Random();

	private Set<String> variableActivityTypes = new HashSet<>();
	private StageActivityTypes stageActivityTypes = new StageActivityTypesImpl();

	public MATSimAssignmentSolver build() {
		if (relaxedLocationSolver == null) {
			GravityInitialLocationGenerator initialLocationGenerator = new LateralDeviationGenerator(random,
					lateralDeviationStd);
			GravityChainSolver gravityChainSolver = new GravityChainSolver(gravityGain, maximumGravityIterations,
					gravityConvergenceThreshold, random, initialLocationGenerator);

			AngularTailSolver angularTailSolver = new AngularTailSolver(random);

			relaxedLocationSolver = new GravityAngularLocationSolver(gravityChainSolver, angularTailSolver);
		}

		if (feasibleDistanceSolver == null) {
			if (distanceSamplerProvider == null) {
				throw new IllegalStateException("MATSimDistanceSamplerProvider must be specified");
			}

			feasibleDistanceSolver = new SamplingFeasibleDistanceSolver(maximumFeasibleDistanceSamples,
					new MATSimDistanceSamplerProvider.Adapter(distanceSamplerProvider));
		}

		if (discreteLocationSolver == null) {
			if (discretizerProvider == null) {
				throw new IllegalStateException("MATSimDiscretizerProvider must be specified");
			}

			discreteLocationSolver = new DiscretizerSolver(new MATSimDiscretizerProvider.Adapter(discretizerProvider));
		}

		if (objectiveFunction == null) {
			if (discretizationThresholdProvider == null) {
				throw new IllegalStateException("MATSimDiscretizationThresholdProvider must be specified");
			}

			objectiveFunction = new ThresholdObjectiveFunction(
					new MATSimDiscretizationThresholdProvider.Adapter(discretizationThresholdProvider));
		}

		LocationAssignmentSolverBuilder locationAssignmentSolverBuilder = new LocationAssignmentSolverBuilder();
		locationAssignmentSolverBuilder.setMaximumDiscretizationIterations(maximumDiscretizationIterations);
		locationAssignmentSolverBuilder.setUseIterativeFeasibleSolutions(useIterativeFeasibleSolutions);
		locationAssignmentSolverBuilder.setDiscreteLocationSolver(discreteLocationSolver);
		locationAssignmentSolverBuilder.setLocationAssignmentObjectiveFunction(objectiveFunction);
		locationAssignmentSolverBuilder.setFeasibleDistanceSolver(feasibleDistanceSolver);
		locationAssignmentSolverBuilder.setRelaxedLocationSolver(relaxedLocationSolver);
		LocationAssignmentSolver solver = locationAssignmentSolverBuilder.build();

		return new MATSimAssignmentSolver(solver, variableActivityTypes, stageActivityTypes);
	}

	public void setObjectiveFunction(LocationAssignmentObjectiveFunction objectiveFunction) {
		this.objectiveFunction = objectiveFunction;
	}

	public void setFeasibleDistanceSolver(FeasibleDistanceSolver feasibleDistanceSolver) {
		this.feasibleDistanceSolver = feasibleDistanceSolver;
	}

	public void setRelaxedLocationSolver(RelaxedLocationSolver relaxedLocationSolver) {
		this.relaxedLocationSolver = relaxedLocationSolver;
	}

	public void setDiscreteLocationSolver(DiscreteLocationSolver discreteLocationSolver) {
		this.discreteLocationSolver = discreteLocationSolver;
	}

	public void setDiscretizationThresholdProvider(
			MATSimDiscretizationThresholdProvider discretizationThresholdProvider) {
		this.discretizationThresholdProvider = discretizationThresholdProvider;
	}

	public void setDistanceSamplerProvider(MATSimDistanceSamplerProvider distanceSamplerProvider) {
		this.distanceSamplerProvider = distanceSamplerProvider;
	}

	public void setDiscretizerProvider(MATSimDiscretizerProvider discretizerProvider) {
		this.discretizerProvider = discretizerProvider;
	}

	public void setMaximumDiscretizationIterations(int maximumDiscretizationIterations) {
		this.maximumDiscretizationIterations = maximumDiscretizationIterations;
	}

	public void setMaximumFeasibleDistanceSamples(int maximumFeasibleDistanceSamples) {
		this.maximumFeasibleDistanceSamples = maximumFeasibleDistanceSamples;
	}

	public void setMaximumGravityIterations(int maximumGravityIterations) {
		this.maximumGravityIterations = maximumGravityIterations;
	}

	public void setUseIterativeFeasibleSolutions(boolean useIterativeFeasibleSolutions) {
		this.useIterativeFeasibleSolutions = useIterativeFeasibleSolutions;
	}

	public void setLateralDeviationStd(double lateralDeviationStd) {
		this.lateralDeviationStd = lateralDeviationStd;
	}

	public void setGravityConvergenceThreshold(double gravityConvergenceThreshold) {
		this.gravityConvergenceThreshold = gravityConvergenceThreshold;
	}

	public void setGravityGain(double gravityGain) {
		this.gravityGain = gravityGain;
	}

	public void setRandom(Random random) {
		this.random = random;
	}

	public void setVariableActivityTypes(Set<String> variableActivityTypes) {
		this.variableActivityTypes = variableActivityTypes;
	}

	public void setStageActivityTypes(StageActivityTypes stageActivityTypes) {
		this.stageActivityTypes = stageActivityTypes;
	}
}
