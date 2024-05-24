/* InterfaceList.java
 * Defines the InterfaceList object (list of I/O events and I/O variables in 
 * function blocks).
 * 
 * Written by: Li Hsien Yoong
 */

package fb;

import java.io.BufferedWriter;
import java.io.IOException;

public class InterfaceList {
	public Event[] eventInputs;
	public Event[] eventOutputs;
	public VarDeclaration[] inputVars;
	// Kyle: public MethodReference[] methodReferences;
	public VarDeclaration[] outputVars;
	public Plug[] plugs;
	public Socket[] sockets;
	// public HashMap<Event, VarDeclaration[]> inputWiths;
	// public HashMap<Event, VarDeclaration[]> outputWiths;
	
	public InterfaceList(Event[] ei, Event[] eo, VarDeclaration[] vi, VarDeclaration[] vo, // Kyle: MethodReference[] m,
			Plug[] p, Socket[] s) {
		eventInputs = ei;
		eventOutputs = eo;
		inputVars = vi;
		outputVars = vo;
		// Kyle: methodReferences = m;
		
		if (eventInputs != null && inputVars != null) {
			// inputWiths = buildWithMap(eventInputs, inputVars);
			for (int i = 0; i < eventInputs.length; i++)
				eventInputs[i].bindDataVars(inputVars);
		}
		
		if (eventOutputs != null && outputVars != null) {
			// outputWiths = buildWithMap(eventOutputs, outputVars);
			for (int i = 0; i < eventOutputs.length; i++)
				eventOutputs[i].bindDataVars(outputVars);
		}
		
		plugs = p;
		sockets = s;
	}
	
	public InterfaceList(VarDeclaration[] vi) {
		inputVars = vi;
	}
	
	public Event[] getInputEvents() {
		return eventInputs;
	}
	
	public Event[] getOutputEvents() {
		return eventOutputs;
	}
	
	public VarDeclaration[] getInputVars() {
		return inputVars;
	}
	
	/* Kyle:
	public MethodReference[] getMethodReferences() {
		return methodReferences;
	}
	*/
	
	public VarDeclaration[] getOutputVars() {
		return outputVars;
	}
	
	/*
	private HashMap<Event, VarDeclaration[]> 
			buildWithMap(Event[] events, VarDeclaration[] data) {
		HashMap<Event, VarDeclaration[]> withs = new HashMap<Event, VarDeclaration[]>();
		
		// Loop through all events
		for (int i = 0; i < events.length; i++) {
			if (events[i].with == null)
				continue;
			VarDeclaration[] vars = new VarDeclaration[events[i].with.length];
			// Loop through all with-associations for the event
			for (int j = 0; j < events[i].with.length; j++) {				
				for (int k = 0; k < data.length; k++) {					
					if (events[i].with[j].compareTo(data[k].name) == 0) {
						vars[j] = data[k];
						break;
					}
				}				
			}
			withs.put(events[i], vars);
		}
		return withs;
	}
	*/
	
	public void print(int indent) {
		if (eventInputs != null) {
			Static.format(indent);
			System.out.println("EventInputs::");
			for (int i = 0; i < eventInputs.length; i++) {
				Static.format(indent + 1);
				System.out.println("Event::");
				eventInputs[i].print(indent + 2);
			}
		}
		
		if (eventOutputs != null) {
			Static.format(indent);
			System.out.println("EventOutputs::");
			for (int i = 0; i < eventOutputs.length; i++) {
				Static.format(indent + 1);
				System.out.println("Event::");
				eventOutputs[i].print(indent + 2);
			}
		}
		
		if (inputVars != null) {
			Static.format(indent);
			System.out.println("InputVars::");
			for (int i = 0; i < inputVars.length; i++) {
				Static.format(indent + 1);
				System.out.println("VarDeclaration::");
				inputVars[i].print(indent + 2);
			}
		}
		
		if (outputVars != null) {
			Static.format(indent);
			System.out.println("OutputVars::");
			for (int i = 0; i < outputVars.length; i++) {
				Static.format(indent + 1);
				System.out.println("VarDeclaration::");
				outputVars[i].print(indent + 2);
			}
		}
	}
	
	public void toXML(BufferedWriter printer, String indent) throws IOException {
		printer.write(indent+"<InterfaceList>\n");
		if( eventInputs != null )
		{
			printer.write(indent+"\t<EventInputs>\n");
			for( Event evt : eventInputs)
			{
				evt.toXML(printer, indent+"\t\t");
			}
			printer.write(indent+"\t</EventInputs>\n");
		}
		
		if( eventOutputs != null )
		{
			printer.write(indent+"\t<EventOutputs>\n");
			for( Event evt : eventOutputs)
			{
				evt.toXML(printer, indent+"\t\t");
			}
			printer.write(indent+"\t</EventOutputs>\n");
		}
		
		if( inputVars != null )
		{
			printer.write(indent+"\t<InputVars>\n");
			for( VarDeclaration var : inputVars)
			{
				var.toXML(printer, indent+"\t\t");
			}
			printer.write(indent+"\t</InputVars>\n");
		}
		
		if( outputVars != null )
		{
			printer.write(indent+"\t<OutputVars>\n");
			for( VarDeclaration var : outputVars)
			{
				var.toXML(printer, indent+"\t\t");
			}
			printer.write(indent+"\t</OutputVars>\n");
		}
		
		printer.write(indent+"</InterfaceList>\n");
		
	}
}