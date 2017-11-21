package masterThesis.drt.analysis;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import masterThesis.drt.closerouting.DrtStageActivityType;
import masterThesis.drt.run.DrtConfigGroup;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

public class DrtStopEvaluator implements ActivityEndEventHandler, ActivityStartEventHandler {
    private Config config;
    private final Map<Id<TransitStopFacility>, TransitStopFacility> stops;
    private Map<Id<TransitStopFacility>, Integer> stopCounter = new HashMap<>();
    private Map<Id<TransitStopFacility>, ArrayList<Double>> stopArrivalTime = new HashMap<>();
    private Map<Id<TransitStopFacility>, ArrayList<Double>> stopEndTime = new HashMap<>();
    private int interval = 1;
            @Inject
    DrtStopEvaluator(Config config, EventsManager events, @Named(DrtConfigGroup.DRT_MODE) TransitSchedule transitSchedule){
        events.addHandler(this);
        this.stops = transitSchedule.getFacilities();
        this.config = config;
    }

    @Override
    public void handleEvent(ActivityEndEvent event) {
        if(event.getActType().equals(DrtStageActivityType.DRTSTAGEACTIVITY)){
            Id<TransitStopFacility> stopId = Id.create(event.getFacilityId(),TransitStopFacility.class);
            if (!stops.containsKey(stopId)){
                throw new RuntimeException("This stop"  + event.getFacilityId().toString() + " does register in the transit stops list, please check!");
            }
            if (stopEndTime.containsKey(stopId)){
                stopEndTime.get(stopId).add(event.getTime());
            }else{
                stopEndTime.put(stopId,new ArrayList<>(Arrays.asList(event.getTime())));
            }
        }

    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        if (event.getActType().equals(DrtStageActivityType.DRTSTAGEACTIVITY)){
            Id<TransitStopFacility> stopId = Id.create(event.getFacilityId(),TransitStopFacility.class);
            if (stopId == null){
                throw new RuntimeException("The passenger does not wait for drt in given stops");
            }
            if (!stops.containsKey(stopId)){
                throw new RuntimeException("This stop " + event.getFacilityId().toString() + " does register in the transit stops list, please check!");
            }
            if (event.getTime() == 0){
                double time = event.getTime();
            }
            int value = 0;
            if(stopCounter.containsKey(stopId)){
                value = stopCounter.get(stopId);
                stopArrivalTime.get(stopId).add(event.getTime());
            }
            else{
                stopArrivalTime.put(stopId,new ArrayList<>(Arrays.asList(event.getTime())));
            }
            value++;
            stopCounter.put(stopId,value);

        }
    }


    @Override
    public void reset(int iteration) {
        stopCounter.clear();
        stopArrivalTime.clear();
        stopEndTime.clear();
    }

    public void writeStopStatsFile(String fileNamePrefix) {
        String fileName = fileNamePrefix +  ".csv";
        BufferedWriter bw = IOUtils.getBufferedWriter(fileName);
        DecimalFormat format = new DecimalFormat();
        format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
        format.setMinimumIntegerDigits(1);
        format.setMaximumFractionDigits(2);
        if (stopCounter.size() < stopEndTime.size() || stopCounter.size() != stopArrivalTime.size()){
            throw new RuntimeException("StopCounter, stopEndTime and stopArrivalTime do not match each other!");
        }
        try {
            bw.write("stopId;count;");
            DescriptiveStatistics arrivalStats[] = new DescriptiveStatistics[stopCounter.size()];
            DescriptiveStatistics departStats[] = new DescriptiveStatistics[stopCounter.size()];
            int i = 0;
            for (Id<TransitStopFacility> stopId:stopCounter.keySet()) {
                if(stopArrivalTime.get(stopId).size() < stopEndTime.get(stopId).size()){
                    throw new RuntimeException("More departures than arrivals in " + stopId);
                }
                arrivalStats[i] = new DescriptiveStatistics();
                departStats[i] = new DescriptiveStatistics();
                ArrayList<Double> arrivals = stopArrivalTime.get(stopId);
                ArrayList<Double> departures = stopEndTime.get(stopId);
                int[] arrivalByTime = countStopsByTime(arrivals);
                int[] departureByTime = countStopsByTime(departures);
                if (i == 0) {
                    for (int q = 0; q < (int) (config.qsim().getEndTime() / 3600 / interval);q++) {
                        bw.write("arrival_" + q + ";");
                        bw.write("departure_" + q + ";");
                    }
                }
                bw.newLine();
                for (int q = 0; q < (int) (config.qsim().getEndTime() / 3600 / interval);q++) {
                    if (q == 0){
                        bw.write(stopId.toString() + ";" + stopCounter.get(stopId) +";");
                    }
                    bw.write(arrivalByTime[q] + ";");
                    bw.write(departureByTime[q] + ";");
                }
                i++;
            }
            bw.flush();
            bw.close();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    private int[] countStopsByTime(ArrayList<Double> count) {
        int[] countByTime =new int[(int) (config.qsim().getEndTime() / 3600 / interval)];
        for (int i = 0; i < count.size(); i++){
            for (int j = 0; j < countByTime.length; j++){
                if(count.get(i) >= j * interval * 3600 && count.get(i) < (j + 1) *interval * 3600){
                    countByTime[j]++;
                    break;
                }
            }
        }
        return countByTime;
    }
}
