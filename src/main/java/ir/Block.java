/* Block.java
 * Abstracts a function block in a network as a node in a directed graph.
 *
 * Written by: Li Hsien Yoong
 */

package ir;

import fb.*;
import java.util.*;

public class Block {
	FBInstance instance;
	Event[] inputEvents;
	Event[] outputEvents;
	VarDeclaration[] inputVars;
	// Kyle: MethodReference[] methodReferences;
	VarDeclaration[] outputVars;
	Block[] successors;
	Arc[] succArcs;
	Block[] predecessors;
	Arc[] predArcs;
	
	// The following are used specifically to handle local communication blocks 
	// in a special way
	String sifbName;
	int sifbIndex;
	int sifbQlen;
	
	public Block(FBInstance i, InterfaceList iface) {
		instance = i;
		inputEvents = iface.getInputEvents();
		outputEvents = iface.getOutputEvents();
		inputVars = iface.getInputVars();
		// Kyle: methodReferences = iface.getMethodReferences();
		outputVars = iface.getOutputVars();
	}
	
	public Block(FBInstance i, InterfaceList iface, String n, int idx, int q) {
		instance = i;
		inputEvents = iface.getInputEvents();
		outputEvents = iface.getOutputEvents();
		inputVars = iface.getInputVars();
		// Kyle: methodReferences = iface.getMethodReferences();
		outputVars = iface.getOutputVars();
		sifbName = n;
		sifbIndex = idx;
		sifbQlen = q;
	}
	
	void connectSuccs(ArrayList<Block> succs, ArrayList<Arc> a) {
		successors = succs.toArray(new Block[0]);
		succArcs = a.toArray(new Arc[0]);
	}
	
	void connectPreds(ArrayList<Block> preds, ArrayList<Arc> a) {
		predecessors = preds.toArray(new Block[0]);
		predArcs = a.toArray(new Arc[0]);
	}
	
	public DataType[] getSuccDataTypes() {
		if (succArcs == null)
			return null;
		
		ArrayList<DataType> data = new ArrayList<DataType>();
		for (int i = 0; i < succArcs.length; i++) {
			if (succArcs[i].destination.sigType != null &&
				succArcs[i].source.signal.startsWith("RD_")) {
				data.add(new DataType(succArcs[i].destination.sigType, 
						              succArcs[i].destination.arraySize));
			}
		}
		if (data.isEmpty())
			return null;
		
		return data.toArray(new DataType[0]);
	}
	
	public DataType[] getPredDataTypes() {
		if (predArcs == null)
			return null;
		
		ArrayList<DataType> data = new ArrayList<DataType>();
		for (int i = 0; i < predArcs.length; i++) {
			if (predArcs[i].source.sigType != null &&
				predArcs[i].destination.signal.startsWith("SD_")) {
				data.add(new DataType(predArcs[i].source.sigType, 
			                          predArcs[i].source.arraySize));
			}
		}
		if (data.isEmpty())
			return null;
		
		return data.toArray(new DataType[0]);
	}
	
	public DataType getSuccDataType(String portName) {
		if (succArcs == null)
			return null;
				
		for (int i = 0; i < succArcs.length; i++) {
			if (succArcs[i].source.getSignal().equals(portName)) {
				DataType data = new DataType(succArcs[i].destination.sigType, 
                                             succArcs[i].destination.arraySize);
				return data;
			}
		}
		return null;
	}
	
	public DataType getPredDataType(String portName) {
		if (predArcs == null)
			return null;
				
		for (int i = 0; i < predArcs.length; i++) {
			if (predArcs[i].destination.getSignal().equals(portName)) {
				DataType data = new DataType(predArcs[i].source.sigType, 
                                             predArcs[i].source.arraySize);
				return data;
			}
		}
		return null;
	}
	
	public Event[] getInputEvents() {
		return inputEvents;
	}
	
	public Event[] getOutputEvents() {
		return outputEvents;
	}
	
	public VarDeclaration[] getInputVars() {
		return inputVars;
	}
	
	public VarDeclaration[] getOutputVars() {
		return outputVars;
	}
	
	public VarDeclaration[] getWithsIEvent(String name) {
		if (inputEvents != null) {
			for (int i = 0; i < inputEvents.length; i++) {
				if (name.equals(inputEvents[i].getName()))
					return inputEvents[i].getWithVar();
			}
		}
		return null;
	}
	
	public VarDeclaration[] getWithsOEvent(String name) {
		if (outputEvents != null) {
			for (int i = 0; i < outputEvents.length; i++) {
				if (name.equals(outputEvents[i].getName()))
					outputEvents[i].getWithVar();
			}
		}
		return null;
	}
	
	public FBInstance getInstance() {
		return instance;
	}
	
	public String getInstanceName() {
		return instance.name;
	}
	
	public String getInstanceType() {
		return instance.type;
	}
	
	// Returns the name of this block. It first checks if this block has an ID 
	// parameter. If so, the value of the ID is returned. Otherwise, the block's
	// instance name is returned.
	public String getIDname() {
		if (instance.params != null) {
			for (int i = 0; i < instance.params.length; i++) {
				if (instance.params[i].name.equals("ID")) {
					String value = instance.params[i].value.replace("\"", "");
					if (!value.isEmpty())
						return value; 
				}
			}
		}
		return instance.name;
	}
	
	public String getSIFBname() {
		return sifbName;
	}
	
	public int getSIFBindex() {
		return sifbIndex;
	}
	
	public int getSIFBQlen() {
		return sifbQlen;
	}
	
	public int hashCode() {
		return sifbIndex + instance.hashCode();
	}
	
	public boolean equals(Object o) {
		if (o instanceof Block) {
			Block b = (Block)o;
			if (instance.equals(b.instance)) {
				if (sifbIndex == b.sifbIndex && sifbQlen == b.sifbQlen)
					return true;
			}
		}
		return false;
	}
	
	public String toString() {
		StringBuilder str = new StringBuilder(instance.toString());
		if (sifbName != null)
			str.append(" " + sifbName + "_" + Integer.toString(sifbIndex) + "\n");
		str.append("  Successors:\n");
		for (int j = 0; j < successors.length; j++) {
			if (successors[j] == null)
				str.append("  [bound to parent]\n");
			else
				str.append("  " + successors[j].instance.toString() + "\n");
			str.append("    " + succArcs[j].toString() + "\n");
		}
		str.append("  Predecessors:\n");
		for (int j = 0; j < predecessors.length; j++) {
			if (predecessors[j] == null)
				str.append("  [bound to parent]\n");
			else
				str.append("  " + predecessors[j].instance.toString() + "\n");
			str.append("    " + predArcs[j].toString() + "\n");
		}
		
		return str.toString();
	}
}
