import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.car.CadytsCarModule;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.*;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.*;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import run.Analysis;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

public class CadytsCalibration {

    @Test
    public final void testCalibrationPrivateTransp() {
        final double beta=30.;

        String[] args = new String[3];
        args[0] = "input/config_chsc_austria.xml";
        args[1] = "7";
        args[2] = "4";

        boolean useRailRaptor = true;
        boolean simplifyNetwork = false;
        Set<String> simplifierIdgnoreModes = new HashSet<>();
//        simplifierIdgnoreModes.add("bus");
//        simplifierIdgnoreModes.add("tram");
//        simplifierIdgnoreModes.add("subway");
//        simplifierIdgnoreModes.add("train");
        boolean useParking = false;

        Config config = Analysis.getConfig(args);
        Scenario scenario = Analysis.getScenario(config, simplifyNetwork, simplifierIdgnoreModes);
        Controler controler = Analysis.getControler(config, scenario, useRailRaptor, useParking);

        addCadytsParams(scenario);

        controler.addOverridingModule(new CadytsCarModule());
        // include cadyts into the plan scoring (this will add the cadyts corrections to the scores):
        controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
            @Inject CadytsContext cadytsContext;
            @Inject ScoringParametersForPerson parameters;
            @Override
            public ScoringFunction createNewScoringFunction(Person person) {
                final ScoringParameters params = parameters.getScoringParameters(person);

                SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
                scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
                scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
                scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

                final CadytsScoring<Link> scoringFunction = new CadytsScoring<>(person.getSelectedPlan(), config, cadytsContext);
                scoringFunction.setWeightOfCadytsCorrection(30. * config.planCalcScore().getBrainExpBeta()) ;
                scoringFunctionAccumulator.addScoringFunction(scoringFunction );

                return scoringFunctionAccumulator;
            }
        }) ;

    Analysis.run(controler);
    }


    private void addCadytsParams(Scenario scenario) {
        ConfigGroup cadytsPtConfig = scenario.getConfig().createModule(CadytsConfigGroup.GROUP_NAME );

        Counts mivCounts = new Counts();
        new MatsimCountsReader(mivCounts).readFile("input/" + scenario.getConfig().counts().getCountsFileName());

        StringBuilder calibrated_linkIds = new StringBuilder();
        for (Object key : mivCounts.getCounts().keySet()) {
            calibrated_linkIds.append(key);
            calibrated_linkIds.append(", ");
        }
        calibrated_linkIds.delete(calibrated_linkIds.length() - 2, calibrated_linkIds.length());

        cadytsPtConfig.addParam(CadytsConfigGroup.START_TIME, "00:00:00");
        cadytsPtConfig.addParam(CadytsConfigGroup.END_TIME, "30:00:00" );
        cadytsPtConfig.addParam(CadytsConfigGroup.REGRESSION_INERTIA, "0.95");
        cadytsPtConfig.addParam(CadytsConfigGroup.USE_BRUTE_FORCE, "true");
        cadytsPtConfig.addParam(CadytsConfigGroup.MIN_FLOW_STDDEV, "8");
        cadytsPtConfig.addParam(CadytsConfigGroup.PREPARATORY_ITERATIONS, "1");
        cadytsPtConfig.addParam(CadytsConfigGroup.TIME_BIN_SIZE, "3600");
//        cadytsPtConfig.addParam(CadytsConfigGroup.CALIBRATED_LINES, calibrated_linkIds.toString());

        CadytsConfigGroup ccc = new CadytsConfigGroup() ;
        scenario.getConfig().addModule(ccc) ;

    }
}
