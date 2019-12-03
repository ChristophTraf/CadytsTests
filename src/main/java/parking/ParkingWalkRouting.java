package parking;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.facilities.Facility;

import java.util.ArrayList;
import java.util.List;

public class ParkingWalkRouting implements RoutingModule {

		/**
		 * @param initialNodeProportion the proportion of "initial nodes" to pass to the routing algorithm.
		 * This allows some randomness in the choice of the initial nodes.
		 */
		@Inject Scenario scenario;
		
		/***************************************************************************/
		public ParkingWalkRouting() 
		/***************************************************************************/
		{
		}
		
		/***************************************************************************/
		public ParkingWalkRouting(Scenario scenario) 
		/***************************************************************************/
		{
			this.scenario = scenario;
		}



		/***************************************************************************/
		@Override
		public List<? extends PlanElement> calcRoute(Facility fromFacility,
				Facility toFacility, double departureTime, Person person) 
		/***************************************************************************/
		{
			
			List<PlanElement> trip = new ArrayList <PlanElement>();
			
			Leg leg = new ParkingLegImpl("walk");
			leg.setDepartureTime(departureTime);
			leg.setMode("walk");
			Route route = new GenericRouteImpl(fromFacility.getLinkId(), toFacility.getLinkId());
			route.setDistance(500);
			route.setTravelTime(5*60);
			route.setStartLinkId(fromFacility.getLinkId());
			route.setEndLinkId(toFacility.getLinkId());
			route.setRouteDescription(fromFacility.getLinkId().toString());
			leg.setTravelTime(5*60);
			leg.setRoute(route);

			return trip;
			
		}

		/***************************************************************************/
		@Override
		public StageActivityTypes getStageActivityTypes() 
		/***************************************************************************/
		{
			return EmptyStageActivityTypes.INSTANCE;
		}
		
	}

