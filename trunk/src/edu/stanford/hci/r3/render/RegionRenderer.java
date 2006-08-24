package edu.stanford.hci.r3.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

import edu.stanford.hci.r3.core.Region;
import edu.stanford.hci.r3.units.Points;
import edu.stanford.hci.r3.units.Units;

/**
 * <p>
 * This software is distributed under the <a href="http://hci.stanford.edu/research/copyright.txt">
 * BSD License</a>.
 * </p>
 * 
 * @author <a href="http://graphics.stanford.edu/~ronyeh">Ron B Yeh</a> (ronyeh(AT)cs.stanford.edu)
 */
public class RegionRenderer {

	private static final String CONFIG_FILE = "config/RegionRenderer.xml";

	public static final boolean DEBUG_REGIONS = readDebugFlagFromConfigFile();

	/**
	 * The font for printing the object during screen-based debugging.
	 */
	private static final Font FONT = new Font("Trebuchet MS", Font.PLAIN, 8);

	/**
	 * The key as stored in the xml config file.
	 */
	private static final String PROPERTY_NAME = "debugRegions";

	private static final Color REGION_COLOR = new Color(123, 123, 123, 30);

	private static final Color TEXT_COLOR = Color.BLACK;

	private static boolean readDebugFlagFromConfigFile() {
		final Properties props = new Properties();
		try {
			props.loadFromXML(new FileInputStream(CONFIG_FILE));
		} catch (InvalidPropertiesFormatException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		final String property = props.getProperty(PROPERTY_NAME);
		final boolean debug = Boolean.parseBoolean(property);
		return debug;
	}

	protected Region region;

	/**
	 * 
	 */
	public RegionRenderer(Region r) {
		region = r;
	}

	private static final BasicStroke OUTLINE = new BasicStroke(1);

	/**
	 * @param g2d
	 *            Draw some boxes to the Graphics context to show where the regions lie.
	 */
	public void renderToG2D(Graphics2D g2d) {
		g2d.setFont(FONT);

		final Rectangle2D b = region.getUnscaledBounds2D();

		final float scaleX = (float) region.getScaleX();
		final float scaleY = (float) region.getScaleY();

		final Units units = region.getUnits();
		final double conv = units.getConversionTo(Points.ONE);

		final float xPts = (float) Math.round(conv * b.getX());
		final float yPts = (float) Math.round(conv * b.getY());
		final float wPts = (float) Math.round(conv * b.getWidth());
		final float hPts = (float) Math.round(conv * b.getHeight());

		// x and y origins are NOT affected by the horiz/vert scaling factors
		final int finalX = (int) Math.round(xPts);
		final int finalY = (int) Math.round(yPts);
		final int finalW = (int) Math.round(wPts * scaleX);
		final int finalH = (int) Math.round(hPts * scaleY);

		// handle different regions differently
		g2d.setStroke(OUTLINE);
		g2d.setColor(Color.BLACK);
		g2d.drawRect(finalX, finalY, finalW, finalH);
		g2d.setColor(REGION_COLOR);
		g2d.fillRect(finalX, finalY, finalW, finalH);
		g2d.setColor(TEXT_COLOR);
		final float lineHeight = FONT.getLineMetrics(region.toString(),
				new FontRenderContext(null, true, true)).getHeight();
		g2d.drawString(region.toString(), finalX, finalY + lineHeight);
	}
}