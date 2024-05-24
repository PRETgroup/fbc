/* ECTransition.java
 * Defines the ECTransition object.
 * 
 * Written by: Li Hsien Yoong
 */

package fb;

import java.io.BufferedWriter;
import java.io.IOException;

import fbtostrl.FBtoStrl;
import fbtostrl.OutputLevel;
import fbtostrl.OutputManager;

public class ECTransition {
	public String source;
	public String destination;
	public String condition;
	public ECAction[] actions;
	
	public ECTransition(String s, String d, String c) {
		source = s;
		destination = d;
		condition = c;
	}
	
	public ECTransition(String s, String d, String c, ECAction[] a) {
		source = s;
		destination = d;
		condition = c;
		actions = a;
	}
	
	public void print(int indent) {
		Static.format(indent);
		System.out.println("Source: " + source);
		Static.format(indent);
		System.out.println("Destination: " + destination);
		Static.format(indent);
		System.out.println("Condition: " + condition);
	}
	
	public void toXML(BufferedWriter printer, String indent) throws IOException {
		// TODO: placement of transitions
		// temp... pos dependant on 1st char of condition
		if( condition == null )
		{
			OutputManager.printError("", "Condition is null: dest=" + destination.trim() + 
					" src=" + source.trim(), OutputLevel.ERROR);
			return;
		}
		char temp = condition.charAt(0); // condition CANT = null
	
		int x, y;
		x = (temp * 100) % 2000;
		y = x;
		printer.append(indent+"<ECTransition Condition=\""+FBtoStrl.XmlEncode(condition).trim()+"\" Destination=\""+destination.trim()+"\" Source=\""+source.trim()+"\" x=\""+x+"\" y=\""+y+"\" />\n");
		
	}
	
	public String getGuardCondition()
	{
		return condition;
	}
	
	public ECAction[] getActions()
	{
		return actions;
	}

	/**
	 * Gareth Added this to compare transitions FROM different states
	 * @param t
	 * @return
	 */
	public boolean sameTarget(ECTransition t) {
		if( !destination.equals(t.destination) ) return false;
		if( !condition.equals(t.condition) ) return false;
		
		return true;
	}
	
	
	public String toString()
	{
		return source + "-"+condition+"->" + destination;
		
	}
	
	 
}
