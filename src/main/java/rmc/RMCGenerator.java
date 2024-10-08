/* RMCGenerator.java
 * Code generator for Roopal's module checker
 *
 * Written by: Li Hsien Yoong
 */

package rmc;

import java.io.*;
import java.util.*;

import esterel.*;
import fbtostrl.Options;
import ir.*;

public class RMCGenerator extends CodeGenerator {
	private String parentsName;
	private ArrayList<KripkeNode> nodes = new ArrayList<KripkeNode>();
	private HashSet<String> labels = new HashSet<String>();
	
	public RMCGenerator(FunctionBlock f, Options o, LinkedList<String> files, 
			            String n, String pn, LinkedList<CodeGenerator> cgs, String p) {
		super(f, o, files, n, pn, cgs);
		parentsName = p;
	}
	
	/* DESCRIPTION: Generates code for this module
	 * PARAMETERS: fileName - base name of the output file(s) produced
	 *             top - true if this is the top-level file, otherwise false
	 *             comBlocks - [not used]
	 * RETURNS: Null if code was generated; otherwise, the code generator itself
	 */
	public CodeGenerator generateCode(String fileName, boolean top, 
			HashSet<Block> comBlocks) throws IOException {
		if (sifbInfo != null) {
			// Can't check service interface function blocks
		}
		else {
			String name = fileName + ".rmc";			
			String fbName = fb.getCompiledType();
			Composite compositeFB = fb.getCompositeFB();
			if (compositeFB == null) {
				if (fb.getBasicFB() != null) {
					printer = new CodePrinter(name, 2);
					fileNames.add(name);
					printer.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
					printer.print("<!-- This file is generated by FBC. -->");
					printer.print("<FB Type=\"" + fbName + "\" Name=\"" + parentsName + insName + "\">");
					printer.indent();
					
					generateBasicFB(fbName);
					
					printer.unindent();
					printer.print("</FB>");
					printer.close();
				}					
				else {
					// Can't check basic function blocks with no ECC
				}
			}
			else {
				printer = new CodePrinter(name, 2);
				fileNames.add(name);
				printer.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
				printer.print("<!-- This file is generated by FBC. -->");
				printer.print("<FB Type=\"" + fbName + "\" Name=\"" + parentsName + insName + "\">");
				printer.indent();
				
				generateInterface();
				printComposite();
				
				printer.unindent();
				printer.print("</FB>");
				printer.close();
			}
		}
		return null;
	}
	
	/* Returns a set containing all declarations for toANY converter functions */
	protected HashSet<String> getToAnyConverterDecls() {
		HashSet<String> decls = new HashSet<String>();
		int index = sifbInfo.getIndex();
		for (int i = 0; i < index; i++) {
			String[] func = sifbInfo.getToAnyConverterDecl(i);
			decls.add("ANY " + func[0] + "(" + getDataType(func[1]) + ");");
		}
		return decls;
	}
	
	/* Returns a set containing all declarations for ANYto converter functions */
	protected HashSet<String> getAnyToConverterDecls() {
		HashSet<String> decls = new HashSet<String>();
		int index = sifbInfo.getIndex();
		for (int i = 0; i < index; i++) {
			String[] func = sifbInfo.getAnyToConverterDecl(i);
			decls.add(getDataType(func[1]) + " " + func[0] + "(ANY);");
		}
		return decls;
	}
	
	/* Generates the Kripke structure representation for a basic function block */
	protected void generateBasicFB(String fbName) throws IOException {		
		formatStateTree(rootState);
		visited.clear();
		buildKripkeStruct(nodes.get(0), rootState);
		/*
		Iterator<KripkeNode> i = nodes.iterator();
		while (i.hasNext()) {
			KripkeNode node = i.next();
			if (node.children != null) {
				assert (node.dataGuards.length == node.children.length): 
					"Data guards and children length do not match.\n";
				for (int j = 0; j < node.children.length; j++) {
					if (node.dataGuards[j].length() > 0)
						node.children[j].addLabel(node.dataGuards[j].toString());
				}
			}
		}
		*/
		
		printer.print("<InitialState>");
		printer.indent();
		printer.print("<State>" + nodes.get(0).name + "</State>");
		printer.unindent();
		printer.print("</InitialState>");
		
		generateInterface();
		makeLabelsSet();
				
		printer.print("<Labels>");
		printer.indent();
		Iterator<String> k = labels.iterator();
		while (k.hasNext())	
			printer.print(k.next());
		printer.unindent();
		printer.print("</Labels>");
		
		printer.print("<Transitions>");
		printer.indent();
		Iterator<KripkeNode> i = nodes.iterator();
		while (i.hasNext()) {
			String[] trans = i.next().getTransitions();
			if( trans == null )
				continue; // skip if no transitions
			for (int j = 0; j < trans.length; j++)
				printer.print(trans[j]);
		}
		printer.unindent();
		printer.print("</Transitions>");
		
		printer.print("<Propositions>");
		printer.indent();
		i = nodes.iterator();
		while (i.hasNext()) {
			KripkeNode node = i.next();
			if (node.labelIndices.isEmpty())
				continue;
			printer.print("<Proposition>");
			printer.indent();
			printer.print("<State>" + node.name + "</State>");
			Iterator<Integer> j = node.labelIndices.iterator();
			while(j.hasNext())
				printer.print("<Label ID=\"" + j.next().toString() + "\" />");
			printer.unindent();
			printer.print("</Proposition>");
		}
		printer.unindent();
		printer.print("</Propositions>");
	}
	
	/* Generates the function block's input/output interface */
	protected void generateInterface() throws IOException {
		HashSet<String> dataset = getAbstractedDataEvents();
		
		// Print out all input events
		if (inputEvents != null || !dataset.isEmpty()) {
			printer.print("<InputSignals>");
			printer.indent();
			if (inputEvents != null) {
				for (int j = 0; j < inputEvents.length; j++)
					printer.print("<Signal>" + inputEvents[j].getName() + "</Signal>");
			}
			if (Transition.isEventless())
				printer.print("<Signal>" + "." + "</Signal>");
			
			// Print out all input events from abstracted data conditions
			Iterator<String> i = dataset.iterator();
			while (i.hasNext()) {
				printer.print("<Signal>" + i.next() + "</Signal>");
			}
			printer.unindent();
			printer.print("</InputSignals>");
		}
		
		// Print out all output events
		if (outputEvents != null) {
			printer.print("<OutputSignals>");
			printer.indent();
			for (int j = 0; j < outputEvents.length; j++)
				printer.print("<Signal>" + outputEvents[j].getName() + "</Signal>");
			printer.unindent();
			printer.print("</OutputSignals>");
		}
	}
		
	/* Returns the set of input events derived from abstracted data conditions */
	private HashSet<String> getAbstractedDataEvents() {
		HashSet<String> dataset = new HashSet<String>();
		Iterator<KripkeNode> k = nodes.iterator();
		while (k.hasNext()) {
			KripkeNode node = k.next();

			if(node.transitions != null) {
				for (int i = 0; i < node.transitions.length; i++) {
					ArrayList<StringBuilder> data = node.transitions[i].getDataConditions();
					Iterator<StringBuilder> j = data.iterator();
					while (j.hasNext())
						dataset.add(j.next().toString());
				}			
			}
		}
		return dataset;
	}
	
	/* Generates the input/output relationship between signal ports */
	protected void printComposite() throws IOException {
		if (runs.length <= 0)
			return;
		SignalTable sigTable = new SignalTable(runs, fb.sigNames, deferredBlocks);
		
		// Print out the signal aliases
		printer.print("<LocalSignals>");
		printer.indent();
		
		Collection<SignalInfo> entries = sigTable.getUniqueSigBindings();
		Iterator<SignalInfo> i = entries.iterator();
		while (i.hasNext()) {
			SignalInfo entry = i.next();
			PortType pt = entry.getPortType();
			if (pt == PortType.INPUT || pt == PortType.OUTPUT)
				continue;
			if (entry.getSigType() == null)
				printer.print("<Signal>" + entry.getSignal() + "</Signal>");
		}
		printer.unindent();
		printer.print("</LocalSignals>");
		
		// Print out the signal bindings for each function block
		printer.print("<SignalBindings>");
		printer.indent();
		for (int j = 0; j < runs.length; j++) {
			String compiledType = runs[j].getType();
			printer.print("<Component Type=\"" + compiledType + "\" Name=\"" + runs[j].getName() + "\">");
			printer.indent();
									
			// Print out all bound signals
			HashSet<PortName> ios = fb.getSubBlockPortSet(compiledType);
			Set<Signal> signals = runs[j].getSignalSet();
			Iterator<Signal> k = signals.iterator();
			while (k.hasNext()) {
				Signal sig = k.next();
				if (sig.getSigType() == null) {
					String subIOName = sig.getSignal();
					SignalInfo bind = sigTable.getBinding(sig);
					printer.print("<Binding>");
					printer.indent();
					printer.print("<Name>" + subIOName + "</Name>");
					printer.print("<Alias>" + bind.getSignal() + "</Alias>");
					printer.unindent();
					printer.print("</Binding>");
					removePort(ios, subIOName);
				}
			}
			
			// Print out signals that are null bound
			Iterator<PortName> io = ios.iterator();
			while (io.hasNext()) {
				PortName p = io.next();
				if (p.getSigType() == null) {
					printer.print("<Binding>");
					printer.indent();
					printer.print("<Name>" + p.getSignal() + "</Name>");
					printer.print("<Alias/>");
					printer.unindent();
					printer.print("</Binding>");
				}
			}
			
			printer.unindent();
			printer.print("</Component>");
		}
		
		Set<Signal> signals = sigTable.getAuxSignalSet();
		Iterator<Signal> k = signals.iterator();
		if (k.hasNext()) {
			printer.print("<Component Name=\"\">");
			printer.indent();
			
			while (k.hasNext()) {
				Signal sig = k.next();
				if (sig.getSigType() == null) {
					printer.print("<Binding>");
					printer.indent();
					printer.print("<Name>" + sig.getSignal() + "</Name>");
					HashSet<Signal> sigSet = sigTable.getAuxRelationSet(sig);
					Iterator<Signal> it = sigSet.iterator();
					while (it.hasNext()) {
						printer.print("<Alias>" + it.next().getSignal() + "</Alias>");
					}
					printer.unindent();
					printer.print("</Binding>");
				}
			}
			
			printer.unindent();
			printer.print("</Component>");
		}
		printer.unindent();
		printer.print("</SignalBindings>");
	}
	
	/* DESCRIPTION: Formats the synchronous state tree for printing 
	 * PARAMETERS: state - currently visited state 
	 */
	protected void formatStateTree(SyncState state) {
		if (visited.contains(state))
			return;
		visited.add(state);
		
		state.index = stateIndex;
		stateIndex++;
		KripkeNode node = new KripkeNode(state);
		node.index = state.index;
		nodes.add(node);
		
		if (state.children == null)
			return;
		
		for (int i = 0; i < state.children.length; i++)
			formatStateTree(state.children[i]);
	}
	
	/* Builds the Kripke structure corresponding to the given SyncState graph */
	private void buildKripkeStruct(KripkeNode node, SyncState state) {
		if (visited.contains(state))
			return;
		visited.add(state);
		
		if (state.parents != null) {
			for (int i = 0; i < state.parents.length; i++)
				node.parents[i] = nodes.get(state.parents[i].index);
		}
		
		if (state.children != null) {		
			for (int i = 0; i < state.children.length; i++)
				node.children[i] = nodes.get(state.children[i].index);
			for (int i = 0; i < state.children.length; i++)
				buildKripkeStruct(node.children[i], state.children[i]);
		}
	}
	
	/* Removes the signal named "sigName" from the set "ios" */
	void removePort(HashSet<PortName> ios, String sigName) {
		Iterator<PortName> i = ios.iterator();
		while (i.hasNext()) {
			if (i.next().getSignal().equals(sigName)) {
				i.remove();
				return;
			}
		}
	}
	
	/* Make set of all possible labels */
	private void makeLabelsSet() {
		int totalLabels = 0;
		HashMap<String, Integer> labelMap = new HashMap<String, Integer>();
		Iterator<KripkeNode> i = nodes.iterator();
		while (i.hasNext()) {
			KripkeNode node = i.next();
			ArrayList<String> label = node.getLabel();
			if (!label.isEmpty()) {
				Iterator<String> j = label.iterator();
				while (j.hasNext()) {
					String l = j.next();
					if (!labelMap.containsKey(l)) {
						labelMap.put(l, totalLabels);
						node.addIndex(totalLabels);
						totalLabels++;
					}
					else
						node.addIndex(labelMap.get(l));
				}
			}
		}
		
		Iterator<String> j = labelMap.keySet().iterator();
		while (j.hasNext()) {
			String label = j.next();
			Integer index = labelMap.get(label);
			StringBuilder l = new StringBuilder(label);
			l.insert(label.indexOf('>'), " ID=\"" + index.toString() + "\"");
			labels.add(l.toString());
		}
	}
	
	/* DESCRIPTION: Matches ST data types with C primitive types. If a match is   
	 *              not found, a user-defined type is assumed.
	 * ARGUMENTS: type - the data type to be matched
	 * RETURNS: The equivalent C type
	 */
	protected static String getDataType(String type) {
		if (type.equalsIgnoreCase("BOOL"))
			return "bool";
		else if (type.equalsIgnoreCase("SINT"))
			return "char";
		else if (type.equalsIgnoreCase("INT"))
			return "short";
		else if (type.equalsIgnoreCase("DINT"))
			return "int";
		else if (type.equalsIgnoreCase("LINT"))
			return "long long";
		else if (type.equalsIgnoreCase("USINT"))
			return "unsigned char";
		else if (type.equalsIgnoreCase("UINT"))
			return "unsigned short";
		else if (type.equalsIgnoreCase("UDINT"))
			return "unsigned int";
		else if (type.equalsIgnoreCase("ULINT"))
			return "unsigned long long";
		else if (type.equalsIgnoreCase("REAL"))
			return "float";
		else if (type.equalsIgnoreCase("LREAL"))
			return "double";
		else if (type.equalsIgnoreCase("STRING"))
			return "char*";
		else if (type.equalsIgnoreCase("BYTE"))
			return "unsigned char";
		else if (type.equalsIgnoreCase("WORD"))
			return "unsigned short";
		else if (type.equalsIgnoreCase("DWORD"))
			return "unsigned int";
		else if (type.equalsIgnoreCase("LWORD"))
			return "unsigned long long";
		else 
			return type;
	}
	
	/* DESCRIPTION: Prints out the code for each synchronous state
	 * PARAMETERS: state - the state currently being visited
	 */ 
	protected void printState(SyncState state, int numOfStates) throws IOException {}
}
