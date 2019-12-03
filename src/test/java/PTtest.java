import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import run.Analysis;

import java.util.HashSet;
import java.util.Set;

public class PTtest {

    @Test
    public final void testTransitSchedule() {
        String[] args = new String[3];
        args[0] = "input/scenario_Jacky_Test_pt_with_SBB.xml";
        args[1] = "7";
        args[2] = "4";

        boolean useRailRaptor = true;
        boolean simplifyNetwork = false;
        Set<String> simplifierIdgnoreModes = new HashSet<>();
        simplifierIdgnoreModes.add("bus");
        simplifierIdgnoreModes.add("tram");
        simplifierIdgnoreModes.add("subway");
        simplifierIdgnoreModes.add("train");
        boolean useParking = false;

        Config config = Analysis.getConfig(args);
        Scenario scenario = Analysis.getScenario(config, simplifyNetwork, simplifierIdgnoreModes);

        Controler controler = Analysis.getControler(config, scenario, useRailRaptor, useParking);
        Analysis.run(controler);
    }
}
