package parking;/* *********************************************************************** *
 * project: org.matsim.*
 * RunZurichBikeSharingSimulation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.router.TripRouterModule;
import org.matsim.core.utils.io.UncheckedIOException;

import java.io.File;


/**
 * @author thibautd, overworked by hebenstreit
 */
public class RunConfigurableParkingRouter {

	/***************************************************************************/
	public static void main(final String... args) 
	/***************************************************************************/
	{
		final String configFile = args[ 0 ];
		//final String configFile = "E:/MATCHSIM_ECLIPSE/matsim-master/playgrounds/thibautd/examples\BikeRouting\haus\config.xml";
		//E:\MATCHSIM_ECLIPSE\matsim-master\playgrounds\thibautd\test\output\eu\eunoiaproject\bikesharing\framework\examples\TestRegressionConfigurableExample\testRunDoesNotFailMultimodal
		
		OutputDirectoryLogging.catchLogEntries();

		final Config config = ScenarioUtilsParking.loadConfig( configFile );

		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
//		failIfExists( config.controler().getOutputDirectory() );

		final Scenario sc = ScenarioUtilsParking.loadScenario( config );

		final Controler controler = new Controler( sc );
		installParking(controler, config) ;
		controler.addOverridingModule(new TripRouterModule());
		
		//////////////////////////////////////////////////////////

		//installBikes(controler);
		controler.run();

	}
	
public static void installParking(Controler controler, Config config) {
	
	controler.addOverridingModule(new AbstractModule() { 
			@Override
			 public void install() 
			 {
				//List<String> mainModeList = new ArrayList<>();
				//mainModeList.add("car_p");
				//mainModeList.add("car");
				//config.plansCalcRoute().setNetworkModes(mainModeList);
				//zu pr�fen was hier angebunden werden soll
				this.addRoutingModuleBinding("car_p").to(ParkenRoutingModule.class);
                this.addTravelTimeBinding("car_p").to(networkTravelTime());
                this.addRoutingModuleBinding("parking_walk").to(ParkingWalkRouting.class);
			 }  
		});
		
}

 


	/***************************************************************************/
	private static void failIfExists(final String outdir) 
	/***************************************************************************/
	{
		final File file = new File( outdir +"/" );
		if ( file.exists() && file.list().length > 0 ) {
new UncheckedIOException( "Directory "+outdir+" exists and is not empty!" );
		}
	}
}