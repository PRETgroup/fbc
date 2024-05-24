/**
 * Helper class to represent a message and the IDs of the nodes exchanging this message.
 * @author lihsien
 */

package ccode;

import java.util.ArrayList;

public class ComMessage {
	private ArrayList<Integer> nodeIDs = new ArrayList<Integer>();
	private String message;
	
	ComMessage(String m) {
		message = m;
	}
	
	public final void addNodeID(int id) {
		nodeIDs.add(new Integer(id));
	}
	
	public final boolean noPartnerNode() {
		return nodeIDs.isEmpty();
	}
	
	public final int[] getNodeIDs() {
		int[] array = new int[nodeIDs.size()];
		for (int i = 0; i < array.length; i++)
			array[i] = nodeIDs.get(i);
		return array;
	}
	
	public final ArrayList<Integer> getArrayListOfIDs() {
		return nodeIDs;
	}
	
	public final int getNumOfMsgPartners() {
		return nodeIDs.size();
	}
	
	public final String getMessage() {
		return message;
	}
	
	public final boolean msgEquals(ComMessage cm) {
		return message.equals(cm.message);
	}
	
	public String toString() {
		return message;
	}
}
