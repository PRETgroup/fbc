/* ECC.java
 * Defines the ECC object.
 * 
 * Written by: Li Hsien Yoong
 */

package fb;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

import fbtostrl.FBtoStrl;

public class ECC {
	public ECState[] states;
	public ECTransition[] transitions;
	ECState root;
	public String name; // Optional / REQUIRED for HCECCs
	
	public ECC(String n, ECState[] s, ECTransition[] t) {
		name = n;
		states = s;
		transitions = t;
		
		if (states == null) 
			return;
		root = states[0];
		
		// Use transitions to build ECState children
		for (int k = 0; k < states.length; k++) {
			if (transitions == null)
				break;
			int countChildren = 0;
			int countParents = 0;

			for (int i = 0; i < transitions.length; i++) {			
				if (states[k].name.equals(transitions[i].source))
					countChildren++;
				if (states[k].name.equals(transitions[i].destination))
					countParents++;
			}
			if (countChildren > 0) {
				states[k].children = new ECState[countChildren];
				//states[k].conditions = new String[countChildren];
				states[k].transitions = new ECTransition[countChildren];
				int j = 0;
				for (int i = 0; i < transitions.length; i++) {			
					if (states[k].name.equals(transitions[i].source)) {
						states[k].children[j] = getECState(transitions[i].destination);
						//states[k].conditions[j] = transitions[i].condition;
						states[k].transitions[j] = transitions[i];
						j++;
					}
				}
			}
			if (countParents > 0) {
				states[k].parents = new ECState[countParents];
				int j = 0;
				for (int i = 0; i < transitions.length; i++) {			
					if (states[k].name.equals(transitions[i].destination)) {
						states[k].parents[j] = getECState(transitions[i].source);
						j++;
					}
				}
			}
		}
	}
	
	// Returns the ECState object for a given state named "name". 
	private ECState getECState(final String name) {
		for (int i = 0; i < states.length; i++) {
			if (states[i].name.compareTo(name) == 0)
				return states[i];
		}
		assert false: "Destination state \'" + name + "\' not found.";
		return null;
	}
	
	public void mapAlgo(Algorithm[] algs) {
		for (int i = 0; i < states.length; i++)
			states[i].mapAlgo(algs);
	}
	
	public void print(int indent) {
		if (states != null) {
			for (int i = 0; i < states.length; i++) {
				Static.format(indent);
				System.out.println("ECState::");
				states[i].print(indent + 1);
			}
		}
		if (transitions != null) {
			for (int i = 0; i < transitions.length; i++) {
				Static.format(indent);
				System.out.println("ECTransition::");
				transitions[i].print(indent + 1);
			}
		}
	}
	
	public void printECC() {
		root.printECC(new HashSet<ECState>());
	}
	
	public ECState getStateByName(String name)
	{
		for( ECState s : states)
		{
			if( s.name.equals(name) )
				return s;
		}
		return null;
	}
	
	public void toXML(BufferedWriter printer, String indent) throws IOException {
		printer.append(indent+"<ECC");
		if( !FBtoStrl.opts.standardXMLOnly && name != null )
			printer.append(" Name=\""+name+"\"");
		printer.append(">\n");
		for( ECState state : states)
		{
			state.toXML(printer, indent+"\t");
		}
		for( ECTransition tran : transitions)
		{
			tran.toXML(printer, indent+"\t");
		}
		printer.append(indent+"</ECC>\n");
	}
	
	public String getRootName()
	{
		return root.name;
	}

	public void getECCNames(LinkedList<String> eccNames) {
		eccNames.add(name); // self
		
		for( ECState state : states) // Refining HCECCs
		{
			if( state.hcecc != null )
				state.hcecc.getECCNames(eccNames);
		}
	}
	
	public boolean isRefined()
	{
		for( ECState state : states) // Refining HCECCs
		{
			if( state.hcecc != null )
				return true;
		}
		return false;
	}
	
	public String toString()
	{
		if( name != null )
			return "ECC: " + name;
		return "ECC S#: "+ states.length + " T#: " + transitions.length;
	}
}
