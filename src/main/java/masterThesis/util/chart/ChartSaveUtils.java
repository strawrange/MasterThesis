package masterThesis.util.chart;

import org.freehep.graphicsio.emf.EMFGraphics2D;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;

import java.awt.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author michalm
 */
public class ChartSaveUtils {
	public static void saveAsPNG(JFreeChart chart, String filename, int width, int height) {
		try {
			ChartUtilities.writeChartAsPNG(new FileOutputStream(filename + ".png"), chart, width, height);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void saveAsEMF(JFreeChart chart, String filename, int width, int height) {
		try (OutputStream out = new FileOutputStream(filename + ".emf")) {
			EMFGraphics2D emf2d = new EMFGraphics2D(out, new Dimension(width, height));
			emf2d.startExport();
			chart.draw((Graphics2D)emf2d.create(), new Rectangle(width, height));
			emf2d.endExport();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
