import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.car.CadytsCarModule;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.CadytsPlanChanger;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.contrib.cadyts.pt.CadytsPtContext;
import org.matsim.contrib.cadyts.pt.CadytsPtModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.*;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import run.Analysis;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

public class CadytsCalibrationPT {

    @Test
    public final void testCalibrationAsScoring() {

        final double beta=30.;

        String[] args = new String[3];
        args[0] = "input\\config_chsc_austriaTest.xml";
        args[1] = "7";
        args[2] = "4";

        boolean useRailRaptor = true;
        boolean simplifyNetwork = false;
        boolean useParking = false;
        Set<String> simplifierIdgnoreModes = new HashSet<>();

        Config config = Analysis.getConfig(args);
        Scenario scenario = Analysis.getScenario(config, simplifyNetwork, simplifierIdgnoreModes);
        Controler controler = Analysis.getControler(config, scenario, useRailRaptor, useParking);

        addCadytsParams(scenario);

        controler.addOverridingModule(new CadytsPtModule());

        controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
            @Inject
            ScoringParametersForPerson parameters;
            @Inject
            Network network;
            @Inject
            CadytsPtContext cContext;
            @Override
            public ScoringFunction createNewScoringFunction(Person person) {
                final ScoringParameters params = parameters.getScoringParameters(person);

                SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
                scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, network));
                scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
                scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

                final CadytsScoring<TransitStopFacility> scoringFunction = new CadytsScoring<TransitStopFacility>(person.getSelectedPlan(), config, cContext);
                scoringFunction.setWeightOfCadytsCorrection(beta*30.) ;
                scoringFunctionAccumulator.addScoringFunction(scoringFunction);

                return scoringFunctionAccumulator;
            }
        }) ;

         Analysis.run(controler);
    }

    @Test
    public final void testCalibrationAsScoringPTwithMIT() {

        final double beta=30.;

        String[] args = new String[3];
        args[0] = "input\\config_chsc_austria.xml";
        args[1] = "7";
        args[2] = "4";

        boolean useRailRaptor = true;
        boolean simplifyNetwork = false;
        boolean useParking = false;
        Set<String> simplifierIdgnoreModes = new HashSet<>();

        Config config = Analysis.getConfig(args);
        Scenario scenario = Analysis.getScenario(config, simplifyNetwork, simplifierIdgnoreModes);
        Controler controler = Analysis.getControler(config, scenario, useRailRaptor, useParking);

        addCadytsParams(scenario);

        controler.addOverridingModule(new CadytsCarModule());
        controler.addOverridingModule(new CadytsPtModule());
        // include cadyts into the plan scoring (this will add the cadyts corrections to the scores):
        controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
            @Inject CadytsContext cadytsContext;
            @Inject ScoringParametersForPerson parameters;
            @Inject
            CadytsPtContext cContextPT;
            @Override
            public ScoringFunction createNewScoringFunction(Person person) {
                final ScoringParameters params = parameters.getScoringParameters(person);

                SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
                scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
                scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
                scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

                final CadytsScoring<Link> scoringFunction = new CadytsScoring<>(person.getSelectedPlan(), config, cadytsContext);
                final CadytsScoring<TransitStopFacility> scoringFunctionPT = new CadytsScoring<>(person.getSelectedPlan(), config, cContextPT);
                scoringFunction.setWeightOfCadytsCorrection(30. * config.planCalcScore().getBrainExpBeta()) ;
                scoringFunctionAccumulator.addScoringFunction(scoringFunction );

                return scoringFunctionAccumulator;
            }
        }) ;

        Analysis.run(controler);
    }

    @Test
    public final void testCalibration() {

        final double beta=30.;
        final int lastIteration = 20;

        String[] args = new String[3];
        args[0] = "input\\config_chsc.xml";
        args[1] = "7";
        args[2] = "4";

        boolean useRailRaptor = true;
        boolean simplifyNetwork = false;
        boolean useParking = false;
        Set<String> simplifierIdgnoreModes = new HashSet<>();

        Config config = Analysis.getConfig(args);
        config.controler().setWriteEventsInterval(5);
        config.controler().setLastIteration(lastIteration);
        Scenario scenario = Analysis.getScenario(config, simplifyNetwork, simplifierIdgnoreModes);
        Controler controler = Analysis.getControler(config, scenario, useRailRaptor, useParking);

        StrategyConfigGroup.StrategySettings stratSets = new StrategyConfigGroup.StrategySettings();
        stratSets.setStrategyName("ccc") ;
        stratSets.setWeight(1.);
        config.strategy().addStrategySettings(stratSets) ;

        controler.getConfig().controler().setCreateGraphs(false);
        controler.getConfig().controler().setDumpDataAtEnd(true);

        controler.addOverridingModule(new CadytsPtModule());
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addPlanStrategyBinding("ccc").toProvider(new javax.inject.Provider<PlanStrategy>() {
                    @Inject CadytsPtContext context;
                    @Override
                    public PlanStrategy get() {
                        final CadytsPlanChanger<TransitStopFacility> planSelector = new CadytsPlanChanger<TransitStopFacility>(scenario, context);
                        planSelector.setCadytsWeight(beta * 30.);
                        return new PlanStrategyImpl(planSelector);
                    }
                });
            }
        });

        controler.run();

    }

    private void addCadytsParams(Scenario scenario) {
        ConfigGroup cadytsPtConfig = scenario.getConfig().createModule(CadytsConfigGroup.GROUP_NAME );

        StringBuilder calibrated_lines = new StringBuilder();
        for (TransitLine transitLine : scenario.getTransitSchedule().getTransitLines().values()) {
            if(transitLine.getName() != null ) {
                calibrated_lines.append(transitLine.getId());
                calibrated_lines.append(", ");
            }
        }
        calibrated_lines.delete(calibrated_lines.length() - 2, calibrated_lines.length());

        cadytsPtConfig.addParam(CadytsConfigGroup.START_TIME, "00:00:00");
        cadytsPtConfig.addParam(CadytsConfigGroup.END_TIME, "30:00:00" );
        cadytsPtConfig.addParam(CadytsConfigGroup.REGRESSION_INERTIA, "0.95");
        cadytsPtConfig.addParam(CadytsConfigGroup.USE_BRUTE_FORCE, "true");
        cadytsPtConfig.addParam(CadytsConfigGroup.MIN_FLOW_STDDEV, "8");
        cadytsPtConfig.addParam(CadytsConfigGroup.PREPARATORY_ITERATIONS, "1");
        cadytsPtConfig.addParam(CadytsConfigGroup.TIME_BIN_SIZE, "3600");
        cadytsPtConfig.addParam(CadytsConfigGroup.CALIBRATED_LINES, calibrated_lines.toString());

        CadytsConfigGroup ccc = new CadytsConfigGroup() ;
        scenario.getConfig().addModule(ccc) ;

    }


}
