/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing;

import com.google.inject.Provider;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.remoteRebalancingMethods.RemoteRebalancingMethodParams;
import org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.remoteRebalancingMethods.RemoteRebalancingRequestBuilder;
import org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.remoteRebalancingMethods.costBasedQLearning.CostBasedQLearningModule;
import org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.remoteRebalancingMethods.costBasedQLearning.CostBasedQLearningParams;
import org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.remoteRebalancingMethods.simpleQLearning.SimpleQLearningModule;
import org.matsim.contrib.drt.optimizer.rebalancing.remoteRebalancing.remoteRebalancingMethods.simpleQLearning.SimpleQLearningParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.TerminationCriterion;
import org.matsim.core.replanning.ReplanningContext;


/**
 * @author michalm
 */
public class DrtModeRemoteRebalancingModule extends AbstractDvrpModeModule {

    private final DrtConfigGroup drtCfg;


    public DrtModeRemoteRebalancingModule(DrtConfigGroup drtCfg) {
        super(drtCfg.getMode());
        this.drtCfg = drtCfg;
    }


    @Override
    public void install() {
        RebalancingParams params = drtCfg.getRebalancingParams().orElseThrow();
        RemoteRebalancingParams strategyParams = (RemoteRebalancingParams) params.getRebalancingStrategyParams();
        RemoteRebalancingMethodParams remoteRebalancingMethodParams = strategyParams.getRemoteRebalancingMethodParams();

        //The order of these checks is important (EvQLearningParams, CostBasedQLearningParams, LexicographicQLearningParams all extend SimpleQLearningParams)
        if(remoteRebalancingMethodParams instanceof CostBasedQLearningParams) {
            install(new CostBasedQLearningModule(this.getMode()));
        } else if(remoteRebalancingMethodParams instanceof SimpleQLearningParams) {
            install(new SimpleQLearningModule(this.getMode()));
        }



        Provider<RemoteRebalancingConnectionManager> provider = modalProvider(instanceGetter -> new RemoteRebalancingConnectionManager(strategyParams, instanceGetter.get(MatsimServices.class), instanceGetter.get(TerminationCriterion.class), getConfig().controler().getFirstIteration()));

        bindModal(RemoteRebalancingConnectionManager.class).toProvider(provider).asEagerSingleton();
        addControlerListenerBinding().toProvider(modalProvider(getter -> getter.getModal(RemoteRebalancingConnectionManager.class)));

        installQSimModule(new AbstractDvrpModeQSimModule(this.getMode()) {
            @Override
            protected void configureQSim() {
                bindModal(RebalancingStrategy.class).toProvider(modalProvider(
                        getter -> {
                            RemoteRebalancingConnectionManager remoteRebalancingConnectionManager = getter.getModal(RemoteRebalancingConnectionManager.class);
                            RemoteRebalancingRequestBuilder requestBuilder = getter.getModal(RemoteRebalancingRequestBuilder.class);
                            Network network = getter.getModal(Network.class);
                            DrtZonalSystem zonalSystem = getter.getModal(DrtZonalSystem.class);
                            OutputDirectoryHierarchy outputDirectoryHierarchy = getter.get(OutputDirectoryHierarchy.class);
                            ReplanningContext replanningContext = getter.get(ReplanningContext.class);
                            return new RemoteRebalancingStrategy(remoteRebalancingConnectionManager, requestBuilder, network, zonalSystem, outputDirectoryHierarchy, replanningContext);
                        })).asEagerSingleton();
            }
        });
    }
}
