/* KripkeNode.java
 * Represents a node in a Kripke structure
 *
 * Written by: Li Hsien Yoong
 */

package rmc;

import fb.Event;
import ir.*;
import java.util.*;

public class KripkeNode {
	private HashSet<String> dataLabels = new HashSet<String>();
	String name;
	Action[] actions;
	StringBuilder[] dataGuards;
	Transition[] transitions;
	public KripkeNode[] children;
	public KripkeNode[] parents;
	int index;
	ArrayList<Integer> labelIndices = new ArrayList<Integer>();
	
	public KripkeNode(SyncState s) {
		name = s.getName();
		actions = s.actions;
		if (s.conditions != null) {
			//dataGuards = new StringBuilder[s.conditions.length];
			transitions = new Transition[s.conditions.length];
			for (int i = 0; i < transitions.length; i++) {
				//dataGuards[i] = new StringBuilder();
				//transitions[i] = new Transition(dataGuards[i], s.conditions[i]);
				transitions[i] = new Transition(s.conditions[i]);
			}
		}
		if (s.children != null)
			children = new KripkeNode[s.children.length];
		if (s.parents != null)
			parents = new KripkeNode[s.parents.length];
	}
	
	/* Creates the string(s) for the label of this Kripke node */
	ArrayList<String> getLabel() {
		ArrayList<String> label = new ArrayList<String>();
		if (dataLabels != null) {
			Iterator<String> i = dataLabels.iterator();
			while (i.hasNext())
				label.add("<DataCondition>" + i.next() + "</DataCondition>");
		}
		if (actions != null) {
			for (int i = 0; i < actions.length; i++) {
				Procedure proc = actions[i].getProcedure();
				if (proc != null)
					label.add("<Algorithm>" + proc.getFullName() + "</Algorithm>");
				Event e = actions[i].getOutput();
				if (e != null)
					label.add("<Event>" + e.getName() + "</Event>");
			}
		}
		return label;
	}
	
	/*
	private void append(StringBuilder label, String name) {
		if (label.length() > 0)
			label.append(':');
		label.append(name);
	}
	*/
	
	void addLabel(String label) {
		dataLabels.add(label);
	}
	
	void addIndex(int index) {
		labelIndices.add(index);
	}
	
	/* Creates the list of transitions extending from this node */
	String[] getTransitions() {
		if( transitions == null ) return null;
		
		String[] trans = new String[transitions.length];
		for (int i = 0; i < transitions.length; i++) {
			trans[i] = "<Transition Source=\"" + name +  
			           "\" Condition=\"" + transitions[i].getGuard() +  
			           "\" Destination=\"" + children[i].name + "\" />";
		}
		return trans;
	}
}
