package de.ipvs.rni.exercise2.layers;

import de.ipvs.rni.exercise2.common.*;
import de.ipvs.rni.exercise2.packets.*;

import java.util.ArrayDeque;
import java.util.Iterator;

public class DataLinkLayer implements ProcessEvents {
	private static final int MTU = 1500;
	private static final int RETRANSMISSION_TIMEOUT = 4;
	public static final boolean CRC_IMPLEMENTED = true;

	private ArrayDeque<Byte> fromUpper = new ArrayDeque<>();
	private ArrayDeque<Byte> toUpper = new ArrayDeque<>();
	// The in-flight queue contains all elements which are not ACKed yet
	private ArrayDeque<Frame> inFlight = new ArrayDeque<>();
	// The retransmissionQueue contains the frames which need to be retransmitted
	public ArrayDeque<Frame> retransmissionQueue = new ArrayDeque<>();
	// The retransmissionTimeouts contains the timeouts for the frames in the
	// in-flight queue (same order)
	private ArrayDeque<Integer> retransmissionTimeouts = new ArrayDeque<>();
	private ArrayDeque<Frame> fromLower;
	private ArrayDeque<Frame> toLower;

	private int seqNo = 0;
	private int nextSeqNo = 0;
	private int availWindow = 5;
	private int nextAck = -1;
	private int curTime = 0;

	public DataLinkLayer() {
		nextAck = seqNo;
	}

	public void bind(ApplicationLayer appLayer) {
		appLayer.setFromLower(toUpper);
		appLayer.setToLower(fromUpper);
	}

	/**
	 * Converts Byte Queue to byte array and constructs Frame from it.
	 * Removes only bytes from queue up to maximum limit - Frame overhead
	 * @param bytesToSent
	 * @return
	 */
	public Frame createNextFrame(ArrayDeque<Byte> bytesToSent) {
		// TODO
		int size = bytesToSent.size();
		size = size > MTU - 4 - 4 - 8 ? MTU - 4 - 4 - 8 : size;
		byte[] bytes = new byte[size];
		int i = 0;
		while (i < size) {
			bytes[i] = bytesToSent.poll().byteValue();
			i++;
		}
		Frame f = new Frame(seqNo++, bytes, bytes.length);
		return f;
	}
	
	/**
	 * Converts byteque into frames to send to physical layer.
	 * Converts frames from physical layer into byteques for application layer.
	 * Tracks acknowledgements of frames and resends after timeout.
	 */
	@Override
	public void process() {
		curTime++;
		// TODO
		ArrayDeque<Frame> toBeProcessed =recieveFrames();
		boolean sendAck = processFrames(toBeProcessed);
		// Send ack in case of new frames (or resend ack for old frames)
		if(sendAck) {
			Frame ack = new Frame(nextSeqNo-1);
			toLower.add(ack);
		}

		// Re-send unacknowledged. Since first Element is oldest, only it needs to be checked for timeout
		// In case of timeout, all in flight frames are resent (go back n).
		if (!inFlight.isEmpty()) {
			if ((curTime - retransmissionTimeouts.peek()) > RETRANSMISSION_TIMEOUT) {
				toLower.addAll(inFlight);
				retransmissionTimeouts.clear();
				for (Frame f : inFlight) {
					retransmissionTimeouts.add(curTime);
				}
			}
		}

		// Send new frames and add elements to inFlight/retransmission queue
		while (!fromUpper.isEmpty() && availWindow > 0) {
			Frame f = createNextFrame(fromUpper);
			toLower.add(f);
			inFlight.add(f);
			retransmissionTimeouts.add(curTime);
			availWindow--;
		}
	}

	/**
	 * empties Frame queue from physical layer.
	 * Checks for integrity of frame and checks for acknowledgements.
	 * For acknowledgements, increments nextAck up to highest recieved acknowledgement, removing elements from inFlight and retransmision Queue in the process (inclusive acknowledgements).
	 * Non Ack Frames added to queue which is returned by this method.
	 * @return Returns all non ack frames with integrity
	 */
	private ArrayDeque<Frame> recieveFrames() {
		ArrayDeque<Frame> toBeProcessed=new ArrayDeque<>();
		while (!fromLower.isEmpty()) {
			Frame f = fromLower.poll();
			if (f.checkIntegrity()) {
				if (f.isAck()) {
					int ackNr = f.getAckNo();
					while (ackNr >= nextAck) {
						nextAck++;
						availWindow++;
						inFlight.poll();
						retransmissionQueue.poll();
					}
				} else {
					toBeProcessed.add(f);

				}
			}

		}
		return toBeProcessed;
	}

	/**
	 * iterates over given Frames, checking if next expected sequence number is contained.
	 * next sequence number increased with each accepted Frame.
	 * Reiterates as long as proper sequence number has been found.
	 * Done so as not to drop frames that arrived in the incorrect order.
	 * Checking for integrity should happen before.
	 * @param toBeProcessed Queue of Frames to be processed.
	 * @return true if acknowledgement is needed (past sequence number recieved or new expected sequence number)
	 */
	private boolean processFrames(ArrayDeque<Frame> toBeProcessed) {
		int trackChange=toBeProcessed.size();
		boolean sendAck = false;
		while (trackChange>0) {
			Frame f = toBeProcessed.poll();
			if (f.getSeqNo() == nextSeqNo) {
				byte[] temp = f.getPayload();
				for (byte b : temp) {
					toUpper.add(b);
				}
				nextSeqNo++;
				sendAck=true;
				trackChange=toBeProcessed.size();
			}else if(f.getSeqNo()<nextSeqNo) {
				sendAck=true;
				trackChange=toBeProcessed.size();
			}else {
				toBeProcessed.add(f);
				trackChange--;
			}
		}
		return sendAck;
	}

	//////////////////////////////////////////////////
	// Getter and Setter for testing purposes only! //
	//////////////////////////////////////////////////

	public int getSeqNo() {
		return seqNo;
	}

	public ArrayDeque<Byte> getFromUpper() {
		return fromUpper;
	}

	public void setFromUpper(ArrayDeque<Byte> fromUpper) {
		this.fromUpper = fromUpper;
	}

	public ArrayDeque<Byte> getToUpper() {
		return toUpper;
	}

	public void setToUpper(ArrayDeque<Byte> toUpper) {
		this.toUpper = toUpper;
	}

	public ArrayDeque<Frame> getInFlight() {
		return inFlight;
	}

	public void setInFlight(ArrayDeque<Frame> inFlight) {
		this.inFlight = inFlight;
	}

	public ArrayDeque<Frame> getFromLower() {
		return fromLower;
	}

	public void setFromLower(ArrayDeque<Frame> fromLower) {
		this.fromLower = fromLower;
	}

	public ArrayDeque<Frame> getToLower() {
		return toLower;
	}

	public void setToLower(ArrayDeque<Frame> toLower) {
		this.toLower = toLower;
	}

	public void setSeqNo(int seqNo) {
		this.seqNo = seqNo;
	}

	public int getAvailWindow() {
		return availWindow;
	}

	public void setAvailWindow(int availWindow) {
		this.availWindow = availWindow;
	}
}
