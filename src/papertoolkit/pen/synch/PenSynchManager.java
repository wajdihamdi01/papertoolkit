package papertoolkit.pen.synch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import papertoolkit.PaperToolkit;
import papertoolkit.util.files.FileUtils;
import papertoolkit.util.files.SortDirection;


/**
 * <p>
 * Helps us figure out what has been synched, etc.
 * </p>
 * <p>
 * <span class="BSDLicense"> This software is distributed under the <a
 * href="http://hci.stanford.edu/research/copyright.txt">BSD License</a>. </span>
 * </p>
 * 
 * @author <a href="http://graphics.stanford.edu/~ronyeh">Ron B Yeh</a> (ronyeh(AT)cs.stanford.edu)
 */
public class PenSynchManager {

	/**
	 * 
	 */
	private File penSynchDataPath;
	
	/**
	 * 
	 */
	private List<File> xmlFiles;

	/**
	 * 
	 */
	public PenSynchManager() {
		// get the XML data path, and check to see if there are any new files since we last checked.
		// All we do is maintain a modification time of the last file we have processed, and we
		// process any xml file that is newer than this lastModified date.
		penSynchDataPath = PaperToolkit.getPenSynchDataPath();

		// get list of XML files in the penSynch directory
		xmlFiles = FileUtils.listVisibleFiles(penSynchDataPath, new String[] { "XML" });

		// DebugUtils.println(xmlFiles);
	}

	/**
	 * @return
	 */
	public List<File> getFiles() {
		SortDirection sortDir = SortDirection.NEW_TO_OLD;
		List<File> files = new ArrayList<File>();
		for (File f : xmlFiles) {
			files.add(f);
		}
		FileUtils.sortByLastModified(files, sortDir);
		return files;
	}

	/**
	 * Return this in a sorted list, from oldest to newest.
	 * 
	 * @param lastModifiedTimestamp
	 * @return
	 */
	public List<File> getFilesNewerThan(long lastModifiedTimestamp) {
		List<File> newFiles = new ArrayList<File>();
		for (File f : xmlFiles) {
			if (f.lastModified() > lastModifiedTimestamp) {
				// DebugUtils.println(f.lastModified());
				newFiles.add(f);
			}
		}
		FileUtils.sortByLastModified(newFiles, SortDirection.OLD_TO_NEW);
		return newFiles;
	}

	public File getMostRecentFile() {
		return getFiles().get(0);
	}

	public PenSynch getMostRecentPenSynch() {
		File mostRecentFile = getMostRecentFile();
		return new PenSynch(mostRecentFile);
	}
}
