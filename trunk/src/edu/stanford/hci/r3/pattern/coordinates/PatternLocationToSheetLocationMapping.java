package edu.stanford.hci.r3.pattern.coordinates;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.stanford.hci.r3.PaperToolkit;
import edu.stanford.hci.r3.paper.Region;
import edu.stanford.hci.r3.paper.Sheet;
import edu.stanford.hci.r3.units.Units;
import edu.stanford.hci.r3.util.SystemUtils;
import edu.stanford.hci.r3.util.files.FileUtils;

/**
 * <p>
 * This class stores mappings from regions of pattern (and their coordinates in Anoto space) to
 * Sheets and locations on those sheets. This mapping works both ways. Given a location on the
 * sheet, we should be able to find the pattern coordinate. Given a coordinate, we should be able to
 * find the location on the sheet.
 * </p>
 * <p>
 * An instance of this object should be built when a PDF is rendered with pattern. At that moment,
 * regions on a sheet are bound to specific physical coordinates. Each application should store this
 * mapping per sheet.
 * </p>
 * <p>
 * The SheetRenderer class uses this class to save the mapping to disk, so that a future instance
 * can load the mapping and run the application.
 * </p>
 * <p>
 * <span class="BSDLicense"> This software is distributed under the <a
 * href="http://hci.stanford.edu/research/copyright.txt">BSD License</a>. </span>
 * </p>
 * 
 * @author <a href="http://graphics.stanford.edu/~ronyeh">Ron B Yeh</a> (ronyeh(AT)cs.stanford.edu)
 */
public class PatternLocationToSheetLocationMapping {

	/**
	 * Allows us to save and load to/from xml files, because we can identify regions more or less
	 * uniquely this way.
	 */
	public static class RegionID {
		private Units height;

		private String name;

		private Units originX;

		private Units originY;

		private Units width;

		public RegionID(Region r) {
			name = r.getName();
			originX = r.getOriginX();
			originY = r.getOriginY();
			width = r.getWidth();
			height = r.getHeight();
		}

		/**
		 * Same name, origin, and dimensions. Good enough for now!
		 * 
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object o) {
			if (o instanceof RegionID) {
				RegionID r = (RegionID) o;
				return name.equals(r.name) && originX.equals(r.originX)
						&& originY.equals(r.originY) && width.equals(r.width)
						&& height.equals(r.height);
			}
			return false;
		}

		/**
		 * Makes HashMaps work.
		 * 
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			return name.hashCode();
		}
	}

	/**
	 * Binds regions to pattern bounds, specified in logical (batched) and physical (streamed)
	 * coordinates.
	 */
	private Map<Region, TiledPatternCoordinateConverter> regionToPatternBounds = new HashMap<Region, TiledPatternCoordinateConverter>();

	/**
	 * The mapping is bound to one sheet.
	 */
	private Sheet sheet;

	/**
	 * One mapping object per sheet. Create this object after you have added all the regions that
	 * you need to the sheet. This class will maintain a mapping of Regions to physical (stremaing)
	 * and logical (batched) pen coordinates.
	 * 
	 * @param s
	 */
	public PatternLocationToSheetLocationMapping(Sheet s) {
		sheet = s;
		initializeMap(s.getRegions());

		// check to see if the pattern configuration file exists.
		// if it does, load it automatically
		Set<File> configurationPaths = sheet.getConfigurationPaths();

		// files end with .patternInfo.xml
		final String[] extensionFilter = new String[] { "patternInfo.xml" };

		for (File path : configurationPaths) {
			// System.out.println(path);
			List<File> patternInfoFiles = FileUtils.listVisibleFiles(path, extensionFilter);
			// System.out.println(patternInfoFiles.size());
			for (File f : patternInfoFiles) {
				// System.out.println(f.getAbsolutePath());
				loadConfigurationFromXML(f);
			}
		}
	}

	/**
	 * @param r
	 * @return
	 */
	public TiledPatternCoordinateConverter getPatternBoundsOfRegion(Region r) {
		return regionToPatternBounds.get(r);
	}

	/**
	 * @param regions
	 */
	private void initializeMap(List<Region> regions) {
		for (Region r : regions) {
			regionToPatternBounds.put(r, new TiledPatternCoordinateConverter());
		}
	}

	/**
	 * @param xmlFile
	 */
	@SuppressWarnings("unchecked")
	public void loadConfigurationFromXML(File xmlFile) {
		HashMap<RegionID, TiledPatternCoordinateConverter> regionIDToPattern = (HashMap<RegionID, TiledPatternCoordinateConverter>) PaperToolkit
				.fromXML(xmlFile);
		for (Region r : regionToPatternBounds.keySet()) {
			RegionID xmlKey = new RegionID(r);
			// System.out.println("Found Key: " + regionIDToPattern.containsKey(xmlKey) + " for " +
			// r.getName());

			// loads the information into our map
			if (regionIDToPattern.containsKey(xmlKey)) {
				regionToPatternBounds.put(r, regionIDToPattern.get(xmlKey));
			}
		}
	}

	/**
	 * 
	 */
	public void printMapping() {
		for (Region r : regionToPatternBounds.keySet()) {
			System.out.print(r.getName() + " --> ");
			TiledPatternCoordinateConverter bounds = regionToPatternBounds.get(r);
			System.out.println(bounds);
		}
	}

	/**
	 * Due to xstream's inability to serial/unserialize really complicated classes, we will save
	 * only a regionName+origin --> pattern info mapping
	 * 
	 * @param xmlFile
	 */
	public void saveConfigurationToXML(File xmlFile) {
		try {

			// create a new map that goes from name+origin to pattern bounds mapping
			// only save active regions... because we don't render pattern for nonactive regions
			HashMap<RegionID, TiledPatternCoordinateConverter> regionIDToPattern = new HashMap<RegionID, TiledPatternCoordinateConverter>();
			Set<Region> regions = regionToPatternBounds.keySet();
			for (Region r : regions) {
				if (!r.isActive()) {
					continue;
				}
				RegionID rid = new RegionID(r);
				regionIDToPattern.put(rid, regionToPatternBounds.get(r));
			}
			PaperToolkit.toXML(regionIDToPattern, new FileOutputStream(xmlFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param r
	 * @param bounds
	 */
	public void setPatternBoundsOfRegion(Region r, TiledPatternCoordinateConverter bounds) {
		if (regionToPatternBounds.containsKey(r) || sheet.containsRegion(r)) {
			// updating a known region OR
			// adding a new region (probably added to the sheet after this object was constructed)
			regionToPatternBounds.put(r, bounds);
		} else {
			System.err.println("PatternLocationToSheetLocationMapping.java: Region unknown. "
					+ "Please add it to the sheet before updating this mapping.");
		}
	}
}