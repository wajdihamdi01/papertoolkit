package edu.stanford.hci.r3.pen.ink;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import edu.stanford.hci.r3.util.DebugUtils;

/**
 * <p>
 * Integrate all the nice features that we can use to analyze ink into this class.
 * </p>
 * <p>
 * <span class="BSDLicense"> This software is distributed under the <a
 * href="http://hci.stanford.edu/research/copyright.txt">BSD License</a>. </span>
 * </p>
 * 
 * @author <a href="http://graphics.stanford.edu/~ronyeh">Ron B Yeh</a> (ronyeh(AT)cs.stanford.edu)
 * 
 */
public class InkUtils {

	/**
	 * Finds all the strokes contained within another stroke's bounding box.
	 * @param inkWell  the strokes to check
	 * @param container  the containing stroke
	 * @return  a list of strokes within the container
	 */
	public static List<InkStroke> findAllStrokesContainedWithin(List<Ink> inkWell,
			InkStroke container) {

		// calculate the bounds of the container stroke
		Rectangle2D bounds = container.getBounds();

		// return any stroke that falls completely within these bounds
		// do not include the original container!
		List<InkStroke> matchingStrokes = new ArrayList<InkStroke>();
		for (Ink ink : inkWell) {
			List<InkStroke> strokes = ink.getStrokes();
			for (InkStroke stroke : strokes) {
				if (bounds.contains(stroke.getBounds()) && !container.equals(stroke)) {
					// the stroke is completely inside the containing stroke
					matchingStrokes.add(stroke);
				}
			}
		}
		return matchingStrokes;
	}

	/**
	 * Finds all strokes outside a containing stroke (no overlap)
	 * @param inkWell  the list of strokes to check
	 * @param container  the containing stroke 
	 * @return  a list of strokes not within the container
	 */
	public static List<InkStroke> findAllStrokesOutside(List<Ink> inkWell,
			InkStroke container) {

		// calculate the bounds of the container stroke
		Rectangle2D bounds = container.getBounds();

		// return any stroke that falls completely outside these bounds
		List<InkStroke> matchingStrokes = new ArrayList<InkStroke>();
		for (Ink ink : inkWell) {
			List<InkStroke> strokes = ink.getStrokes();
			for (InkStroke stroke : strokes) {
				if (!bounds.intersects(stroke.getBounds())) {
					// the stroke is completely outside the containing stroke
					matchingStrokes.add(stroke);
				}
			}
		}
		return matchingStrokes;
	}

	/**
	 * Finds all strokes that intersect with but are not entirely contained
	 * by another stroke.
	 * 
	 * @param inkWell  the list of strokes
	 * @param container  the containing stroke
	 * @return  a list of strokes that are partly inside and outside the container
	 */
	public static List<InkStroke> findAllStrokesPartlyOutside(List<Ink> inkWell,
			InkStroke container) {

		// calculate the bounds of the container stroke
		Rectangle2D bounds = container.getBounds();

		// return any stroke that overlaps the container but is not contained
		// by it
		List<InkStroke> matchingStrokes = new ArrayList<InkStroke>();
		for (Ink ink : inkWell) {
			List<InkStroke> strokes = ink.getStrokes();
			for (InkStroke stroke : strokes) {
				Rectangle2D strokeBounds = stroke.getBounds();
				if (bounds.intersects(strokeBounds) && 
						!bounds.contains(strokeBounds)) {
					// the stroke is completely outside the containing stroke
					matchingStrokes.add(stroke);
				}
			}
		}
		return matchingStrokes;
	}
	
	/**
	 * Clusters a list of strokes into a list of list of strokes.  The margin
	 * allows you to control how big the compared bounding boxes are.  A margin
	 * of 2.0 would double the width and height of each bounding box before
	 * making intersection comparisons.
	 *   
	 * @param strokes	list of strokes to group
	 * @param margin  the fraction of each stroke size to check for overlap
	 * @return  a list of inkstroke clusters
	 */
	public static List<Ink> clusterStrokes(List<InkStroke> strokes, 
			final double margin) {
		
		
		
		class Cluster extends Ink {
			private Rectangle2D bounds;
			private Rectangle2D expand(Rectangle2D r) {
				double dx = r.getWidth() * margin / 2;
				double dy = r.getHeight() * margin / 2;
				return new Rectangle2D.Double(r.getX()-dx, r.getY()-dy, 
						r.getWidth()+dx*2, r.getHeight()+dy*2);
			}
			boolean matches(InkStroke stroke) {
				return expand(stroke.getBounds()).intersects(bounds);
			}
			void add(InkStroke stroke) {
				super.addStroke(stroke);
				Rectangle2D strokeBounds = expand(stroke.getBounds());
				if (bounds == null)
					bounds = strokeBounds;
				else
					bounds.add(strokeBounds);
			}
		}
		
		List<Cluster> clusters = new ArrayList<Cluster>();
		
		for (InkStroke stroke : strokes) {
			Cluster foundCluster = null;
			for (Cluster cluster : clusters) {
				if (cluster.matches(stroke)) {
					foundCluster = cluster;
					break;
				}
			}
			if (foundCluster == null) {
				foundCluster = new Cluster();
				clusters.add(foundCluster);
			}
			foundCluster.add(stroke);
		}
		
		
		return new ArrayList<Ink>(clusters);
	}

	/**
	 * Finds the first clusters near a particular point (perhaps revise to find
	 * nearest cluster).
	 *   
	 * @param inkWell  list of clusters
	 * @param point  the point to compare to
	 * @param range  number of units away from the point to check
	 * @return  the first cluster near the point
	 */
	public static Ink findInkNearPoint(List<Ink> inkWell, 
			Point2D point, double range) {

		Rectangle2D pointRange = new Rectangle2D.Double(point.getX()-range,
					point.getY()-range, range*2,range*2);
		for (Ink ink : inkWell)
			for (InkStroke stroke : ink.getStrokes())
				if (pointRange.intersects(stroke.getBounds()))
					return ink;
		return null;
	}
	
	/**
	 * Finds the stroke with the largest area within a list of clusters.
	 * @param inkWell  the list of clusters
	 * @return  the inkstroke with the largest area
	 */
	public static InkStroke findStrokeWithLargestArea(List<Ink> inkWell) {
		InkStroke biggestStroke = null;
		double maxArea = Double.MIN_VALUE;

		for (Ink ink : inkWell) {
			List<InkStroke> strokes = ink.getStrokes();
			for (InkStroke stroke : strokes) {

				double area = stroke.getArea();
				if (area > maxArea) {
					biggestStroke = stroke;
					maxArea = area;
				}
			}
		}

		DebugUtils.println(biggestStroke);
		return biggestStroke;
	}

}