package parking;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.facilities.Facility;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

public class ParkenRoutingModule implements RoutingModule {
		private final StageActivityTypes stageTypes = EmptyStageActivityTypes.INSTANCE;
		
		public final RoutingModule carRouting;
		public final RoutingModule ptRouting;
		@Inject Scenario scenario;
		
		
		/***************************************************************************/
		@Inject
		public ParkenRoutingModule(
				final Scenario scenario,
				@Named( TransportMode.pt )
				final RoutingModule ptRouting,
				@Named (TransportMode.car )
				final RoutingModule carRouting)
		/***************************************************************************/
		{
			this(	ptRouting,
					carRouting);
		
			this.scenario = scenario; //TODO: RADIEN RICHTIG STELLEN
		}

		/***************************************************************************/
		public ParkenRoutingModule(
				final RoutingModule ptRouting,
				final RoutingModule carRouting) 
		/***************************************************************************/
		{
			this.ptRouting = ptRouting;
			this.carRouting = carRouting;
		}

		/***************************************************************************/
	    @Override             
		public List<PlanElement> calcRoute(
				Facility fromFacility,
				Facility toFacility,
				double departureTime,
				Person person) 
		/***************************************************************************/
		{
    	
			List<PlanElement> trip = new ArrayList<>();

            if (fromFacility.toString().contains("_parken"))
			{
				if (toFacility.toString().contains("_parken"))
				{
					//dummy walk - vor und nach des "car" Legs
					trip.addAll(createWalkSubtrip(fromFacility, fromFacility, departureTime, person));
					List<PlanElement> carLegs = createCarSubtrip(fromFacility, toFacility, departureTime+5*60, person, carRouting);
					trip.addAll(carLegs);
					Leg legCar = (Leg) carLegs.get(0);
					double duration = legCar.getTravelTime();
					trip.addAll(createWalkSubtrip(toFacility, toFacility, departureTime + 5*60 + duration, person));
				}
				else
				{
					//dummy walk - vor dem "car" Leg
					trip.addAll(createWalkSubtrip(fromFacility, fromFacility, departureTime, person));
					List<PlanElement> carLegs = createCarSubtrip(fromFacility, toFacility, departureTime+5*60, person, carRouting);
					trip.addAll(carLegs);
				}
			}
			else if (toFacility.toString().contains("_parken"))
			{
				//dummy walk - nach dem "car" Leg
				List<PlanElement> carLegs = createCarSubtrip(fromFacility, toFacility, departureTime, person, carRouting);
				trip.addAll(carLegs);
				Leg legCar = (Leg) carLegs.get(0);
				double duration = legCar.getTravelTime();
				trip.addAll(createWalkSubtrip(toFacility, toFacility, departureTime + duration, person));
			}
			
			else
			{
				List<PlanElement> carLegs = createCarSubtrip(fromFacility, toFacility, departureTime, person, carRouting);
				trip.addAll(carLegs);
			}
			return trip;
		}
	    
	    public List<PlanElement> createWalkSubtrip(
	    		final Facility fromFacility,
				final Facility toFacility,
				double departureTime,
				final Person person)
	    {
			List<PlanElement> trip = new ArrayList <PlanElement>();
			
			Leg leg = new ParkingLegImpl("walk");
			leg.setDepartureTime(departureTime);
			leg.setMode("walk");
			Route route = new GenericRouteImpl(fromFacility.getLinkId(), toFacility.getLinkId());
			route.setDistance(250);
			route.setTravelTime(4*60);
			route.setStartLinkId(fromFacility.getLinkId());
			route.setEndLinkId(toFacility.getLinkId());
			route.setRouteDescription(fromFacility.getLinkId().toString());
			leg.setTravelTime(4*60);
			leg.setRoute(route);
			trip.add(leg);
			return trip;
	    }
		
	    /***************************************************************************/
		public List<PlanElement> createCarSubtrip(
				final Facility fromFacility,
				final Facility toFacility,
				double departureTime,
				final Person person,
				RoutingModule routing) 
		/***************************************************************************/
		{
			final List<? extends PlanElement> trip =
				routing.calcRoute(
						fromFacility,
						toFacility,
						departureTime,
						person );

			return (List<PlanElement>) trip;
		}

		/***************************************************************************/
		@Override
		public StageActivityTypes getStageActivityTypes() 
		/***************************************************************************/
		{
			return stageTypes;
		}
		
	                      
	}
