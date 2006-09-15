package edu.stanford.hci.r3.paper;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import edu.stanford.hci.r3.events.ContentFilter;
import edu.stanford.hci.r3.events.EventHandler;
import edu.stanford.hci.r3.render.RegionRenderer;
import edu.stanford.hci.r3.units.Inches;
import edu.stanford.hci.r3.units.Units;
import edu.stanford.hci.r3.util.graphics.GraphicsUtils;

/**
 * <p>
 * A region is defined by a Shape that outlines the region. Internally, the region remembers the
 * Units that it is represented in. Thus, you may use whatever units pleases you.
 * </p>
 * <p>
 * Also, a region can be INPUT(REALTIME|BATCH), OUTPUT(REALTIME|INTERMITTENT), or STATIC. This is
 * entirely determined by the type(s) of attachments (input event handlers, input filters, realtime
 * outputs, print output) you add to a region. If you do not add anything, it is by default a STATIC
 * region. For example, if you add an input event handler for a streaming pen, it becomes an
 * INPUT(REALTIME) region. Regions that handle interactive input will automatically be overlaid with
 * pattern when rendered to PDF or to a printer.
 * </p>
 * <p>
 * Regardless of whether the Shape is closed or not, we assume that all regions are closed Shapes. A
 * rectangular region is represented using this class's Region(4 args) constructors.
 * </p>
 * <p>
 * <span class="BSDLicense"> This software is distributed under the <a
 * href="http://hci.stanford.edu/research/copyright.txt">BSD License</a>. </span>
 * </p>
 * 
 * @author <a href="http://graphics.stanford.edu/~ronyeh">Ron B Yeh</a> (ronyeh(AT)cs.stanford.edu)
 */
public class Region {

	/**
	 * String constant for a warning message.
	 */
	private static final String WARNING_POST = " (see Region.java)";

	/**
	 * String constant for a warning message.
	 */
	private static final String WARNING_PRE = "WARNING: Using Default Renderer for ";

	/**
	 * If the region is active (i.e., NOT STATIC), we will overlay pattern over it.
	 */
	private boolean active = false;

	/**
	 * Filters events and passes them to other event handlers (which are usually customized to the
	 * event filter)...
	 */
	private List<ContentFilter> contentFilters = new ArrayList<ContentFilter>();

	/**
	 * All Regions can have event handlers that listen for pen events. If the event handler list is
	 * non empty, the region should also be set to active. We can do this automatically. If the
	 * region is not set to active, no pattern will be rendered when a renderer processes this
	 * region.
	 */
	private List<EventHandler> eventHandlers = new ArrayList<EventHandler>();

	/**
	 * The name of the region (e.g., Public/Private Button). Useful for debugging. Initialized with
	 * a simple default.
	 */
	private String name = "A Region";

	/**
	 * This is used only to interpret the shape's true physical size. The value of the units object
	 * doesn't matter. Only the type of the unit matters.
	 */
	protected Units referenceUnits;

	/**
	 * Internal horizontal scale of the region. When rendering, we will multiply the shape by this
	 * scale. This is only a RECOMMENDATION and not a requirement of the renderer, however, as some
	 * regions may not make sense if scaled after the fact. However, we will try to make sure most
	 * of our calculations respect this scaling factor.
	 */
	protected double scaleX = 1.0;

	/**
	 * Internal vertical scale of the region. When rendering, we will multiply the shape by this
	 * scale.
	 */
	protected double scaleY = 1.0;

	/**
	 * This is the shape of the region. It is stored as unscaled coordinates, to be interpreted by
	 * the referenceUnits object. Upon rendering, client code SHOULD but is not REQUIRED to respect
	 * the scaleX and scaleY parameters to adjust the width and height.
	 */
	private Shape shape;

	private Color strokeColor = Color.BLACK;

	/**
	 * By default, regions are visible (they tend to be images, pattern, etc). However, if you would
	 * like to create an invisible region, go ahead. We're not gonna stop you.=)
	 */
	private boolean visible = true;

	/**
	 * For our American friends.
	 * 
	 * @param xInches
	 * @param yInches
	 * @param wInches
	 * @param hInches
	 */
	public Region(double xInches, double yInches, double wInches, double hInches) {
		this(new Rectangle2D.Double(xInches, yInches, wInches, hInches), Inches.ONE);
	}

	/**
	 * 
	 * @param s
	 *            the shape that defines this region.
	 * @param u
	 *            the reference unit for interpreting the shape's coordinates
	 */
	public Region(Shape s, Units u) {
		shape = s;
		referenceUnits = u;
	}

	/**
	 * A protected constructor so subclasses can assign the shape whichever way they please. A
	 * Region doesn't really make sense without a shape, so use this constructor carefully (i.e.,
	 * make sure to assign a sensible shape).
	 * 
	 * @param u
	 */
	protected Region(Units u) {
		referenceUnits = u;
	}

	/**
	 * We will convert all the units to x's type. Thus, if you pass in an Inch, Centimeter, Foot,
	 * Points, we will convert everything to Inches. It's OK to keep the x object around, because we
	 * only use it to interpret the shape object.
	 * 
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 */
	public Region(Units x, Units y, Units w, Units h) {
		this(new Rectangle2D.Double(x.getValue(), y.getValueIn(x), // assume a Rectangle2D
				w.getValueIn(x), h.getValueIn(x)), x);
	}

	/**
	 * @param filter
	 */
	public void addContentFilter(ContentFilter filter) {
		contentFilters.add(filter);
		active = true;
	}

	/**
	 * Keeps track of this event handler. The PaperToolkit will dispatch events to these, whenever
	 * the event deals with this region.
	 * 
	 * @param handler
	 */
	public void addEventHandler(EventHandler handler) {
		eventHandlers.add(handler);
		active = true;
	}

	/**
	 * @return
	 */
	public List<ContentFilter> getEventFilters() {
		return contentFilters;
	}

	/**
	 * @return the event engine will access the event handlers, to invoke events.
	 */
	public List<EventHandler> getEventHandlers() {
		return eventHandlers;
	}

	/**
	 * @return
	 */
	public Units getHeight() {
		return referenceUnits.getUnitsObjectOfSameTypeWithValue(shape.getBounds2D().getHeight()
				* scaleY);
	}

	/**
	 * @return
	 */
	public String getIsActiveString() {
		return " [" + (isActive() ? "ACTIVE" : "STATIC") + "]";
	}

	/**
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return
	 */
	public Units getOriginX() {
		return referenceUnits.getUnitsObjectOfSameTypeWithValue(shape.getBounds2D().getX());
	}

	/**
	 * @return
	 */
	public Units getOriginY() {
		return referenceUnits.getUnitsObjectOfSameTypeWithValue(shape.getBounds2D().getY());
	}

	/**
	 * Subclasses should override this, to customize rendering. Otherwise, you'll just get gray
	 * boxes, which is what RegionRenderer does.
	 * 
	 * @return the renderer for this region
	 */
	public RegionRenderer getRenderer() {
		// subclasses should override this method
		// otherwise, you will get a warning
		if (!getClass().getSimpleName().equals("Region")) {
			System.out.println(WARNING_PRE + this + WARNING_POST);
		}
		return new RegionRenderer(this);
	}

	/**
	 * @return
	 */
	public double getScaleX() {
		return scaleX;
	}

	/**
	 * @return
	 */
	public double getScaleY() {
		return scaleY;
	}

	/**
	 * Subclasses can modify the internal shape object.
	 * 
	 * @return the internal shape, at your peril.
	 */
	public Shape getShape() {
		return shape;
	}

	public Color getStrokeColor() {
		return strokeColor;
	}

	/**
	 * TODO: We should make sure that Units objects immutable if possible.
	 * 
	 * @return a pointer to the actual units object.
	 */
	public Units getUnits() {
		return referenceUnits;
	}

	/**
	 * @return a bounds object that represents the UNSCALED internal shape.
	 */
	public Rectangle2D getUnscaledBounds2D() {
		return shape.getBounds2D();
	}

	/**
	 * @return
	 */
	public Units getUnscaledBoundsHeight() {
		return referenceUnits.getUnitsObjectOfSameTypeWithValue(shape.getBounds2D().getHeight());
	}

	/**
	 * @return
	 */
	public Units getUnscaledBoundsWidth() {
		return referenceUnits.getUnitsObjectOfSameTypeWithValue(shape.getBounds2D().getWidth());
	}

	/**
	 * @return a copy of the internal shape as a Java2D GeneralPath. You should use this with
	 *         getScaleX/Y to determine the true shape. Alternatively, use getScaledShapeCopy()
	 */
	public Shape getUnscaledShapeCopy() {
		return new GeneralPath(shape);
	}

	/**
	 * @return
	 */
	public Units getWidth() {
		return referenceUnits.getUnitsObjectOfSameTypeWithValue(shape.getBounds2D().getWidth()
				* scaleX);
	}

	/**
	 * @return if this region is an active (NON-STATIC) region. This means that upon rendering to
	 *         PDF/Printer, pattern will be displayed over this region.
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * @return whether the region will be visible. TODO: The renderer SHOULD respect this flag.
	 */
	public boolean isVisible() {
		return visible;
	}

	/**
	 * For De-Serialization through XStream (just like in Java deserialization), we need to fill in
	 * some fields, especially if they are null due to old serialized versions, or because of
	 * transient variables.
	 * 
	 * For fields that are unexpectedly null, signal a warning.
	 * 
	 * @return
	 */
	private Object readResolve() {
		if (eventHandlers == null) {
			System.err.println("Region.java:: [" + getName()
					+ "]'s eventHandlers list was unexpectedly null upon "
					+ "deserialization with XStream. Perhaps you need to "
					+ "reserialize your Regions?");
			eventHandlers = new ArrayList<EventHandler>();
		}
		if (contentFilters == null) {
			System.err.println("Region.java:: [" + getName()
					+ "]'s eventFilters list was unexpectedly null upon "
					+ "deserialization with XStream. Perhaps you need to "
					+ "reserialize your Regions?");
			contentFilters = new ArrayList<ContentFilter>();
		}

		return this;
	}

	/**
	 * Resets to the default scaling factors.
	 */
	public void resetScale() {
		setScale(1.0, 1.0);
	}

	/**
	 * Scales the points in the current region by a horizontal and vertical multiplier.
	 * 
	 * @param sX
	 * @param sY
	 */
	public void scaleRegion(double sX, double sY) {
		scaleX = scaleX * sX;
		scaleY = scaleY * sY;
	}

	/**
	 * Resizes the region uniformly in x and y. We actually just store the number and scale it
	 * whenever we need to render the final region. The Lazy Approach. =) This is nice because we
	 * can scale the region multiple times without getting aliasing effects.
	 * 
	 * @param scale
	 */
	public void scaleRegionUniformly(double scale) {
		scaleRegion(scale, scale);
	}

	/**
	 * @param activeRegion
	 *            determines whether this will be an active region or not
	 */
	public void setActive(boolean activeRegion) {
		active = activeRegion;
	}

	/**
	 * @param n
	 *            the name of the region. Name it something useful, like "Blue Button for Changing
	 *            Pen Colors"
	 */
	public void setName(String n) {
		name = n;
	}

	/**
	 * Replaces the scaling factors.
	 * 
	 * @param newScaleX
	 * @param newScaleY
	 */
	public void setScale(double newScaleX, double newScaleY) {
		scaleX = newScaleX;
		scaleY = newScaleY;
	}

	/**
	 * Subclasses can use this method to set the shape after constructing the object.
	 * 
	 * @param s
	 */
	protected void setShape(Shape s) {
		shape = s;
	}

	public void setStrokeColor(Color strokeCol) {
		strokeColor = strokeCol;
	}

	/**
	 * @param v
	 *            whether the region will be visible
	 */
	public void setVisible(boolean v) {
		visible = v;
	}

	/**
	 * Please override for more interesting output. This will print the name of the class along with
	 * all the segments of the shape.
	 * 
	 * @see java.lang.Object#toString()
	 * @return the String representation of this Region
	 */
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		final String className = shape.getClass().getName();
		sb.append(getName() + " of type ");
		sb.append(className.substring(className.lastIndexOf(".") + 1) + ": {");

		final PathIterator pathIterator = shape.getPathIterator(AffineTransform.getScaleInstance(
				scaleX, scaleY));

		sb.append(GraphicsUtils.getPathAsString(pathIterator));

		sb.append("} in " + referenceUnits.getUnitName());
		sb.append(getIsActiveString());
		return sb.toString();
	}

}