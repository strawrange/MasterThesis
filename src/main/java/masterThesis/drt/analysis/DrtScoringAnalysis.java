package masterThesis.drt.analysis;

import masterThesis.drt.scoring.AVScoringFunction;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;

public class DrtScoringAnalysis implements ShutdownListener,IterationEndsListener {
    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        AVScoringFunction.closeIteration();
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        AVScoringFunction.closeBW();
    }
}
