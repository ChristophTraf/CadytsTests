package run;

import ch.sbb.matsim.mobsim.qsim.SBBTransitModule;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.HashSet;
import java.util.Set;

public class RailRaptor {
    public static void main(String[] args) {

        String configFilename = args[0];
        Config config = ConfigUtils.loadConfig(configFilename, new ConfigGroup[0]);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new AbstractModule() {
            public void install() {
                this.install(new SBBTransitModule());
                this.install(new SwissRailRaptorModule());
            }
        });

        // ToDo: adjust these settings
        config.strategy().addParam("Module_1", "ChangeExpBeta");
        config.strategy().addParam("ModuleProbability_1", "0.7");
        config.strategy().addParam("Module_2", "ReRoute");
        config.strategy().addParam("ModuleProbability_2", "0.1");
        config.strategy().addParam("Module_3", "ChangeTripMode");
        config.strategy().addParam("ModuleProbability_3", "0.1");
        config.strategy().addParam("Module_4", "TimeAllocationMutator");
        config.strategy().addParam("ModuleProbability_4", "0.1");

        // network cleaner
        MultimodalNetworkCleaner mmNetCl = new MultimodalNetworkCleaner(scenario.getNetwork());
        Set<String> modes = new HashSet<String>();
        modes.add("car");
        mmNetCl.run(modes);
        mmNetCl.removeNodesWithoutLinks();
        controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        controler.run();

    }
}
