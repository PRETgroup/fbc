/* StrlGenerator.java
 * Esterel code generator
 *
 * Written by: Li Hsien Yoong
 */

package esterel;

import java.io.*;
import java.util.*;

import fbtostrl.*;
import fb.*;
import ir.*;

public class StrlGenerator extends CodeGenerator {
	private boolean needLoop;
	private VarType stateVar;				// state variable to emulate goto behaviour
	private AlgIntf intf = new AlgIntf();	// data interface
	
	public StrlGenerator(FunctionBlock f, Options o, LinkedList<String> files, 
			             String n, String pn, LinkedList<CodeGenerator> cgs) {
		super(f, o, files, n, pn, cgs);
	}
	
	/* DESCRIPTION: Generates the Esterel code for this module
	 * PARAMETERS: fileName - base name of the output file(s) produced
	 *             top - true if this is the top-level file, otherwise false
	 *             comBlocks - [not used]
	 * RETURNS: Null if code was generated; otherwise, the code generator itself
	 */
	public CodeGenerator generateCode(String fileName, boolean top, 
			HashSet<Block> comBlocks) throws IOException {
		myFileName = fileName;
		
		if (sifbInfo != null) {
			return retrieveSIFB(fileName, top) ? null : this;
		}
		else {
			String name = fileName + ".strl";
			printer = new CodePrinter(name);
			fileNames.add(name);
			printer.print("// This file is generated by FBC.", 1);
			String comment = fb.getComment();
			if (comment != null) {
				if (!comment.equals(""))
					printer.print("/* " + comment + "*/");
			}
			printer.print("module " + fb.getCompiledType() + ":");
			printer.indent();
		
			generateHostTypes();

			Composite compositeFB = fb.getCompositeFB();
			if (compositeFB == null) {
				if (fb.getBasicFB() != null)
					generateBasicFB(fb.getCompiledType());
				else
					generateInterface(fb.getCompiledType());
			}
			else {
				generateInterface(fb.getCompiledType());
				printComposite();
				
				// Print out all deferred blocks
				Iterator<CodeGenerator> i = deferredBlocks.iterator();
				while (i.hasNext()) {
					CodeGenerator cg = i.next();
					cg.generateCode(cg.getFileName(), false, null);
				}
			}

			printer.unindent();
			printer.print("end module", 1);
			
			if (compositeFB == null) {
				if (procedures != null) {
					printAlgs();
					//printCAlgs(fileName, fileNames);
				}

				if (!intf.isEmpty())				
					generateAlgInterface();
			}
			
			printer.close();
		}
		return null;
	}
	
	/* Returns a set containing all declarations for toANY converter functions */
	protected HashSet<String> getToAnyConverterDecls() {
		HashSet<String> decls = new HashSet<String>();
		int index = sifbInfo.getIndex();
		for (int i = 0; i < index; i++) {
			String[] func = sifbInfo.getToAnyConverterDecl(i);
			decls.add("\thost function " + func[0] + "(" + getDataType(func[1]) + ") : ANY;");
		}
		return decls;
	}
	
	/* Returns a set containing all declarations for ANYto converter functions */
	protected HashSet<String> getAnyToConverterDecls() {
		HashSet<String> decls = new HashSet<String>();
		int index = sifbInfo.getIndex();
		for (int i = 0; i < index; i++) {
			String[] func = sifbInfo.getAnyToConverterDecl(i);
			decls.add("\thost function " + func[0] + "(ANY) : " + getDataType(func[1]) + ";");
		}
		return decls;
	}
	
	/* Generates the module interface */
	protected void generateAlgInterface() throws IOException {
		printer.print("interface " + intf.name + ":");
		printer.indent();
		ListIterator<VarType> i = intf.listIterator();
		while (i.hasNext()) {
			printer.print(makeIntfDeclaration(i.next()) + ";");
		}		
		printer.unindent();
		printer.print("end interface");
	}
	
	/* DESCRIPTION: Retrieves the desired SIFB and produces the corresponding code
	 * PARAMETERS: fileName - base name of the output file(s) produced
	 *             top - true if this is the top-level file, otherwise false
	 * RETURNS: TRUE if the SIFB code was generated, FALSE otherwise
	 */
	protected boolean retrieveSIFB(String fileName, boolean top) throws IOException {
		if (!top && !sifbInfo.resolvedAny())
			return false;
		// TODO: NOTE: Gareth changed it to 'getPostfix' which is _<index>_uniquepostfix
		// this should be correct behaviour
		String file = fileName + sifbInfo.getPostfix() + ".strl";
		if (fileNames.contains(file))
			return true;
		
		String path = sifbInfo.getPath() + sifbInfo.getBlockName();
		BufferedWriter out = null;
		BufferedReader in = null;
		try {
			out = new BufferedWriter(new FileWriter(file));
			fileNames.add(file);
			file = path + ".strl";
			in = new BufferedReader(new FileReader(file));
		} catch (IOException e) {
			OutputManager.printError(fileName, file + ": Could not be opened.\n", OutputLevel.FATAL);
			System.exit(-1);
		}
		copyFile(out, in);			
		return true;
	}
	
	/* Generates the host type declarations */
	protected void generateHostTypes() throws IOException {
		Iterator<String> i = fb.getHostTypes().iterator();
		while (i.hasNext()) {
			String type = i.next();
			if (isHostType(type))
				printer.print("host type " + type + ";");
		}
	}
	
	/* DESCRIPTION: Generates code to instantiate the subblocks in a composite 
	 *              function block
	 */
	protected void printComposite() throws IOException {
		if (runs.length <= 0)
			return;
		SignalTable sigTable = new SignalTable(runs, fb.sigNames, deferredBlocks);
		
		boolean first = true;
		StringBuilder line = new StringBuilder("signal ");
		Collection<SignalInfo> entries = sigTable.getUniqueSigBindings();
		Iterator<SignalInfo> i = entries.iterator();
		while (i.hasNext()) {
			SignalInfo entry = i.next();
			PortType pt = entry.getPortType();
			if (pt == PortType.INPUT || pt == PortType.OUTPUT)
				continue;
			
			if (first)
				first = false;
			else
				line.append("," + CodePrinter.newLine + "           ");	
			line.append(entry.getSignal());
			String type = entry.getSigType();
			if (type != null) {
				line.append(" : value " + getDataType(type));
				String init = entry.getInit();
				if (init != null) {
					line.append(" init " + parseParam(type, init, 
							                          !entry.getArraySize().isEmpty(),-1)); // index -1 == whole array string if applicable
				}
			}
		}
		
		boolean end = false;
		if (line.length() > 7) {
			end = true;
			line.append(" in");
			printer.print(line.toString());
			printer.indent();
		}
		
		// Print out all the run statements
		int j = 0;
		for (;;) {
			// TODO: Remove: Postfix is now integrated into 'compiledtype'
			String postfix = "";//runs[j].getPostfix();
			String compiledType = runs[j].getType();
			HashSet<String> ios = fb.getSubBlockIOSet(compiledType);
			int length = compiledType.length() + postfix.length() + 6;
			line.replace(0, line.length(), "run " + compiledType + postfix + " [");
			
			// Print out all bound signals
			Set<Signal> signals = runs[j].getSignalSet();
			Iterator<Signal> k = signals.iterator();
			if (k.hasNext()) {
				Signal sig = k.next();
				SignalInfo bind = sigTable.getBinding(sig);
				String subIOName = sig.getSignal();
				line.append(bind.getSignal() + " / " + subIOName);
				ios.remove(subIOName);
				while (k.hasNext()) {
					line.append(",");
					printer.print(line.toString());
					sig = k.next();
					bind = sigTable.getBinding(sig);
					subIOName = sig.getSignal();
					line.replace(0, line.length(), 
							getIndent(length) + bind.getSignal() + " / " + subIOName);
					ios.remove(subIOName);
				}
			}
			
			// Print out signals that are null bound
			Iterator<String> io = ios.iterator();
			if (io.hasNext()) {
				if (signals.size() <= 0)
					line.append("/ " + io.next());
				while (io.hasNext()) {
					line.append(",");
					printer.print(line.toString());
					line.replace(0, line.length(), getIndent(length) + "/ " + io.next());
				}
			}			
			line.append("];");
			printer.print(line.toString());
			
			j++;
			if (j < runs.length) {
				printer.unindent();
				printer.print("||");
				printer.indent();
			}
			else
				break;
		}
		
		Set<Signal> signals = sigTable.getAuxSignalSet();
		Iterator<Signal> k = signals.iterator();
		if (k.hasNext()) {
			printer.unindent();
			printer.print("||");
			printer.indent();
			printer.print("loop");
			printer.indent();
			while (k.hasNext()) {
				Signal sig = k.next();
				if (sig.getSigType() == null) {
					line.replace(0, line.length(), "emit " + sig.getSignal() + " <= ");
					HashSet<Signal> sigSet = sigTable.getAuxRelationSet(sig);
					Iterator<Signal> it = sigSet.iterator();
					line.append(it.next().getSignal());
					while (it.hasNext()) {
						line.append(" or " + it.next().getSignal());
					}
				}
				else {
					line.replace(0, line.length(), "emit ?" + sig.getSignal() + " <= ?");
					HashSet<Signal> sigSet = sigTable.getAuxRelationSet(sig);
					Iterator<Signal> it = sigSet.iterator();
					line.append(it.next().getSignal());
				}
				line.append(";");
				printer.print(line.toString());
			}
			printer.print("pause;");
			printer.unindent();
			printer.print("end loop;");
		}
		
		if (end) {
			printer.unindent();
			printer.print("end signal;");
		}
	}
	
	/*
	protected void printMultiDrivers(Instance run) throws IOException {
		Set<LocalSig> outs = run.getMultiOutputSignals();
		Iterator<LocalSig> o = outs.iterator();
		boolean hadNext = false;
		if (o.hasNext()) {
			hadNext = true;
			printer.unindent();
			printer.print("||");
			printer.indent();
			printer.print("loop");
			printer.indent();
		}
		while (o.hasNext()) {
			LocalSig sig = o.next();
			printer.print("if " + sig.getName() + " then");
			printer.indent();
			Iterator<String> oth = run.getMultiOutputListIterator(sig);
			if (sig.getType() == null) {
				while (oth.hasNext()) {
					printer.print("emit " + oth.next() + ";");
				}				
			}
			else {				
				while (oth.hasNext()) {
					printer.print("emit ?" + oth.next() + " <= ?" + sig.getName() + ";");
				}
			}
			printer.unindent();
			printer.print("end if;");
		}
		if (hadNext) {
			printer.print("pause;");
			printer.unindent();
			printer.print("end loop;");
		}
				
		if ( !run.multiInputSignalsExist() )
			return;
		hadNext = false;
		Iterator<CollatedSigMap> i = run.getMultiInputIterator();
		if (i.hasNext()) {
			hadNext = true;
			printer.unindent();
			printer.print("||");
			printer.indent();
			printer.print("loop");
			printer.indent();
		}
		while (i.hasNext()) {
			CollatedSigMap sigMap = i.next();
			Iterator<String> j = sigMap.getSourcesIterator();
			if (sigMap.getType() == null) {
				StringBuilder code = new StringBuilder(
						"if (" + Instance.getLocalSigBindingName(j.next()));
				while (j.hasNext())
					code.append(" or " + Instance.getLocalSigBindingName(j.next()));
				code.append(") then");
				printer.print(code.toString());
				printer.indent();
				printer.print("emit " + sigMap.getSigName());
			}
			else {
				printer.print("if");
				printer.indent();
				while (j.hasNext()) {
					String test = Instance.getLocalSigBindingName(j.next());
					printer.print("case " + test + " do");
					printer.indent();
					printer.print("emit ?" + sigMap.getSigName() + " <= " + "?" + test);
					printer.unindent();
				}
			}
			printer.unindent();
			printer.print("end if;");
		}
		if (hadNext) {
			printer.print("pause;");
			printer.unindent();
			printer.print("end loop;");
		}
	}
	*/
	
	/* Generates the Esterel code for a basic function block */
	protected void generateBasicFB(String fbName) throws IOException {
		if( fb.getBasicFB() instanceof BasicHCECCFB )
		{
			formatHCECCStateTree(rootState); // Set childConditions for everything...
			stateIndex = 0;
			visited.clear();
		}
		
		formatStateTree(rootState);

		// Print out all host procedure declarations
		/*
		if (procedures != null) {
			for (int i = 0; i < procedures.length; i++) {
				String buf = getParamList(procedures[i]);
				if (buf == null)
					buf = "host procedure " + procedures[i].getFullName() + "();";
				else {
					buf = "host procedure " + procedures[i].getFullName() + "(" +
					      buf + ");";
				}
				printer.print(buf);
			}
		}
		*/
		generateInterface(fbName);
		
		// Remove unneeded "gotos"
		int numOfStates = gotoStates.size();
		if (numOfStates == 1) {
			stateVar = null;
			gotoStates.clear();
			numOfStates = 0;
		}

		// Print out state variable declaration
		if (stateVar != null) {
			printer.print("var " + makeDeclaration(stateVar) + " in");
			printer.indent();
		}
		
		// Print out local signals used as data interface
		if (!intf.isEmpty()) {
			ArrayList<String> inits = generateLocalSigs();
			if (inits.isEmpty())
				printer.print("signal extends " + intf.name + " in");
			else {
				printer.print("signal extends " + intf.name + ",");
				String line;
				ListIterator<String> i = inits.listIterator();
				for (;;) {
					line = "       " + i.next();
					if (i.hasNext()) {
						line += ",";
						printer.print(line);
					}
					else
						break;
				}
				line += " in";
				printer.print(line);
			}
			printer.indent();
			
			// Update all input event-data associations
			if (inputEvents != null) {
				HashMap<String, ArrayList<String>> map;
				map = getEventDataBindings(inputEvents);
				if (map != null) {
					printer.print("loop");
					printer.indent();
					Set<String> dataset = map.keySet();
					Iterator<String> i = dataset.iterator();
					while (i.hasNext()) {
						String data = i.next();
						ArrayList<String> events = map.get(data);
						Iterator<String> j = events.iterator();
						if (!j.hasNext())
							continue;
						StringBuilder line = new StringBuilder("if (" + j.next());
						while (j.hasNext()) {
							line.append(" or " + j.next());
						}
						line.append(") then");
						printer.print(line.toString());
						printer.indent();
						printer.print("emit ?" + data + "_ <= ?" + data + ";");
						printer.unindent();
						printer.print("end if;");
					}
					printer.print("pause;");
					printer.unindent();
					printer.print("end loop;");
					printer.unindent();
					printer.print("||");
					printer.indent();
				}
				/*
				boolean makeLoop = false;
				for (int i = 0; i < inputEvents.length ;i++) {
					if (inputEvents[i].withVar != null) {
						if (!makeLoop) {
							makeLoop = true;
							printer.print("loop");
							printer.indent();
						}
						printer.print("if (" + inputEvents[i].getName() + ") then");
						printer.indent();
						for (int j = 0; j < inputEvents[i].withVar.length; j++) {
							String var = inputEvents[i].withVar[j].getName();
							printer.print("emit ?" + var + "_ <= ?" + var + ";");
						}
						printer.unindent();
						printer.print("end if;");
					}
				}
				
				if (makeLoop) {
					printer.print("pause;");
					printer.unindent();
					printer.print("end loop;");
					printer.unindent();
					printer.print("||");
					printer.indent();
				}
				*/
			}
		}

		// Print out the module's body
		if (needLoop) {
			printer.print("loop");
			printer.indent(); 
		}
		
		// Print out state variable tests if necessary
		if (numOfStates == 2) {
			printer.print("if " + stateVar.getName() + " = 1 then");
			printer.indent();
			generateStates(gotoStates.getLast(), 2);
			printer.unindent();
			printer.print("else");
			printer.indent();
			generateStates(rootState, 2);
			printer.unindent();
			printer.print("end if;");
		}
		else if (numOfStates > 2) {
			int index = 1;
			ListIterator<SyncState> i = gotoStates.listIterator();
			i.next();	// skip the root state
			printer.print("switch " + stateVar.getName());
			printer.indent();
			while (i.hasNext()) {
				printer.print("case " + Integer.toString(index) + " do");
				printer.indent();
				generateStates(i.next(), numOfStates);
				printer.unindent();
				index++;
			}
			printer.print("default do");
			printer.indent();
			generateStates(rootState, numOfStates);
			printer.unindent(2);
			printer.print("end switch;");
		}
		else {
			generateStates(rootState, numOfStates);
		}
		
		if (needLoop) {
			printer.unindent();
			printer.print("end loop;");
		}
		
		// Update all output event-data associations
		if (outputEvents != null) {
			HashMap<String, ArrayList<String>> map;
			map = getEventDataBindings(outputEvents);
			if (map != null) {
				printer.unindent();
				printer.print("||");
				printer.indent();
				printer.print("loop");
				printer.indent();
				Set<String> dataset = map.keySet();
				Iterator<String> i = dataset.iterator();
				while (i.hasNext()) {
					String data = i.next();
					ArrayList<String> events = map.get(data);
					Iterator<String> j = events.iterator();
					if (!j.hasNext())
						continue;
					StringBuilder line = new StringBuilder("if (" + j.next());
					while (j.hasNext()) {
						line.append(" or " + j.next());
					}
					line.append(") then");
					printer.print(line.toString());
					printer.indent();
					printer.print("emit ?" + data + " <= ?" + data + "_;");
					printer.unindent();
					printer.print("end if;");
				}
				printer.print("pause;");
				printer.unindent();
				printer.print("end loop;");
			}
			/*
			boolean makeLoop = false;
			for (int i = 0; i < outputEvents.length ;i++) {
				if (outputEvents[i].withVar != null) {
					if (!makeLoop) {
						makeLoop = true;
						printer.unindent();
						printer.print("||");
						printer.indent();
						printer.print("loop");
						printer.indent();
					}
					printer.print("if (" + outputEvents[i].getName() + ") then");
					printer.indent();
					for (int j = 0; j < outputEvents[i].withVar.length; j++) {
						String var = outputEvents[i].withVar[j].getName();
						printer.print("emit ?" + var + " <= ?" + var + "_;");
					}
					printer.unindent();
					printer.print("end if;");
				}
			}
			if (makeLoop) {
				printer.print("pause;");
				printer.unindent();
				printer.print("end loop;");
			}
			*/
		}
		
		if (!intf.isEmpty()) {
			printer.unindent();
			printer.print("end signal;");
		}
		
		if (stateVar != null) {
			printer.unindent();
			printer.print("end var;");
		}
	}
	
	/* DESCRIPTION: Create the mapping that associates data with the corresponding 
	 *              list of events 
	 * PARAMETERS: events - array of events
	 * RETURNS: Map associating data with corresponding list of events 
	 */
	HashMap<String, ArrayList<String>> getEventDataBindings(Event[] events) {
		HashMap<String, ArrayList<String>> map = new HashMap<String, ArrayList<String>>();
		for (int i = 0; i < events.length ;i++) {
			if (events[i].withVar != null) {
				for (int j = 0; j < events[i].withVar.length; j++) {
					String key = events[i].withVar[j].getName();
					ArrayList<String> value = map.get(key);
					if (value == null) {
						value = new ArrayList<String>();
						map.put(key, value);
					}
					value.add(events[i].getName());
				}
			}
		}
		
		if (map.isEmpty())
			return null;
		return map;
	}
	
	/* DESCRIPTION: Formats the synchronous state tree for printing 
	 * PARAMETERS: state - currently visited state 
	 */
	protected void formatStateTree(SyncState state) {
		if (visited.contains(state))
			return;
		visited.add(state);
		
		if (state.children == null)
			return;
		
		if (state.parents != null) {
			if (state == rootState) {
				if (state.parents.length >= 1)
					needLoop = true;
			}
			if (state.parents.length >= 2) {
				if (stateVar == null) {
					String name = fb.varNames.addLocalUniqueName("stateVar");
					stateVar = new VarType(Category.INTERNAL, name, "UINT", "0");
				}
				if (gotoStates.peek() == rootState) {
					state.index = stateIndex;
					stateIndex++;
					gotoStates.add(state);
				}
				else {
					rootState.index = 0;
					stateIndex++;
					gotoStates.addFirst(rootState);
					needLoop = true;
					if (state != rootState) {
						state.index = stateIndex;
						stateIndex++;
						gotoStates.add(state);
					}
				}
			}
		}
		
		for (int i = 0; i < state.conditions.length; i++) {
			if (state.conditions[i].equalsType(ConditionType.DATA))
				state.childConditions.add(state.conditions[i]);
		}
				
		for (int i = 0; i < state.children.length; i++)
			formatStateTree(state.children[i]);
	}
	
	/* Generates the module interface */
	protected void generateInterface(String fbName) throws IOException {
		// Print out all pure input signal (input event) declarations
		if (inputEvents != null) {
			for (int i = 0; i < inputEvents.length; i++) {
				String buf = "input " + inputEvents[i].getName() + "; ";
				if (inputEvents[i].getComment() != null)
					buf = buf + "// " + inputEvents[i].getComment();
				printer.print(buf);
			}
		}

		// Print out the Resume input signal if any
		if (resume != null)
			printer.print("input " + resume.getName() + "; ");
		else if (inheritedResumes != null) {
			for (int i = 0; i < inheritedResumes.length; i++)
				printer.print("input " + inheritedResumes[i] + "; ");
		}

		// Print out all valueonly input signal (input data) declarations
		if (inputData != null) {
			for (int i = 0; i < inputData.length; i++)
				printer.print(makeDeclaration(inputData[i]) + ";");
		}

		// Print out all pure output signal (output event) declarations
		if (outputEvents != null) {
			for (int i = 0; i < outputEvents.length; i++) {
				String buf = "output " + outputEvents[i].getName() + "; ";
				if (outputEvents[i].getComment() != null)
					buf = buf + "// " + outputEvents[i].getComment();
				printer.print(buf);
			}
		}

		// Print out all valueonly output signal (output data) declarations
		if (outputData != null) {
			for (int i = 0; i < outputData.length; i++)
				printer.print(makeDeclaration(outputData[i]) + ";");
		}
		
		if (inputData != null || outputData != null || internalVars != null)
			intf.name = fbName + "Intf";
	}
	
	/* Generates local signal initializers for interface data and internal variables */
	protected ArrayList<String> generateLocalSigs() {
		ArrayList<String> inits = new ArrayList<String>();
		
		// Create the initializer for local copies of input data
		if (inputData != null) {
			for (int i = 0; i < inputData.length; i++) {
				inputData[i].setAlias(inputData[i].getName() + "_");
				intf.add(inputData[i]);
				String initial = inputData[i].getInitial();
				if (initial != null) {
					inits.add("refine " + inputData[i].getAlias() + " : init " + 
							  parseParam(inputData[i].getType(), initial, 
									     !inputData[i].getArraySize().isEmpty(),-1));// index -1 == whole array string if applicable
				}
			}
		}
		
		// Create the initializer for local copies of output data
		if (outputData != null) {
			for (int i = 0; i < outputData.length; i++) {
				outputData[i].setAlias(outputData[i].getName() + "_");
				intf.add(outputData[i]);
				String initial = outputData[i].getInitial();
				if (initial != null) {
					inits.add("refine " + outputData[i].getAlias() + " : init " + 
							  parseParam(outputData[i].getType(), initial, 
									     !outputData[i].getArraySize().isEmpty(),-1));// index -1 == whole array string if applicable
				}
			}
		}
		
		// Create the initializer for local copies of internal variables
		if (internalVars != null) {
			for (int i = 0; i < internalVars.length; i++) {
				internalVars[i].setAlias(internalVars[i].getName() + "_");
				intf.add(internalVars[i]);
				String initial = internalVars[i].getInitial();
				if (initial != null) {
					inits.add("refine " + internalVars[i].getAlias() + " : init " + 
							  parseParam(internalVars[i].getType(), initial, 
									     !internalVars[i].getArraySize().isEmpty(),-1));// index -1 == whole array string if applicable
				}
			}
		}
		
		return inits;
	}
	
	/* DESCRIPTION: Create the interface declaration for the given variable
	 * ARGUMENTS: var - variable to declare
	 * RETURNS: String used for the declaration of the variable
	 */
	protected static String makeIntfDeclaration(VarType var) {
		StringBuilder decl = new StringBuilder();
		
		if ( var.isCategory(Category.IFACE_IN) )
			decl.append("input ");
		else
			decl.append("inputoutput ");
		decl.append(var.getAlias() + " : value " + getDataType(var.getType()));
		
		String array = var.getArraySize();
		if (!array.isEmpty())
			decl.append("[" + array + "]");
		
		return decl.toString();
	}
	
	/* DESCRIPTION: Create the declaration for the variable
	 * ARGUMENTS: var - variable to declare
	 * RETURNS: String used for the declaration of the variable
	 */
	protected static String makeDeclaration(VarType var) {
		StringBuilder decl = new StringBuilder();
		String type = var.getType();
		String initial = var.getInitial();
		String arraySize = var.getArraySize();
		boolean array = false;
		if (!arraySize.isEmpty()) {
			arraySize = "[" + arraySize + "]";
			array = true;
		}
		
		if ( var.isCategory(Category.INTERNAL) ) {
			decl.append(var.getName() + " : " + getDataType(type) + arraySize);
			if (initial != null)
				decl.append(" := " + parseParam(type, initial, array,-1));// index -1 == whole array string if applicable
		}
		else {
			if ( var.isCategory(Category.IFACE_IN) )
				decl.append("input ");
			else
				decl.append("output ");
			decl.append(var.getName() + " : value " + getDataType(type) + arraySize);
			if (initial != null)
				decl.append(" init " + parseParam(type, initial, array,-1));// index -1 == whole array string if applicable
		}		
		return decl.toString();
	}
	
	/* DESCRIPTION: Matches ST data types with Esterel primitive types. If a 
	 *              match is not found, a host type is assumed.
	 * ARGUMENTS: type - the data type to be matched
	 * RETURNS: The equivalent Esterel type
	 */
	public static String getDataType(String type) {
		if (type.equalsIgnoreCase("BOOL"))
			return "bool";
		else if (type.equalsIgnoreCase("SINT"))
			return "signed<[8]>";
		else if (type.equalsIgnoreCase("INT"))
			return "signed<[16]>";
		else if (type.equalsIgnoreCase("DINT"))
			return "signed<[32]>";
		else if (type.equalsIgnoreCase("LINT"))
			return "signed<[64]>";
		else if (type.equalsIgnoreCase("USINT"))
			return "unsigned<[8]>";
		else if (type.equalsIgnoreCase("UINT"))
			return "unsigned<[16]>";
		else if (type.equalsIgnoreCase("UDINT"))
			return "unsigned<[32]>";
		else if (type.equalsIgnoreCase("ULINT"))
			return "unsigned<[64]>";
		else if (type.equalsIgnoreCase("REAL"))
			return "float";
		else if (type.equalsIgnoreCase("LREAL"))
			return "double";
		else if (type.equalsIgnoreCase("STRING") || type.equalsIgnoreCase("WSTRING"))
			return "string";
		else if (type.equalsIgnoreCase("BYTE"))
			return "bool[8]";
		else if (type.equalsIgnoreCase("WORD"))
			return "bool[16]";
		else if (type.equalsIgnoreCase("DWORD"))
			return "bool[32]";
		else if (type.equalsIgnoreCase("LWORD"))
			return "bool[64]";
		else if (type.equalsIgnoreCase("TIME"))
			return "signed<[32]>";
		else 
			return type;
	}
	
	/* DESCRIPTION: Checks if the given data type is an Esterel primitive types. 
	 *              If it is, it returns FALSE; otherwise, TRUE
	 * ARGUMENTS: type - the data type to be checked
	 * RETURNS: TRUE if "type" is a host type; FALSE otherwise
	 */
	protected static boolean isHostType(String type) {
		if (type.equalsIgnoreCase("BOOL")    || type.equalsIgnoreCase("SINT")   ||
			type.equalsIgnoreCase("INT")     || type.equalsIgnoreCase("DINT")   ||
			type.equalsIgnoreCase("LINT")    || type.equalsIgnoreCase("USINT")  ||
			type.equalsIgnoreCase("UINT")    || type.equalsIgnoreCase("UDINT")  ||
			type.equalsIgnoreCase("ULINT")   || type.equalsIgnoreCase("REAL")   ||
			type.equalsIgnoreCase("LREAL")   || type.equalsIgnoreCase("STRING") ||
			type.equalsIgnoreCase("WSTRING") || type.equalsIgnoreCase("BYTE")   || 
			type.equalsIgnoreCase("WORD")    || type.equalsIgnoreCase("DWORD")  ||
			type.equalsIgnoreCase("LWORD")   || type.equalsIgnoreCase("TIME")   ||
			type.equalsIgnoreCase("ANY")) {
			return false;
		}
		else
			return true;
	}
	
	/* DESCRIPTION: Maps C data types with Esterel primitive types. If a match 
	 *              is not found, a user-defined type is assumed.
	 * ARGUMENTS: type - the data type to be mapped
	 * RETURNS: The equivalent Esterel type
	 */
	protected static String mapCType(String type) {
		if (type.equals("boolean") || type.equals("bool") || type.equals("_Bool"))
			return "bool";
		else if (type.equals("char"))
			return "signed<[8]>";
		else if (type.equals("short"))
			return "signed<[16]>";
		else if (type.equals("int") || type.equals("long"))
			return "signed<[32]>";
		else if (type.equals("unsigned char"))
			return "unsigned<[8]>";
		else if (type.equals("unsigned short"))
			return "unsigned<[16]>";
		else if (type.equals("unsigned int") || type.equals("unsigned long"))
			return "unsigned<[32]>";
		else 
			return type;
	}
	
	/* DESCRIPTION: Create the string for the parameter declaration of the given 
	 *              procedure
	 * ARGUMENTS: proc - procedure to be declared
	 * RETURNS: String containing the parameter declaration for the procedure
	 */
	// Returns the 
	protected static String getParamList(Procedure proc) {
		StringBuilder list = new StringBuilder();
		String language = proc.getLanguage();
		ArrayList<VarType> arguments = proc.getArgumentList();
		ArrayList<ParamType> paramTypes = proc.getParamTypeList();
		
		if (language.equals("ST")) {
			int size = arguments.size();
			for (int i = 0; i < size; i++) {
				VarType var = arguments.get(i);
				if ( var.isCategory(Category.IFACE_IN) ) {
					// All input data are treated as read-only
					list.append( "in " + getDataType(var.getType()) );
				} else {
					// Output data are mapped to corresponding variables.
					// All variables are then made modifiable even if the actual 
					// procedure only needs to read it.
					list.append( "out " + getDataType(var.getType()) );
				}
							
				if (i < size - 1)
					list.append(", ");			
			}
		}
		else if (!paramTypes.isEmpty() && language.equals("C")) {
			int size = paramTypes.size();
			int i = 0;
			while (true) {
				ParamType param = paramTypes.get(i); 
				String type = param.getType();
				if (!type.isEmpty()) {
					if (param.isWrite())
						list.append("out " + mapCType(type));
					else
						list.append("in " + mapCType(type));
				}
				i++;
				
				if (i < size)
					list.append(", ");
				else
					break;
			}
		}
		return list.toString();
	}
	
	/* DESCRIPTION: Performs a depth-first traversal to print out the code for   
	 *              each synchronous state 
	 * PARAMETERS: state - the state currently being visited
	 *             numOfStates - total number of states in the ECC
	 */ 
	protected void printState(SyncState state, int numOfStates) throws IOException {
		if (visited.contains(state)) {
			return;
		}
		visited.add(state);
		
		// Print out the action for this state
		if (state.actions != null) {			
			for (int i = 0; i < state.actions.length; i++) {
				Procedure procedure = state.actions[i].getProcedure();
				if (procedure != null)
					printer.print("run " + procedure.getFullName() + ";");
				
				Event output = state.actions[i].getOutput(); 
				if (output != null)
					printer.print("emit " + output.getName() + ";");
			}			
		}
		
		
		// Print out the test for pure data conditions, if any
		/*
		Iterator<Condition> it = state.childConditions.iterator();
		int size = state.childConditions.size();
		if (size > 0) {
			LinkedList<Token> tokens = it.next().getConditionTokens();			
			if (size == 1) {
				printer.print("if (" + conditionToString(tokens) + ") then");
			}
			else {				
				StringBuilder line = new StringBuilder(
						"if ( (" + conditionToString(tokens) + ") ");
				while (it.hasNext()) {
					tokens = it.next().getConditionTokens();
					line.append("or (" + conditionToString(tokens) + ") ");
				}
				line.append(") then");
				printer.print(line.toString());
			}
			printer.indent();
			printer.print("emit " + resume.getName() + ";");
			printer.unindent();
			printer.print("end if;");			
		}
		*/
		
		// Print out the transition conditions
		if (state.conditions != null) {
			printer.print("pause;");
			printTransitions(state, numOfStates);
		}
		else
			printer.print("halt;");
		
		return;
	}
	
	/* DESCRIPTION: Creates the test condition from the list of tokens
	 * ARGUMENTS: tokens - list of tokens
	 * RETURNS: A string containing the test condition
	 */
	protected String conditionToString(LinkedList<Token> tokens) {
		StringBuilder guard = new StringBuilder();
		ListIterator<Token> i = tokens.listIterator();
		while (i.hasNext()) {
			Token tok = i.next();
			TokenType type = tok.getType();
			String token = tok.getToken();
			switch (type) {
				case CPARENTHESIS:
					int len = guard.length();
					assert (len > 1): "Unexpected `)\' in guard condition.\n"; 
					guard.deleteCharAt(len - 1);
					break;
				case ARITHOP:
					if (token.equalsIgnoreCase("MOD"))
						token = "mod";
					break;
				case LOGICOP:
					if (token.equalsIgnoreCase("NOT")	|| 
						token.equalsIgnoreCase("OR")	||
						token.equalsIgnoreCase("AND")	||
						token.equalsIgnoreCase("XOR"))
						token.toLowerCase(); 
					else if (token.equals("&"))
						token = "and";
					else if (token.equals("!"))
						token = "not";
					break;
				case EVENT:
					token = "pre(" + token + ")";
					break;
				case IFVAR:
				case INTVAR:
					token = "pre(?" + getVarAlias(token, type) + ")";
					break;
			}			
			guard.append(token);
			if (type != TokenType.OPARENTHESIS)
				guard.append(" ");
		}
		
		guard.deleteCharAt(guard.length() - 1);
		return guard.toString();
	}
	
	/* Returns the alias name for a given token */
	String getVarAlias(String token, TokenType type) {
		if (type == TokenType.IFVAR) {
			for (int i = 0; i < inputData.length; i++) {
				if (inputData[i].getName().equals(token))
					return inputData[i].getAlias();
			}
			for (int i = 0; i < outputData.length; i++) {
				if (outputData[i].getName().equals(token))
					return outputData[i].getAlias();
			}
		}
		else if (type == TokenType.INTVAR) {
			for (int i = 0; i < internalVars.length; i++) {
				if (internalVars[i].getName().equals(token))
					return internalVars[i].getAlias();
			}
		}
		assert (false): "Alias for `" + token + "\' cannot be found.\n";
		return null;
	}
	
	/* DESCRIPTION: Pretty prints the ECC transition conditions 
	 * PARAMETERS: state - the state currently being visited 
	 *             numOfStates - total number of states in the ECC
	 */ 
	protected void printTransitions(SyncState state, int numOfStates) throws IOException {
		int len = state.conditions.length;
		assert (len == state.children.length): 
			"Conditions and children length do not match.\n";
		
		if (len > 1) {
			printer.print("await");
			printer.indent();
		}
		int i = 0;
		for (;;) {
			if ( state.conditions[i].equalsType(ConditionType.IMMEDIATE) ) {
				if (i > 0) {
					printer.print("case immediate tick do");
					printer.indent();
				}
				if (gotoStates.contains(state.children[i]))
					printer.print(stateVar.getName() + " := " + state.children[i].index + ";");
				else
					printState(state.children[i], numOfStates);
				if (len > 1) {
					printer.unindent(2);
					printer.print("end await;");
				}
				break;
			}
			LinkedList<Token> tokens = state.conditions[i].getConditionTokens();
			if (len == 1) {
				printer.print("await immediate " + conditionToString(tokens) + ";");
			}
			else {
				printer.print("case immediate " + conditionToString(tokens) + " do");
				printer.indent();
			}
			
			if (gotoStates.contains(state.children[i]))
				printer.print(stateVar.getName() + " := " + state.children[i].index + ";");
			else
				printState(state.children[i], numOfStates);
			i++;
			if (i >= len) {
				if (len > 1) {
					printer.unindent(2);
					printer.print("end await;");
				}
				break;
			}
			printer.unindent();
		}
	}
	
	/* Outputs the algorithms for this module */
	protected void printAlgs() throws IOException {
		printer.print("// ECC algorithms");
		for (int i = 0; i < procedures.length; i++) {
			String remark = procedures[i].getComment();
			if (remark != null) {
				if (!remark.isEmpty())
					printer.print("/* " + remark + " */");
			}
			printer.print("module " + procedures[i].getFullName() + ":");
			printer.indent();
			if (!intf.isEmpty()) {
				printer.print("extends " + intf.name + ";");
				
				String line;
				ListIterator<VarType> j = intf.listIterator();
				VarType var = j.next();
				line = "var " + var.getName() + " : " + getDataType(var.getType()) + 
				       " := " + "pre(?" + var.getAlias() + ")";
				while (j.hasNext()) {
					line += ",";
					printer.print(line);
					var = j.next();
					line = "    " + var.getName() + " : " + getDataType(var.getType()) + 
					       " := " + "pre(?" + var.getAlias() + ")";
				}
				line += " in";
				printer.print(line);				
				printer.indent();
			}
			
			printer.print(procedures[i].getText());
			
			if (!intf.isEmpty()) {
				ListIterator<VarType> j = intf.listIterator();
				while (j.hasNext()) {
					VarType var = j.next();
					if ( !var.isCategory(Category.IFACE_IN) ) 
						printer.print("emit ?" + var.getAlias() + " <= " + var.getName() + ";");
				}
				printer.unindent();
				printer.print("end var;");
			}
			printer.unindent();
			printer.print("end module", 1);					
		}
	}
	
	/* DESCRIPTION: Define the given port with the given type. It is assumed 
	 *              that the given port is originally of type "ANY"."
	 * ARGUMENTS: portName - the name of the port whose type is to be redefined
	 *            dt - new data type
	 * RETURNS: TRUE if the type of "portName" is successfully redefined, FALSE 
	 *          otherwise
	 */
	public boolean defineAnyPort(String portName, DataType dt) {	
		if (sifbInfo != null) {
			return sifbInfo.defineAnyPort("", portName, dt);
		}
		return false;
	}
}
