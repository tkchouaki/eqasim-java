package org.eqasim.ile_de_france.drt.utils;

import org.apache.commons.io.FileUtils;
import org.eqasim.ile_de_france.drt.RunDrtSimulation;
import org.matsim.core.config.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class QueuedDrtSimulations {

    public static void main(String[] args) throws CommandLine.ConfigurationException, IOException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("pending-simulations-dir", "running-simulations-dir", "config-file-name", "output-dir")
                .build();

        Path pendingSimulationsDirPath = Path.of(cmd.getOptionStrict("pending-simulations-dir"));
        Path runningSimulationsDirPath = Path.of(cmd.getOptionStrict("running-simulations-dir"));
        Path outputDirPath = Path.of(cmd.getOptionStrict("output-dir"));


        File pendingSimulationDir = new File(pendingSimulationsDirPath.toAbsolutePath().toString());
        File[] pendingSimulations = pendingSimulationDir.listFiles();

        if(pendingSimulations.length == 0) {
            return;
        }

        File simulationDir = pendingSimulations[0];
        FileUtils.moveDirectoryToDirectory(simulationDir, new File(runningSimulationsDirPath.toAbsolutePath().toString()), false);

        Path configPath = runningSimulationsDirPath.resolve(simulationDir.getName()).resolve(cmd.getOptionStrict("config-file-name"));
        Path outputPath = outputDirPath.resolve(simulationDir.getName());

        List<String> arguments = new ArrayList<>();
        arguments.add("--config-path="+configPath.toAbsolutePath());
        arguments.add("--config:controler.outputDirectory="+outputPath.toAbsolutePath());
        for(String option: cmd.getAvailableOptions())
        {
            if(option.startsWith("config:"))
            {
                String value = cmd.getOptionStrict(option);
                arguments.add("--"+option+"="+value);
            }
        }

        RunDrtSimulation.main(arguments.toArray(new String[0]));
    }
}
