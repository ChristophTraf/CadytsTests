/* *********************************************************************** *
 * project: org.matsim.*
 * CadytsIntegrationTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
 * *********************************************************************** */

import ch.sbb.matsim.mobsim.qsim.SBBTransitModule;
import ch.sbb.matsim.mobsim.qsim.pt.SBBTransitEngineQSimModule;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import com.google.inject.Provides;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup.MobsimType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.components.StandardQSimComponentConfigurator;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This is a modified copy of CadytsIntegrationTest (which is used for the cadyts pt integration)
 * in order to establish an according test for the cadyts car integration.
 * At this stage all original pt code is still included here, but outcommeted, to make the adaptations
 * from pt to car well traceable in case of any errors.
 */

public class TestScenarios {

//	@Rule
//	public MatsimTestUtils utils = new MatsimTestUtils();

    public final void testNormalSim() {

        String inputDir = "C:\\Users\\jaas\\WORKSPACE_IJ_Projects\\matsim-master\\matsim-master\\matsim\\test\\input\\scenarios\\Jacky\\";
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy_HHMMSS");
        Date d = new Date();

        String outputDir = "C:\\Users\\jaas\\WORKSPACE_IJ_Projects\\matsim-master\\matsim-master\\matsim\\test\\output\\Jacky\\test_normal\\";

        Config config = createTestConfigNormal(inputDir, outputDir);
        config.controler().setOutputDirectory(outputDir + sdf.format(d));

        double storageCapacityFactor = 1;
        double flowCapacityFactor = 1;

        config.qsim().setFlowCapFactor(flowCapacityFactor);
        config.qsim().setStorageCapFactor(storageCapacityFactor);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        Controler controler = new Controler(scenario);

        controler.run();
    }


    //--------------------------------------------------------------
    @Test
    public final void testPtSim() {

        boolean useRailRaptor = true;
        String inputDir = "D:\\Projekte_PV\\chsc\\IntelliJ_Projects\\autoWaves.git\\input\\test_pt\\";
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy_HHMMSS");
        Date d = new Date();

        String outputDir = "D:\\Projekte_PV\\chsc\\IntelliJ_Projects\\autoWaves.git\\output\\test_pt\\";

        final Config config = createTestConfigPt(inputDir, outputDir);
        config.controler().setOutputDirectory(outputDir + sdf.format(d));

        double storageCapacityFactor = 1;
        double flowCapacityFactor = 1;

        config.qsim().setFlowCapFactor(flowCapacityFactor);
        config.qsim().setStorageCapFactor(storageCapacityFactor);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        for (Vehicle vehicle : scenario.getTransitVehicles().getVehicles().values()) {
            System.out.println(vehicle.getId());
        }

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
                    SBBTransitEngineQSimModule.configure(components);
                    return components;
                }
            });
        }
        controler.run();
    }


    //--------------------------------------------------------------

    //--------------------------------------------------------------
    @Test
    public final void testCordons() {

        String inputDir = "C:\\Users\\jaas\\WORKSPACE_IJ_Projects\\matsim-master\\matsim-master\\matsim\\test\\input\\scenarios\\Jacky\\";
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy_HHMMSS");
        Date d = new Date();

        String outputDir = "C:\\Users\\jaas\\WORKSPACE_IJ_Projects\\matsim-master\\matsim-master\\matsim\\test\\output\\Jacky\\test_cordon\\";

        Config config = createTestConfigCordon(inputDir, outputDir);
        config.controler().setOutputDirectory(outputDir + sdf.format(d));

        double storageCapacityFactor = 1;
        double flowCapacityFactor = 1;

        config.qsim().setFlowCapFactor(flowCapacityFactor);
        config.qsim().setStorageCapFactor(storageCapacityFactor);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        Controler controler = new Controler(scenario);

        controler.run();
    }


    //--------------------------------------------------------------

    private static Config createTestConfigNormal(String inputDir, String outputDir) {
        Config config = ConfigUtils.createConfig();
        config.global().setRandomSeed(4711);
        config.network().setInputFile(inputDir + "network_test_sp.xml");
        config.plans().setInputFile(inputDir + "plans_test_sp.xml");
        config.controler().setFirstIteration(1);
        config.controler().setLastIteration(10);
        config.controler().setOutputDirectory(outputDir);
        config.controler().setWriteEventsInterval(1);
        config.controler().setMobsim(MobsimType.qsim.toString());
        config.qsim().setFlowCapFactor(1.);
        config.qsim().setStorageCapFactor(1.);
        config.qsim().setStuckTime(10.);

        config.strategy().addParam("Module_1", "ChangeExpBeta");
        config.strategy().addParam("ModuleProbability_1", "0.7");
        config.strategy().addParam("Module_2", "ReRoute");
        config.strategy().addParam("ModuleProbability_2", "0.1");
        config.strategy().addParam("Module_3", "ChangeTripMode");
        config.strategy().addParam("ModuleProbability_3", "0.1");
        config.strategy().addParam("Module_3", "SubtourModeChoice");
        config.strategy().addParam("ModuleProbability_3", "0.1");

        config.qsim().setRemoveStuckVehicles(false);
        {
            ActivityParams params = new ActivityParams("h");
            config.planCalcScore().addActivityParams(params);
            params.setTypicalDuration(12 * 60 * 60.);
        }
        {
            ActivityParams params = new ActivityParams("w");
            config.planCalcScore().addActivityParams(params);
            params.setTypicalDuration(8 * 60 * 60.);
        }
        config.counts().setInputFile(inputDir + "counts_test_sp.xml");
        return config;
    }


    //--------------------------------------------------------------


    private static Config createTestConfigPt(String inputDir, String outputDir) {
        Config config = ConfigUtils.createConfig();
        config.global().setRandomSeed(4711);
        config.network().setInputFile(inputDir + "network_pt.xml");
        config.plans().setInputFile(inputDir + "plans_test_sp.xml");
        config.transit().setUseTransit(true);
        config.transit().setTransitScheduleFile(inputDir + "output_transitSchedule.xml");
        config.transit().setVehiclesFile(inputDir + "output_transitVehicles.xml");
        config.controler().setFirstIteration(1);
        config.controler().setLastIteration(10);
        config.controler().setOutputDirectory(outputDir);
        config.controler().setWriteEventsInterval(1);
        config.controler().setMobsim(MobsimType.qsim.toString());
        config.qsim().setFlowCapFactor(1.);
        config.qsim().setStorageCapFactor(1.);
        config.qsim().setStuckTime(10.);

        config.strategy().addParam("Module_1", "ChangeExpBeta");
        config.strategy().addParam("ModuleProbability_1", "1");
        config.strategy().addParam("Module_2", "ReRoute");
        config.strategy().addParam("ModuleProbability_2", "0");
        config.strategy().addParam("Module_3", "ChangeTripMode");
        config.strategy().addParam("ModuleProbability_3", "0");
        config.strategy().addParam("Module_3", "SubtourModeChoice");
        config.strategy().addParam("ModuleProbability_3", "0");

        config.qsim().setRemoveStuckVehicles(false);
        {
            ActivityParams params = new ActivityParams("h");
            config.planCalcScore().addActivityParams(params);
            params.setTypicalDuration(12 * 60 * 60.);
        }
        {
            ActivityParams params = new ActivityParams("w");
            config.planCalcScore().addActivityParams(params);
            params.setTypicalDuration(8 * 60 * 60.);
        }
        {
            ActivityParams params = new ActivityParams("s");
            config.planCalcScore().addActivityParams(params);
            params.setTypicalDuration(10 * 60.);
        }

        return config;
    }
    //--------------------------------------------------------------


    private static Config createTestConfigCordon(String inputDir, String outputDir) {
        Config config = ConfigUtils.createConfig();
        config.global().setRandomSeed(4711);
        config.network().setInputFile(inputDir + "network_test_sp_undefined_del_link.xml");
        config.plans().setInputFile(inputDir + "plans_test_cordons_isolated_undefined_del_links.xml");
        config.controler().setFirstIteration(1);
        config.controler().setLastIteration(10);
        config.controler().setOutputDirectory(outputDir);
        config.controler().setWriteEventsInterval(1);
        config.controler().setMobsim(MobsimType.qsim.toString());
        config.qsim().setFlowCapFactor(1.);
        config.qsim().setStorageCapFactor(1.);
        config.qsim().setStuckTime(10.);

        config.strategy().addParam("Module_1", "ChangeExpBeta");
        config.strategy().addParam("ModuleProbability_1", "0.7");
        config.strategy().addParam("Module_2", "ReRoute");
        config.strategy().addParam("ModuleProbability_2", "0.1");
        config.strategy().addParam("Module_3", "ChangeTripMode");
        config.strategy().addParam("ModuleProbability_3", "0.1");
        config.strategy().addParam("Module_3", "SubtourModeChoice");
        config.strategy().addParam("ModuleProbability_3", "0.1");

        {
            PlanCalcScoreConfigGroup.ModeParams params = new PlanCalcScoreConfigGroup.ModeParams("undefined");
            config.planCalcScore().addModeParams(params);
            params.setMarginalUtilityOfTraveling(0);
        }

        config.qsim().setRemoveStuckVehicles(false);
        {
            ActivityParams params = new ActivityParams("h");
            config.planCalcScore().addActivityParams(params);
            params.setTypicalDuration(12 * 60 * 60.);
        }
        {
            ActivityParams params = new ActivityParams("w");
            config.planCalcScore().addActivityParams(params);
            params.setTypicalDuration(8 * 60 * 60.);
        }
        {
            ActivityParams params = new ActivityParams("w_in");
            config.planCalcScore().addActivityParams(params);
            params.setTypicalDuration(8 * 60 * 60.);
        }
        {
            ActivityParams params = new ActivityParams("w_out");
            config.planCalcScore().addActivityParams(params);
            params.setTypicalDuration(8 * 60 * 60.);
        }

        return config;
    }


    //--------------------------------------------------------------


    private static class DummyMobsim implements Mobsim {
        public DummyMobsim() {
        }

        public void run() {
        }
    }

}
