package masterThesis.drt.eventsrouting;

import com.google.inject.Inject;
import masterThesis.drt.eventsrouting.stopstoptime.StopStopTime;
import masterThesis.drt.eventsrouting.waitstoptime.WaitTime;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;

import java.util.HashMap;
import java.util.Map;

public class TransitRouterCommonDisutility implements AfterMobsimListener {
    private final Map<Id<Link>, double[]> linkTravelTimes = new HashMap<Id<Link>, double[]>();
    private final Map<Id<Link>, double[]> linkWaitingTimes = new HashMap<Id<Link>, double[]>();
    private int numSlots;
    private double timeSlot;
    private double startTime;
    private StopStopTime stopStopTime;
    private WaitTime waitTime;
    private TransitRouterNetworkWW routerNetwork;

    @Inject
    public TransitRouterCommonDisutility(StopStopTime stopStopTime, WaitTime waitTime, TransitRouterNetworkWW routerNetwork, TravelTimeCalculatorConfigGroup tTConfigGroup, QSimConfigGroup qSimConfigGroup) {
        startTime = qSimConfigGroup.getStartTime();
        double endTime = qSimConfigGroup.getEndTime();
        timeSlot = tTConfigGroup.getTraveltimeBinSize();
        numSlots = (int) ((endTime-startTime)/timeSlot);
        this.stopStopTime = stopStopTime;
        this.waitTime = waitTime;
        this.routerNetwork = routerNetwork;
        updateTimes();
    }

    private void updateTimes() {
        for(TransitRouterNetworkWW.TransitRouterNetworkLink link:routerNetwork.getLinks().values()) {
            if(!link.fromNode.getId().toString().endsWith("_W")) {
                double[] times = new double[numSlots];
                for(int slot = 0; slot<numSlots; slot++)
                    times[slot] = stopStopTime.getStopStopTime(link.fromNode.stop.getId(), link.toNode.stop.getId(), startTime+slot*timeSlot);
                linkTravelTimes.put(link.getId(), times);
            }
            else {
                double[] times = new double[numSlots];
                for(int slot = 0; slot<numSlots; slot++)
                    times[slot] = waitTime.getRouteStopWaitTime(link.fromNode.stop.getId(), startTime+slot*timeSlot);
                linkWaitingTimes.put(link.getId(), times);
            }
        }
    }

    double getTravelTime(Id<Link> linkId, double time) {
        return linkTravelTimes.get(linkId)[time/timeSlot<numSlots?(int)(time/timeSlot):(numSlots-1)];
    }

    double getWaitingTime(Id<Link> linkId, double time) {
        return linkWaitingTimes.get(linkId)[time/timeSlot<numSlots?(int)(time/timeSlot):(numSlots-1)];
    }

    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event) {
        updateTimes();
    }
}
