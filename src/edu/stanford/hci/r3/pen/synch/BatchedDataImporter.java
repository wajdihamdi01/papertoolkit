package edu.stanford.hci.r3.pen.synch;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Properties;

import javax.swing.JOptionPane;

import edu.stanford.hci.r3.PaperToolkit;
import edu.stanford.hci.r3.util.DebugUtils;
import edu.stanford.hci.r3.util.SystemUtils;
import edu.stanford.hci.r3.util.classpath.EclipseProjectClassLoader;
import edu.stanford.hci.r3.util.files.FileUtils;

/**
 * <p>
 * The batched importer will be called by the pen importer (.NET code) every time you synchronize
 * the pen. It sends the information to the BatchServer, which will notify any running applications.
 * 
 * TODO: In the future, applications do not need to be running all the time. They will be notified
 * of new data upon booting.
 * 
 * This class is launched by PaperToolkit\penSynch\bin\BatchImporter.exe. It creates a log file in
 * the same directory, that you can tail using an app such as BareTail.
 * </p>
 * <p>
 * <span class="BSDLicense"> This software is distributed under the <a
 * href="http://hci.stanford.edu/research/copyright.txt">BSD License</a>. </span>
 * </p>
 * 
 * @author <a href="http://graphics.stanford.edu/~ronyeh">Ron B Yeh</a> (ronyeh(AT)cs.stanford.edu)
 */
public class BatchedDataImporter {

	/**
	 * 
	 */
	private static final boolean DEBUG = true;

	/**
	 * @param args
	 */
	public BatchedDataImporter(String[] args) {
		sendToRegisteredMonitors(args);

		// The OLD WAY. We should preserve it...
		// sendToBatchedDataServer(args);
	}

	/**
	 * Send the data to the monitors that we have placed in the
	 * PaperToolkit/penSynch/RegisteredBatchedMonitors/ directory.
	 */
	private void sendToRegisteredMonitors(String[] args) {
		// find all files in PaperToolkit/penSynch/RegisteredBatchedMonitors/
		final File monitorsDir = new File(PaperToolkit.getToolkitRootPath(),
				"/penSynch/RegisteredBatchedMonitors/");
		final List<File> monitors = FileUtils.listVisibleFiles(monitorsDir);
		for (File f : monitors) {
			// DebugUtils.println(f.getAbsolutePath());
			String fileName = f.getName().toLowerCase();
			if (fileName.endsWith(".jar")) {
				// this works if your JRE install has associated JAR files with the double-click
				// action
				DebugUtils.println("Running JAR: " + fileName);
				SystemUtils.runJar(f, new String[] { '"' + new File(
						"penSynch/RegisteredBatchedMonitors/Examples/Example.txt")
						.getAbsolutePath() + '"' });
			} else if (fileName.endsWith(".bat") || fileName.endsWith(".exe")) {
				DebugUtils.println("Running BAT/EXE: " + fileName);
				SystemUtils.run(f, new String[] { '"' + new File(
						"penSynch/RegisteredBatchedMonitors/Examples/Example.txt")
						.getAbsolutePath() + '"' });
			} else if (fileName.endsWith(".monitor")) {
				DebugUtils.println("Running Monitor: " + f.getAbsolutePath());
				runMonitorFromConfigFile(f, args);
			} else {
				// unhandled file
			}
		}
	}

	/**
	 * @param args
	 */
	private void sendToBatchedDataServer(String[] args) {
		PrintWriter pw = null;
		try {
			if (DEBUG) {
				JOptionPane.showMessageDialog(null, "Batched Pen Data saved to penSynch/Data/XML/."
						+ "Notifying Registered Listeners...");
			}

			pw = new PrintWriter(new File("BatchImporter.log"));
			pw.println("Running the Batched Pen Data Importer");
			for (String arg : args) {
				pw.println("Argument: " + arg);
			}

			// find all registered listeners, and run them in sequence, with the correct arguments

			// open a socket connection to the batch importer / event handler
			// if an app is running, it will handle the incoming batched ink...
			// if not, this will simply throw an exception and fall through...
			final Socket socket = new Socket("localhost", BatchedDataServer.DEFAULT_PLAINTEXT_PORT);
			final BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket
					.getOutputStream()));

			// send over the xml file! =)
			bw.write("XML: " + args[0] + SystemUtils.LINE_SEPARATOR); // the
			// path!
			bw.write(BatchedDataServer.EXIT_COMMAND + SystemUtils.LINE_SEPARATOR);
			bw.flush();
			bw.close();

			// close the socket connection...
			socket.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			pw.println(e.getLocalizedMessage());
		} catch (UnknownHostException e) {
			e.printStackTrace();
			pw.println(e.getLocalizedMessage());
		} catch (IOException e) {
			e.printStackTrace();
			pw.println(e.getLocalizedMessage());
		}
		if (pw != null) {
			pw.flush();
			pw.close();
		}
	}

	/**
	 * Adds a monitor into the configuration file. This monitor will be run every time a digital pen
	 * is docked.
	 */
	public static void registerMonitor(BatchedDataImportMonitor monitor) {
		DebugUtils.println("Registering " + monitor.getName());

		// this is the name of the class
		DebugUtils.println(monitor.getClass());

		// this should be the run dir
		DebugUtils.println(monitor.getClass().getResource("/"));

		// create a new instance of this, and run the handler....
		// do it by name...
		try {

			Class<?> c = Class.forName("edu.stanford.hci.r3.demos.batched.monitor.BatchedMonitor");
			Object newInstance = c.newInstance();

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		// figure out its path / classpath...

		// save it to our configuration file

	}

	/**
	 * The BatchImporter.exe calls this through a wrapper JAR...
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		new BatchedDataImporter(args);
	}

	/**
	 * Reads in the file, figures out which directory our monitor resides in, and runs it, with the
	 * correct classpath!
	 * 
	 * @param monitorConfigFile
	 *            the *.monitor configuration file, in Java Properties (non-xml) format.
	 * @param args
	 */
	private static void runMonitorFromConfigFile(File monitorConfigFile, String[] args) {
		Properties p = new Properties();
		try {
			p.load(new FileInputStream(monitorConfigFile));

			String mainClass = p.getProperty("MainClass");
			String eclipseProjectDirectory = p.getProperty("EclipseProjectDirectory");

			DebugUtils.println(mainClass);
			DebugUtils.println(eclipseProjectDirectory);

			EclipseProjectClassLoader eclipesProjectClassLoader = new EclipseProjectClassLoader(new File(
					eclipseProjectDirectory));
			try {
				Class<?> classObj = eclipesProjectClassLoader.loadClass(mainClass);
				Method m = classObj.getMethod("main", new Class[] { args.getClass() });
				m.setAccessible(true);
				int mods = m.getModifiers();
				if (m.getReturnType() != void.class || !Modifier.isStatic(mods)
						|| !Modifier.isPublic(mods)) {
					throw new NoSuchMethodException("main");
				}
				// call the main class
				m.invoke(null, new Object[] { args });

			} catch (ClassNotFoundException e) {
				System.err.println("BatchedDataImporter:: Could not find the class: " + mainClass);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		DebugUtils.println(p.toString());
	}

	/**
	 * @param monitor
	 */
	public static void removeMonitor(BatchedDataImportMonitor monitor) {
		// TODO Auto-generated method stub
	}
}
