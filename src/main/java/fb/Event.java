/* Event.java
 * Defines the Event object.
 * 
 * Written by: Li Hsien Yoong
 */

package fb;

import java.io.BufferedWriter;
import java.io.IOException;

public class Event {
	String name;						// name of the event
	String comment;						// comment for the event
	public String[] with;				// array of data associations
	public VarDeclaration[] withVar;	// data associations with the variable objects
	
	public Event(String n, String c, String[] w) {
		name = n;
		comment = c;
		with = w;
	}
	
	public Event(String n) {
		name = n;
	}
	
	//public void changeName(String newName) {
	//	name = newName;
	//}
	
	public String getName() {
		return name;
	}
	
	public String getComment() {
		return comment;
	}
	
	public String[] getWiths() {
		return with;
	}
	
	public VarDeclaration[] getWithVar() {
		return withVar;
	}
	
	void bindDataVars(VarDeclaration[] vars) {		
		if (with == null)
			return;
		withVar = new VarDeclaration[with.length];
			
		// Loop through all with-associations for the event
		for (int j = 0; j < with.length; j++) {				
			for (int k = 0; k < vars.length; k++) {					
				if ( with[j].equals(vars[k].name) ) {
					withVar[j] = vars[k];
					break;
				}
			}				
		}
	}
	
	public void print(int indent) {
		Static.format(indent);
		System.out.println("Name: " + name);
		if (comment != null) {
			Static.format(indent);
			System.out.println("Comment: " + comment);
		}
		if (with != null) {
			for (int i = 0; i < with.length; i++) {
				Static.format(indent);
				System.out.println("With: " + with[i]);
			}
		}
	}
	
	public void toXML(BufferedWriter printer, String indent) throws IOException {
		printer.write(indent+"<Event ");
		printer.write("Name=\""+name+"\" ");
		if( comment != null )
			printer.write("Comment=\""+comment+"\" ");
		if( with != null )
		{
			printer.write(">\n");
			for( String var : with )
			{
				printer.write(indent+"\t<With Var=\""+var+"\"/>\n");
			}
			printer.write(indent+"</Event>\n");
		}
		else
			printer.write("/>\n");
		
	}
	
	public String toString()
	{
		return name;
	}
}