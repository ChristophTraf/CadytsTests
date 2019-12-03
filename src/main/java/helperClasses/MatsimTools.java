package helperClasses;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import java.util.HashSet;
import java.util.Set;

public class MatsimTools {

    public Set<Id<Link>> getRouteLinksFromTransitSchedule(TransitSchedule schedule) {

        Set<Id<Link>> linkIdSet = new HashSet<>();

        for (TransitLine transitLine : schedule.getTransitLines().values()) {
            for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
                for (Id<Link> linkId : transitRoute.getRoute().getLinkIds()) {
                    linkIdSet.add(linkId);
                }
                linkIdSet.add(transitRoute.getRoute().getStartLinkId());
                linkIdSet.add(transitRoute.getRoute().getEndLinkId());
            }

        }
        return linkIdSet;
    }
}
