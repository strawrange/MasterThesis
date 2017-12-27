package masterThesis.dvrp.data;

import masterThesis.drt.data.DrtRequest;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.data.Requests;

import java.util.Comparator;

public class DrtRequests extends Requests{
    public static final Comparator<Request> UPDATE_TIME_COMPARATOR = new Comparator<Request>() {
        public int compare(Request r1, Request r2) {
            if (r1 instanceof DrtRequest && r2 instanceof DrtRequest) {
                return Double.compare(((DrtRequest) r1).getUpdateTime(),((DrtRequest) r2).getUpdateTime());
            }else{
                throw new RuntimeException("Only Drt Request can use this comparator");
            }
        }
    };
}
