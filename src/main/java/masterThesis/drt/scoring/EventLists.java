package masterThesis.drt.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.population.Person;

import java.util.ArrayList;

public class EventLists {

    private ArrayList<Event> events = new ArrayList<>();
    private int count = 0;

    public ArrayList<Event> getEvents() {
        return events;
    }

    public int getCount() {
        return count;
    }

    public void add(Event event) {
        events.add(event);
        if (event instanceof PersonEntersVehicleEvent){
            count++;
        }
    }

    public int size(){
        return events.size();
    }

    public void clear() {
        events.clear();
    }

}
