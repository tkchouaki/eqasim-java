package org.eqasim.ile_de_france;

import com.google.common.io.Resources;
import org.apache.commons.io.FileUtils;
import org.eqasim.ile_de_france.drt.RunDrtSimulation;
import org.eqasim.ile_de_france.drt.utils.AdaptConfigForDrt;
import org.eqasim.ile_de_france.drt.utils.AdaptConfigForFeeder;
import org.eqasim.ile_de_france.drt.utils.CreateDrtVehicles;
import org.junit.Test;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.ConfigUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestCps {

    @Test
    public void testCpsSim() throws CommandLine.ConfigurationException, IOException {
        URL configPath = Resources.getResource("cps/config.xml");
        String outputDirectory = ConfigUtils.loadConfig(configPath).controler().getOutputDirectory();
        RunSimulation.main(new String[]{"--config-path="+configPath.toString(), "--config:controler.firstIteration=0", "--config:controler.lastIteration=1"});
        FileUtils.deleteDirectory(new File(outputDirectory));
    }

    @Test
    public void testCpsDrt() throws URISyntaxException, IOException, CommandLine.ConfigurationException {
        URL configPath = Resources.getResource("cps/config.xml");
        Path initialConfigDir = Path.of(configPath.toURI()).getParent();
        Path drtConfigDir = Path.of(initialConfigDir.getFileName().toString()+"_drt");
        FileUtils.copyDirectory(new File(initialConfigDir.toAbsolutePath().toString()), new File(drtConfigDir.toAbsolutePath().toString()));
        Path networkPath = drtConfigDir.resolve("network.xml.gz");
        Path drtVehiclesPath = drtConfigDir.resolve("drt_vehicles.xml.gz");
        CreateDrtVehicles.main(new String[]{"--network-path="+networkPath.toAbsolutePath(), "--output-vehicles-path="+drtVehiclesPath.toAbsolutePath(), "--vehicles-number="+100});
        Path drtConfigPath = drtConfigDir.resolve("config.xml");
        AdaptConfigForDrt.main(new String[]{"--input-config-path="+drtConfigPath.toAbsolutePath(), "--output-config-path="+drtConfigPath.toAbsolutePath(), "--vehicles-path="+drtVehiclesPath.toAbsolutePath()});
        RunDrtSimulation.main(new String[]{"--config-path="+drtConfigPath.toAbsolutePath(), "--config:global.numberOfThreads=1", "--config:controler.lastIteration=1"});
        FileUtils.deleteDirectory(new File(drtConfigDir.toAbsolutePath().toString()));
        FileUtils.deleteDirectory(new File(drtConfigDir.resolveSibling("simulation_output").toAbsolutePath().toString()));
    }

    @Test
    public void testCpsFeeder() throws URISyntaxException, IOException, CommandLine.ConfigurationException {
        URL configPath = Resources.getResource("cps/config.xml");
        Path initialConfigDir = Path.of(configPath.toURI()).getParent();
        Path feederConfigDir = Path.of(initialConfigDir.getFileName().toString()+"_feeder");
        FileUtils.copyDirectory(new File(initialConfigDir.toAbsolutePath().toString()), new File(feederConfigDir.toAbsolutePath().toString()));
        Path networkPath = feederConfigDir.resolve("network.xml.gz");
        Path drtVehiclesPath = feederConfigDir.resolve("drt_vehicles.xml.gz");
        CreateDrtVehicles.main(new String[]{"--network-path="+networkPath.toAbsolutePath(), "--output-vehicles-path="+drtVehiclesPath.toAbsolutePath(), "--vehicles-number="+100});
        Path drtConfigPath = feederConfigDir.resolve("config.xml");
        AdaptConfigForFeeder.main(new String[]{"--input-config-path="+drtConfigPath.toAbsolutePath(), "--output-config-path="+drtConfigPath.toAbsolutePath(), "--vehicles-path="+drtVehiclesPath.toAbsolutePath()});
        RunDrtSimulation.main(new String[]{"--config-path="+drtConfigPath.toAbsolutePath(), "--config:global.numberOfThreads=1", "--config:controler.lastIteration=10"});
        FileUtils.deleteDirectory(new File(feederConfigDir.toAbsolutePath().toString()));
        FileUtils.deleteDirectory(new File(feederConfigDir.resolveSibling("simulation_output").toAbsolutePath().toString()));
    }
}
