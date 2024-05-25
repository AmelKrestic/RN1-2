package de.ipvs.rni.exercise2.layers;

import de.ipvs.rni.exercise2.common.Application;
import de.ipvs.rni.exercise2.common.ProcessEvents;
import de.ipvs.rni.exercise2.common.Utils;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * This class represents the application layer.
 * 
 * An Application can bind to the application layer for network communication.
 * The application layer provides the application with three methods for message
 * communication: 'sendMessage', 'hasMessage', and 'popMessage'.
 *
 */
public class ApplicationLayer implements ProcessEvents {

	public final static byte END = (byte) 0xAE;

	// Input Queue/Buffer, to store messages from the application.
	private ArrayDeque<Serializable> fromUpper = new ArrayDeque<>();
	// Ouput Buffer, where the appliaction layer forwards an received object/message
	// to the application.
	private ArrayDeque<Object> toUpper = new ArrayDeque<>();
	// Input Buffer, where the appliaction layer receives new bytes from its lower
	// layer.
	private ArrayDeque<Byte> fromLower;
	// Output Buffer, where the appliaction layer forwards its data to next lower
	// layer.
	private ArrayDeque<Byte> toLower;
	// Buffer for aggregating bytes before object creation.
	private ArrayDeque<Byte> messageBuffer = new ArrayDeque<>();

	/**
	 * Bind an app to the application layer.
	 * 
	 * @param app An application which should be bound to this layer
	 */
	public void bind(Application app) {
		app.setApplicationLayer(this);
	}

	/**
	 * Transform an object into an byte array/buffer/queue. Conversion via Utils
	 * class, adding END flag at the end
	 * 
	 * @param obj Any object which implements the Serializable interface
	 * @return
	 */
	public ArrayDeque<Byte> transformMessageForSending(Serializable obj) {
		// TODO
		ArrayDeque<Byte> temp = Utils.objToBytes(obj);
		temp.add(END);
		return temp;
	}

	/**
	 * Transforms the input bytes back into an object to be handed over to the next
	 * layer. Removal of END flag.
	 * 
	 * @param byteInput
	 * @return the object reconstructed
	 */
	public Object transformMessageForReceiving(ArrayDeque<Byte> byteInput) {
		byteInput.removeLast();
		Object o=Utils.bytesToObj(byteInput);
		byteInput.clear();
		return o;

	}

	/**
	 * Forward bytes to next lower layer
	 * 
	 * @param byteBuffer Source Byte Buffer
	 * @param nextLayer  Destination Byte Buffer
	 */
	public void sendMessageBytes(ArrayDeque<Byte> byteBuffer, ArrayDeque<Byte> nextLayer) {
		nextLayer.addAll(byteBuffer);
	}

	/**
	 * Handles communication between layers. First loop converts objects from
	 * application queue to byte queues and adds them to toLower. Second loop reads
	 * out byte queue from lower layer and separates them into objects with END
	 * character.
	 */
	@Override
	public void process() {

		while (!fromUpper.isEmpty()) {
			Serializable obj = fromUpper.poll();
			ArrayDeque<Byte> msg = transformMessageForSending(obj);
			toLower.addAll(msg);
		}
		while (!fromLower.isEmpty()) {
			ArrayDeque<Byte> msg = messageBuffer;
			Byte b = null;
			while (!fromLower.isEmpty()) {
				b = fromLower.poll();
				msg.add(b);
				if (b == END) {
					break;
				}
			}
			if (b != END) {
				messageBuffer = msg;
				break;
			}
			Object obj = transformMessageForReceiving(msg);
			toUpper.add(obj);
			messageBuffer = new ArrayDeque<>();
		}

	}

	/**
	 * Send an object which implements the interface Serializable.
	 * 
	 * @param obj
	 */
	public void sendMessage(Serializable obj) {
		fromUpper.offer(obj);
	}

	/**
	 * Probe if we received an object/message.
	 * 
	 * @return
	 */
	public boolean hasMessage() {
		return !toUpper.isEmpty();
	}

	/**
	 * Get new object/message and remove it from the application layer queue/buffer.
	 * 
	 * @return
	 */
	public Object pollMessage() {
		return toUpper.poll();
	}

	/**
	 * Check if byteBuffer has an end delimiter.
	 * 
	 * This method assumes that the byte buffer has stuffed bytes .
	 * 
	 * @param byteBuffer
	 * @return
	 */
	public ArrayDeque<Serializable> getFromUpper() {
		return fromUpper;
	}

	public void setFromUpper(ArrayDeque<Serializable> fromUpper) {
		this.fromUpper = fromUpper;
	}

	public ArrayDeque<Object> getToUpper() {
		return toUpper;
	}

	public void setToUpper(ArrayDeque<Object> toUpper) {
		this.toUpper = toUpper;
	}

	public ArrayDeque<Byte> getFromLower() {
		return fromLower;
	}

	public void setFromLower(ArrayDeque<Byte> fromLower) {
		this.fromLower = fromLower;
	}

	public ArrayDeque<Byte> getToLower() {
		return toLower;
	}

	public void setToLower(ArrayDeque<Byte> toLower) {
		this.toLower = toLower;
	}
}
