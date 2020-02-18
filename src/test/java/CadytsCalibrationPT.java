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
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.*;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import run.Analysis;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

public class CadytsCalibrationPT {


    @Test
    public final void testCalibrationAsScoringPTwithMIT() {

        final double beta=30.;

        String[] args = new String[3];
        args[0] = "input/Cadyts/config.xml";
        args[1] = "7";
        args[2] = "4";

        boolean useRailRaptor = true;
        boolean simplifyNetwork = false;
        boolean useParking = false;
        Set<String> simplifierIdgnoreModes = new HashSet<>();

        Config config = Analysis.getConfig(args);

        config.global().setNumberOfThreads( 1 );
        config.facilities().setFacilitiesSource( FacilitiesConfigGroup.FacilitiesSource.onePerActivityLinkInPlansFile );

        Scenario scenario = Analysis.getScenario(config, simplifyNetwork, simplifierIdgnoreModes);
        Controler controler = Analysis.getControler(config, scenario, useRailRaptor, useParking);

//        addCadytsParams(scenario);

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
                scoringFunctionAccumulator.addScoringFunction(scoringFunction);
                scoringFunctionAccumulator.addScoringFunction(scoringFunctionPT);

                return scoringFunctionAccumulator;
            }
        }) ;

        new ConfigWriter(controler.getConfig()).writeFileV2("output\\config.xml");
        Analysis.run(controler);
    }


    private void addCadytsParams(Scenario scenario) {
//        ConfigGroup cadytsPtConfig = scenario.getConfig().createModule(CadytsConfigGroup.GROUP_NAME );

        // create pt line string for pt calibration
        CadytsConfigGroup cadytsPtConfig = ConfigUtils.addOrGetModule( scenario.getConfig(), CadytsConfigGroup.class );
        StringBuilder calibrated_lines = new StringBuilder();
        for (TransitLine transitLine : scenario.getTransitSchedule().getTransitLines().values()) {
            if(transitLine.getName() != null ) {
                calibrated_lines.append(transitLine.getId());
                calibrated_lines.append(", ");
            }
        }
        calibrated_lines.delete(calibrated_lines.length() - 2, calibrated_lines.length());

        // create link id string for car calibration
        Counts mivCounts = new Counts();
        new MatsimCountsReader(mivCounts).readFile(scenario.getConfig().counts().getCountsFileName());
        StringBuilder calibrated_linkIds = new StringBuilder();
        for (Object key : mivCounts.getCounts().keySet()) {
            calibrated_linkIds.append(key);
            calibrated_linkIds.append(", ");
        }
        calibrated_linkIds.delete(calibrated_linkIds.length() - 2, calibrated_linkIds.length());

//        cadytsPtConfig.addParam(CadytsConfigGroup.START_TIME, "00:00:00");
        cadytsPtConfig.setStartTime( 0 );
//        cadytsPtConfig.addParam(CadytsConfigGroup.END_TIME, "30:00:00" );
        cadytsPtConfig.setEndTime( 30*3600 );
        cadytsPtConfig.addParam(CadytsConfigGroup.REGRESSION_INERTIA, "0.95");
        cadytsPtConfig.addParam(CadytsConfigGroup.USE_BRUTE_FORCE, "true");
        cadytsPtConfig.addParam(CadytsConfigGroup.MIN_FLOW_STDDEV, "8");
        cadytsPtConfig.addParam(CadytsConfigGroup.PREPARATORY_ITERATIONS, "1");
        cadytsPtConfig.addParam(CadytsConfigGroup.TIME_BIN_SIZE, "3600");
        cadytsPtConfig.addParam(CadytsConfigGroup.CALIBRATED_LINES, calibrated_lines.toString());
        cadytsPtConfig.addParam("calibratedLinks", calibrated_linkIds.toString());

        // note that the values set above will be "global", i.e. the same for cadyts4car.  Only the "calibratedLines" are now different; the corresponding
        // element is called "calibratedLinks" for cadyts4car.  kai, feb'20

//        CadytsConfigGroup ccc = new CadytsConfigGroup() ;
//        scenario.getConfig().addModule(ccc) ;

    }


}
