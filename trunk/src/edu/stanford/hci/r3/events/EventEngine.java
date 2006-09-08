package edu.stanford.hci.r3.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.stanford.hci.r3.pen.Pen;
import edu.stanford.hci.r3.pen.streaming.PenListener;
import edu.stanford.hci.r3.pen.streaming.PenSample;
import edu.stanford.hci.r3.util.DebugUtils;

/**
 * <p>
 * When you ask the PaperToolkit to run a paper Application, there will be exactly one EventEngine
 * handling all pen events for that Application. This EventEngine will process batched pen data, and
 * also handle streaming data. We will tackle streaming first.
 * </p>
 * <p>
 * This class is responsible for creating clicks, drags, etc.
 * </p>
 * <p>
 * TODO: Test if multiple pens work!
 * </p>
 * <p>
 * This software is distributed under the <a href="http://hci.stanford.edu/research/copyright.txt">
 * BSD License</a>.
 * </p>
 * 
 * @author <a href="http://graphics.stanford.edu/~ronyeh">Ron B Yeh</a> (ronyeh(AT)cs.stanford.edu)
 */
public class EventEngine {

	/**
	 * Keeps track of how many times a pen has been registered. If during an unregister, this count
	 * drops to zero, we remove the pen altogether.
	 */
	private Map<Pen, Integer> penRegistrationCount = new HashMap<Pen, Integer>();

	/**
	 * Allows us to identify a pen by ID (the position of the pen in this list).
	 */
	private List<Pen> pens = new ArrayList<Pen>();

	/**
	 * Each pen gets one and only one event engine listener...
	 */
	private Map<Pen, PenListener> penToListener = new HashMap<Pen, PenListener>();

	/**
	 * 
	 */
	public EventEngine() {

	}

	/**
	 * @param pen
	 * @param listener
	 */
	private void addPenToInternalLists(Pen pen, PenListener listener) {
		penToListener.put(pen, listener);
		pen.addLivePenListener(listener);
		pens.add(pen);
	}

	/**
	 * @param pen
	 * @return the registration count AFTER the decrement.
	 */
	private int decrementPenRegistrationCount(Pen pen) {
		Integer count = penRegistrationCount.get(pen);
		if (count == null) {
			// huh? We don't have a record for this pen...
			DebugUtils.println("We do not have a record for this pen, and "
					+ "cannot decrement the registration count.");
			return 0;
		} else if (count == 1) {
			penRegistrationCount.remove(pen); // decrement from one to zero
			return 0;
		} else {
			penRegistrationCount.put(pen, count - 1);
			return count - 1;
		}
	}

	/**
	 * @param pen
	 * @return a pen listener that will report data to this event engine. The engine will then
	 *         package the data and report it to all event handlers (read: interactors) that are
	 *         interested in this data.
	 */
	private PenListener getNewPenListener(final Pen pen) {
		return new PenListener() {

			public void penDown(PenSample sample) {
				System.out.println(sample);
			}

			public void penUp(PenSample sample) {
				System.out.println(sample);
			}

			public void sample(PenSample sample) {
				System.out.println(sample);
				System.out.println("Dispatching Event for pen #" + pens.indexOf(pen));
			}
		};
	}

	/**
	 * @param pen
	 */
	private void incrementPenRegistrationCount(Pen pen) {
		Integer count = penRegistrationCount.get(pen);
		if (count == null) {
			penRegistrationCount.put(pen, 1); // incremented from zero to one
		} else {
			penRegistrationCount.put(pen, count + 1);
		}
		DebugUtils.println("Count is at " + penRegistrationCount.get(pen));
	}

	/**
	 * If you register a pen multiple times, a different pen listener will be attached to the pen.
	 * Only ONE EventEngine listener will be attached to a pen at one time. Otherwise, multiple
	 * events would get fired by the same pen.
	 * 
	 * @param pen
	 */
	public void register(Pen pen) {
		// get the old listener, if it exists
		PenListener listener = penToListener.get(pen);
		if (listener != null) {
			removePenFromInternalLists(pen, listener);
		}
		// this pen has never been registered, or
		// we just removed the old listener...

		// add a new listener
		listener = getNewPenListener(pen);
		addPenToInternalLists(pen, listener);
		incrementPenRegistrationCount(pen);
	}

	/**
	 * @param pen
	 *            removes this pen from our internal lists without updating the registration count.
	 * @param listener
	 */
	private void removePenFromInternalLists(Pen pen, PenListener listener) {
		penToListener.remove(pen);
		pen.removeLivePenListener(listener);
		pens.remove(pen);
	}

	/**
	 * @param pen
	 */
	public void unregisterPen(Pen pen) {
		int newCount = decrementPenRegistrationCount(pen);
		if (newCount == 0) {
			DebugUtils.println("Count is at Zero. Let's remove the pen and its listener...");
			PenListener listener = penToListener.get(pen);
			removePenFromInternalLists(pen, listener);
		}
	}
}