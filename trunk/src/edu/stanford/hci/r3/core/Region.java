package edu.stanford.hci.r3.core;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;

import edu.stanford.hci.r3.render.RegionRenderer;
import edu.stanford.hci.r3.units.Inches;
import edu.stanford.hci.r3.units.Units;
import edu.stanford.hci.r3.util.graphics.GraphicsUtils;

/**
 * <p>
 * This software is distributed under the <a href="http://hci.stanford.edu/research/copyright.txt">
 * BSD License</a>.
 * </p>
 * 
 * @author <a href="http://graphics.stanford.edu/~ronyeh">Ron B Yeh</a> (ronyeh(AT)cs.stanford.edu)
 * 
 * A region is defined by a Shape that outlines the region. Internally, the region remembers the
 * Units that it is represented in. Thus, you may use whatever units pleases you.
 * 
 * Also, a region can be INPUT(REALTIME|BATCH), OUTPUT(REALTIME|INTERMITTENT), or STATIC. This is
 * entirely determined by the type(s) of attachments (input event handlers, input filters, realtime
 * outputs, print output) you add to a region. If you do not add anything, it is by default a STATIC
 * region. For example, if you add an input event handler for a streaming pen, it becomes an
 * INPUT(REALTIME) region. Regions that handle interactive input will automatically be overlaid with
 * pattern when rendered to PDF or to a printer.
 * 
 * Regardless of whether the Shape is closed or not, we assume that all regions are closed Shapes.
 */
public class Region {

	/**
	 * If the region is active (i.e., NOT STATIC), we will overlay pattern over it.
	 */
	private boolean active = false;

	/**
	 * Internal horizontal scale of the region. When rendering, we will multiply the shape by this
	 * scale.
	 */
	protected double scaleX = 1.0;

	/**
	 * Internal vertical scale of the region. When rendering, we will multiply the shape by this
	 * scale.
	 */
	protected double scaleY = 1.0;

	/**
	 * This is the shape of the region.
	 */
	private Shape shape;

	/**
	 * This is used only to interpret the shape's true physical size. The value of the units object
	 * doesn't matter. Only the type of the unit matters.
	 */
	protected Units units;

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
	 */
	public Region(Shape s, Units u) {
		shape = s;
		units = u;
	}

	/**
	 * A protected constructor so subclasses can assign the shape whichever way they please. A
	 * Region doesn't really make sense without a shape, so use this constructor carefully (i.e.,
	 * make sure to assign a sensible shape).
	 * 
	 * @param u
	 */
	protected Region(Units u) {
		units = u;
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

	public String getIsActiveString() {
		return " [" + (isActive() ? "ACTIVE" : "STATIC") + "]";
	}

	/**
	 * Subclasses should override this, to customize rendering. Otherwise, you'll just get gray
	 * boxes, which is what RegionRenderer does.
	 * 
	 * @return the renderer for this region
	 */
	public RegionRenderer getRenderer() {
		System.out.println("WARNING: Using Default Renderer for " + this + " (see Region.java)");
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

	/**
	 * @return a copy of the units object.
	 */
	public Units getUnits() {
		return units.getCopy();
	}

	/**
	 * @return
	 */
	protected Units getUnitsReference() {
		return units;
	}

	/**
	 * @return a bounds object that represents the UNSCALED internal shape.
	 */
	public Rectangle2D getUnscaledBounds2D() {
		return shape.getBounds2D();
	}

	/**
	 * @return a copy of the internal shape as a Java2D GeneralPath. You should use this with
	 *         getScaleX/Y to determine the true shape. Alternatively, use getScaledShapeCopy()
	 */
	public Shape getUnscaledShapeCopy() {
		return new GeneralPath(shape);
	}

	/**
	 * @return if this region is an active (NON-STATIC) region. This means that upon rendering to
	 *         PDF/Printer, pattern will be displayed over this region.
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * @return
	 */
	public boolean isVisible() {
		return visible;
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

	/**
	 * @param v
	 */
	public void setVisible(boolean v) {
		visible = v;
	}

	/**
	 * Please override for more interesting output.
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		String className = shape.getClass().getName();
		sb.append(className.substring(className.lastIndexOf(".") + 1) + ": {");

		PathIterator pathIterator = shape.getPathIterator(AffineTransform.getScaleInstance(scaleX,
				scaleY));

		sb.append(GraphicsUtils.getPathAsString(pathIterator));

		sb.append("} in " + units.getUnitName());
		sb.append(getIsActiveString());
		return sb.toString();
	}

}