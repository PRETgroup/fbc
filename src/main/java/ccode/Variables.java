/* Variables.java
 * Helper class to generate global variables for local communication blocks.
 *
 * Written by: Li Hsien Yoong
 */

package ccode;

import fb.Event;
import ir.*;

public class Variables {
	private String sendType = new String();
	private String recvType = new String();
	private Event[] sendIEvents;
	private Event[] sendOEvents;
	private Event[] recvIEvents;
	private Event[] recvOEvents;
	private DataType[] data;
	private int qlen;
	
	public Variables(DataType[] d, int q) {
		data = d;
		qlen = q;
	}
	
	void setSendEvents(Event[] inputs, Event[] outputs) {
		sendIEvents = inputs;
		sendOEvents = outputs;
	}
	
	void setRecvEvents(Event[] inputs, Event[] outputs) {
		recvIEvents = inputs;
		recvOEvents = outputs;
	}
	
	void setSendType(String type) {
		sendType = type;
	}
	
	void setRecvType(String type) {
		recvType = type;
	}
	
	String getSendType() {
		return sendType;
	}
	
	String getRecvType() {
		return recvType;
	}
	
	DataType[] getData() {
		return data;
	}
	
	Event[] getSendIEvents() {
		return sendIEvents;
	}
	
	Event[] getSendOEvents() {
		return sendOEvents;
	}
	
	Event[] getRecvIEvents() {
		return recvIEvents;
	}
	
	Event[] getRecvOEvents() {
		return recvOEvents;
	}
	
	int getQlen() {
		return qlen;
	}
}
