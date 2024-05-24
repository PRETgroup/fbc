/* ECState.java
 * Defines the ECState object.
 * 
 * Written by: Li Hsien Yoong
 */

package fb;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

public class ECState {
	public String name;
	public String comment;
	public ECAction[] actions;
	public ECState[] children;
	public ECState[] parents;
	//public String[] conditions;
	
	public ECTransition[] transitions; // For Mealy-Machines
	
	// For HCECC
	public ECState[] parallelStates; // Parallel ECCs
	public HCECC hcecc; // Refining HCECC
	
	public ECState(String n, String c, ECAction[] a) {
		name = n;
		comment = c;
		actions = a;
	}
	
	public ECState(String n, String c, ECAction[] a,
			HCECC refiningHCECC) {
		name = n;
		comment = c;
		actions = a;
		hcecc = refiningHCECC;
	}

	void mapAlgo(Algorithm[] algs) {
		if (actions != null) {
			for (int i = 0; i < actions.length; i++)
				actions[i].mapAlgo(algs);
		}
		if( hcecc != null )
			hcecc.mapAlgo(algs);
	}
	
	public String getName() {
		return name;
	}
	
	public String getComment() {
		return comment;
	}
	
	public void print(int indent) {
		if (name != null) {
			Static.format(indent);
			System.out.println("Name: " + name);
		}
		if (comment != null) {
			Static.format(indent);
			System.out.println("Comment: " + comment);
		}		
		if (actions != null) {
			for (int i = 0; i < actions.length; i++) {
				Static.format(indent);
				System.out.println("ECAction::");
				actions[i].print(indent + 1);
			}
		}
	}
	
	public void printECC(HashSet<ECState> visited) {
		if (visited.contains(this))
			return;
		visited.add(this);
			
		System.out.print(name + " : ");
		if (actions != null) {
			for (int i = 0; i < actions.length; i++)				
				actions[i].printECC();
		}
		
		if (children == null) {
			//assert (conditions == null): "`children\' and `conditions\' diverge.";
			assert (transitions == null): "`children\' and `transitions\' diverge.";
			System.out.println("\n- ");
		} else {
			//assert (children.length == conditions.length): "`children\' and `conditions\' diverge.";
			assert (children.length == transitions.length): "`children\' and `transitions\' diverge.";
			System.out.print("\n- ");
			for (int i = 0; i < children.length; i++) {
				//System.out.print(children[i].name + "<" + conditions[i] + ">");
				System.out.print(children[i].name + "<" + transitions[i].condition + ">");
				if (i < children.length - 1)
					System.out.print(", ");
				else
					System.out.print("\n\n");
			}
			
			// Recursively print out all children
			for (int i = 0; i < children.length; i++)
				children[i].printECC(visited);
		}
	}
	
	public void toXML(BufferedWriter printer, String indent) throws IOException {
//		 TODO: placement of states
		// temp... pos dependant on 1st char of condition
		char temp = name.charAt(0); // name CANT = null
		int x, y;
		x = (temp * 100) % 2000;
		y = x;
		printer.append(indent+"<ECState Comment=\""+comment+"\" Name=\""+name+"\" x=\""+x+"\" y=\""+y+"\" ");
		if( actions != null )
		{
			printer.append(">\n");
			for(ECAction act : actions)
			{
				if( act != null)
					act.toXML(printer, indent+"\t");
			}
			printer.append(indent+"</ECState>\n");
		}
		else
			printer.append("/>\n");
		
	}
	
	public String toString()
	{
		return "ECState: " + this.name;
	}
}
