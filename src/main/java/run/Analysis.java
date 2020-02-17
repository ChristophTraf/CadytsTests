package run;

import ch.sbb.matsim.mobsim.qsim.SBBTransitModule;
import ch.sbb.matsim.mobsim.qsim.pt.SBBTransitEngineQSimModule;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import com.google.inject.Provides;
import helperClasses.MatsimTools;
import network.NetworkSimplifier2;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.components.StandardQSimComponentConfigurator;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.utils.TransitScheduleValidator;
import parking.ParkenRoutingModule;
import parking.ParkingWalkRouting;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class Analysis {



    public static void main(String[] args) {

        boolean useRailRaptor = args[4].equals("true");
        boolean simplifyNetwork = args[5].equals("true");
        boolean useParking = args[6].equals("true");

        Set<String> simplifierIdgnoreModes = new HashSet<>();
//        simplifierIdgnoreModes.add("bus");
//        simplifierIdgnoreModes.add("tram");
//        simplifierIdgnoreModes.add("subway");
//        simplifierIdgnoreModes.add("train");
//        simplifierIdgnoreModes.add("test");

        // Following methods are extracted so that a test can operate on them.
        Config config = getConfig(args);
        Scenario scenario = getScenario(config, simplifyNetwork, simplifierIdgnoreModes);
        Controler controler = getControler(config, scenario, useRailRaptor, useParking);
        run(controler);
    }

    // ----------------------------------- methods (tests can be applied) --------------------------------------- //

    public static Controler getControler(Config config, Scenario scenario, boolean useRailRaptor, boolean useparking) {
        Controler controler = new Controler(scenario);

        // install SwissRailRaptor
        if (useRailRaptor) {
            controler.addOverridingModule(new AbstractModule() {
                public void install() {
                    this.install(new SBBTransitModule());
                    this.install(new SwissRailRaptorModule());
                }

                @Provides
                QSimComponentsConfig provideQSimComponentsConfig() {
                    QSimComponentsConfig components = new QSimComponentsConfig();
                    (new StandardQSimComponentConfigurator(config)).configure(components);
//                    SBBTransitEngineQSimModule.configure(components);
                    return components;
                }
            });
        }

        // install parking
        if (useparking) {
            controler.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    this.addRoutingModuleBinding("car_p").to(ParkenRoutingModule.class);
                    this.addTravelTimeBinding("car_p").to(networkTravelTime());
                    this.addRoutingModuleBinding("parking_walk").to(ParkingWalkRouting.class);
                }
            });
        }
        return controler;
    }

    public static Scenario getScenario(final Config config, boolean simplifyNW, Set<String> simplifierIgnoreModes) {

        Scenario scenario = ScenarioUtils.loadScenario(config);

        // simplify network
        NetworkSimplifier2 nw_simpl = new NetworkSimplifier2();
        if (simplifyNW) {

            // extract links from transit routes
            MatsimTools msMatsimTools = new MatsimTools();
            Set<Id<Link>> ignorLinks = msMatsimTools.getRouteLinksFromTransitSchedule(scenario.getTransitSchedule());
//            nw_simpl.setIgnorLinks(ignorLinks);
            nw_simpl.setSimplifierIgnoreModes(simplifierIgnoreModes);

            // simplify links from network except links from transit routes
//            nw_simpl.setGenerateShortLinkIds(true);
            nw_simpl.run(scenario.getNetwork());
            new NetworkWriter(scenario.getNetwork()).write("input/simplifiedShort.xml");
        }

        // network cleaner
        MultimodalNetworkCleaner mmNetCl = new MultimodalNetworkCleaner(scenario.getNetwork());
        Set<String> modes = new HashSet<>();
        modes.add("car");
        mmNetCl.run(modes);
        mmNetCl.removeNodesWithoutLinks();

        new NetworkWriter(scenario.getNetwork()).write("input/simplifiedCleanedNetwork.xml.gz");

        TransitScheduleValidator.printResult(TransitScheduleValidator.validateAll(scenario.getTransitSchedule(),
                scenario.getNetwork()));

        return scenario;
    }

    public static Config getConfig(String[] args) {
        Config config = ConfigUtils.loadConfig(args[0]);
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy_HHMMSS");
        Date d = new Date();
        config.controler()
                .setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controler().setOutputDirectory("output/" + sdf.format(d));

        // try performance of fast Algorithms, these perform parallized routing
        config.controler().setRoutingAlgorithmType(ControlerConfigGroup.RoutingAlgorithmType.FastDijkstra);

        // set storage and flow capacity depending on population
//        int numberOfAgents = 10000;
//        String plans = "plans" + numberOfAgents + ".xml";
//        int popVienna = 2000000;
//        int percentagePopulation = numberOfAgents/popVienna;
//        double storageCapacityFactor = percentagePopulation/100;
//        double flowCapacityFactor = storageCapacityFactor != -1 ? Math.pow(storageCapacityFactor, 0.75) : -1;

//        config.qsim().setFlowCapFactor(flowCapacityFactor);
//        config.qsim().setStorageCapFactor(storageCapacityFactor);

        // set number of threads
        int threadCount_glob = Integer.parseInt(args[1]);
        int threadCount_qsim = Integer.parseInt(args[2]);
//        int threadCount_parallel = Integer.parseInt(args[3]);
        config.global().setNumberOfThreads(threadCount_glob);
        config.qsim().setNumberOfThreads(threadCount_qsim);
//        config.parallelEventHandling().setNumberOfThreads(threadCount_parallel);

        config.transit().setBoardingAcceptance(TransitConfigGroup.BoardingAcceptance.checkStopOnly);

        config.qsim().setUsingThreadpool(true);

        // ToDo: adjust these settings
//        config.strategy().addParam("Module_1", "ChangeExpBeta");
//        config.strategy().addParam("ModuleProbability_1", "0.1");
//        config.strategy().addParam("Module_2", "ReRoute");
//        config.strategy().addParam("ModuleProbability_2", "0.5");
//        config.strategy().addParam("Module_3", "ChangeTripMode");
//        config.strategy().addParam("ModuleProbability_3", "0.5");
//
//        config.strategy().addParam("Module_4", "SubtourModeChoice");
//        config.strategy().addParam("ModuleProbability_4", "0.3");
//        config.strategy().addParam("Module_5", "TimeAllocationMutator");
//        config.strategy().addParam("ModuleProbability_5", "0.5");
//        config.strategy().addParam("Module_4", "PlansCalcTransitRoute");
//        config.strategy().addParam("ModuleProbability_4", "0.1");

        return config;
    }

    public static void run(final Controler controler) {
        controler.run();
    }
}
