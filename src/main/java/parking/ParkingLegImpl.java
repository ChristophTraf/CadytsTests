package parking;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.utils.misc.Time;
import org.matsim.utils.objectattributes.attributable.Attributes;

class ParkingLegImpl  implements Leg {

    private Route route = null;

    private double depTime = Time.UNDEFINED_TIME;
    private double travTime = Time.UNDEFINED_TIME;
    private String mode;

    private final Attributes attributes = new Attributes();

    /* deliberately package */  ParkingLegImpl(final String transportMode) {
        this.mode = transportMode;
    }

    @Override
    public final String getMode() {
        return this.mode;
    }

    @Override
    public final void setMode(String transportMode) {
        this.mode = transportMode;
    }

    @Override
    public final double getDepartureTime() {
        return this.depTime;
    }

    @Override
    public final void setDepartureTime(final double depTime) {
        this.depTime = depTime;
    }

    @Override
    public final double getTravelTime() {
        return this.travTime;
    }

    @Override
    public final void setTravelTime(final double travTime) {
        this.travTime = travTime;
    }

    @Override
    public Route getRoute() {
        return this.route;
    }

    @Override
    public final void setRoute(Route route) {
        this.route = route;
    }

    @Override
    public final String toString() {
        return "[mode=" + this.getMode() + "]" +
                "[depTime=" + Time.writeTime(this.getDepartureTime()) + "]" +
                "[travTime=" + Time.writeTime(this.getTravelTime()) + "]" +
                "[arrTime=" + Time.writeTime(this.getDepartureTime() + this.getTravelTime()) + "]" +
                "[route=" + this.route + "]";
    }

    @Override
    public Attributes getAttributes() {
        return attributes;
    }

    //	private boolean locked;
//
//		public void setLocked() {
//			this.locked = true ;
//		}
//		private void testForLocked() {
//			if ( this.locked ) {
//				throw new RuntimeException("too late to change this") ;
//			}
//		}

}

