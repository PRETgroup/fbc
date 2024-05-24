/* CodeGenerator.java
 * Abstract class for all kinds of function block code generators.
 *
 * Written by: Li Hsien Yoong
 * 
 * Gareth added 'dataIO'&'updatedata' to SIFB template for HMI generation // later removed them
 */

package ir;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import fbtostrl.*;
import fb.*;
import esterel.*;

public abstract class CodeGenerator {
	protected Options opts;
	public FunctionBlock fb;
	protected String[] headers;
	public Event[] inputEvents;
	public Event[] outputEvents;
	public VarType[] inputData;
	// Kyle: public MethodReference[] methodReferences;
	public VarType[] outputData;
	protected Parameter[] params;
	
	protected VarType[] internalVars;
	protected Procedure[] procedures;
	protected SyncState rootState;
	protected String[] inheritedResumes;
	// TODO: finish removing resume
	// (its always null)
	protected Event resume;			// additional Resume event for pure data conditions
	protected int stateIndex;
	
	protected Instance[] runs;
	
	public SIPath sifbInfo;		// information on the referenced SIFB
	
	protected String myFileName;	// use during deferred code generation
	protected String insName;		// instance name
	protected LinkedList<CodeGenerator> deferredBlocks;
	
	protected HashSet<SyncState> visited = new HashSet<SyncState>();
	protected LinkedList<SyncState> gotoStates = new LinkedList<SyncState>();
	protected CodePrinter printer;
	protected LinkedList<String> fileNames;	// list of files that have been generated
	
	// Set of additional source dependencies
	public static HashSet<String> srcDependencies = new HashSet<String>();
	
	// Set of additional object dependencies
	public static HashSet<String> objDependencies = new HashSet<String>();
	
	// Set of include paths
	public static HashSet<String> includePaths = new HashSet<String>();
	
	// Map of global variables
	public static HashMap<String, GlobalVar> globalVars = new HashMap<String, GlobalVar>();
	
	// Map containing the periods of multirate messages
	public static HashMap<String, MsgPeriods> msgPeriodMap;
	
	protected String fullyQualifiedInsName;	
	protected String lateCode;	// arbitrary code to be generated after the deferred phase
	
	public LinkedList<CodeGenerator> getDeferredBlocks() {
		return deferredBlocks;
	}
	
	public CodeGenerator(FunctionBlock f, Options o, LinkedList<String> files, 
			             String n, String pn, LinkedList<CodeGenerator> cgs) {
		opts = o;
		insName = n;
		fullyQualifiedInsName = pn+n;
		
		fb = f;
		headers = fb.headers;
		inputEvents = fb.inputEvents;
		outputEvents = fb.outputEvents;
		inputData = fb.inputData;
		// Kyle: methodReferences = fb.methodReferences;
		outputData = fb.outputData;
		params = fb.parameters;
		internalVars = fb.internalVars;
		procedures = fb.procedures;
		rootState = fb.rootState;
		inheritedResumes = fb.inheritedResumes;
		if (fb.compositeFB != null)
			runs = fb.compositeFB.runs;
		sifbInfo = fb.sifbInfo;
		fileNames = files;
		deferredBlocks = cgs;
	}
	
	abstract public CodeGenerator generateCode(String fileName, boolean top, 
			HashSet<Block> comBlocks) throws IOException;
	
	abstract protected void printState(SyncState state, int numOfStates) throws IOException;
	
	abstract protected HashSet<String> getToAnyConverterDecls();
	
	abstract protected HashSet<String> getAnyToConverterDecls();
	
	protected void generateStates(SyncState state, int numOfStates) throws IOException {
		visited.clear();
		printState(state, numOfStates);
	}
	
	protected static String getIndent(int indent) {
		char[] spaces = new char[indent];
		for (int i = 0; i < indent; i++)
			spaces[i] = ' ';
		return new String(spaces);
	}
	
	/* Returns the file name for a given path */
	public static String getFileName(String path) {
		int index = path.lastIndexOf(File.separatorChar);
		if (index >= 0)
			return path.substring(index + 1);
		else
			return path;
	}
	
	/* Returns the parent name for a given path */
	public static String getParentName(String path) {
		int index = path.lastIndexOf(File.separatorChar);
		if (index >= 0)
			return path.substring(0, index + 1);
		else
			return "";
	}
	
	/* Returns the given path after removing any file extension */
	public static String removeFileExt(String path) {
		int index = path.lastIndexOf('.');
		if (index >= 0)
			return path.substring(0, index);
		else
			return path;
	}
	
	/* Returns the file extension for a given path */
	public static String getFileExt(String path) {
		int index = path.lastIndexOf('.');
		if (index >= 0)
			return path.substring(index);
		else
			return "";
	}
	
	public static String makeHeaderMacro(String filename) {
		// Replace puctuation, space, and tab characters with '_'
		return filename.toUpperCase().replaceAll("[\\p{Punct} \t]", "_") + "_";
	}
	
	/**
	 * Escape special characters from a given file path.
	 * @param filename name of the file (possibly including the path) to be escaped
	 * @return Escaped string for the given filename
	 */
	public static String escapeFilePaths(String filename) {
		StringBuilder formatted = new StringBuilder(filename);
		for (int i = 0; i < formatted.length(); i++) {
			char c = formatted.charAt(i);
			if (c == ':' || c == '\\' || Character.isWhitespace(c)) {
				formatted.insert(i, '\\');
				i++;
			}
		}
		return formatted.toString();
	}
	
	public String getResumeName() {
		if (resume != null)
			return resume.getName();
		return null;
	}
	
	/* DESCRIPTION: Copies the SIFB code template from "in" to the "out"
	 * PARAMETERS: out - output file
	 *             in - input file
	 */
	protected void copyFile(BufferedWriter out, BufferedReader in) throws IOException {
		String filename = "";		
		String line;
		while ((line = in.readLine()) != null) {
			int index = line.indexOf("@copyFile#");
			if (index >= 0) {
				index += 10;
				String path = getParentName(fileNames.getLast());
				String file = line.substring(index);
				String sifbfile = file;
					
				// Depends on how template is written NOT on the OS FBC is running on
				if( file.contains("\\") )
					file = file.substring(file.lastIndexOf('\\'));
				else if( file.contains("/") ) 
					file = file.substring(file.lastIndexOf('/')+1);
					
				if (file.endsWith(".c"))
					srcDependencies.add(file);
					
				filename = path + file;
				if (fileNames.contains(filename))
					continue;

				BufferedWriter write = null;
				BufferedReader read = null;
				try {
					write = new BufferedWriter(new FileWriter(filename));
					fileNames.add(filename);
					filename = sifbInfo.getPath() + sifbfile;
					read = new BufferedReader(new FileReader(filename));
				} catch (IOException e) {
					OutputManager.printError("", filename + ": Could not be opened.\n", OutputLevel.FATAL);
					System.exit(-1);
				}
				copyFile(write, read);
			}
			else if ( (index = line.indexOf("@addObjDependency#")) >= 0 ) {
				index += 18;
				String file = line.substring(index);					
				if( file.contains("\\") )
					file = file.substring(file.lastIndexOf('\\'));
				else if( file.contains("/") ) 
					file = file.substring(file.lastIndexOf('/'));
				
				if (file.endsWith(".o") || file.endsWith(".obj"))
					objDependencies.add(file);
			}
			else if ( (index = line.indexOf("@replaceVarString#")) >= 0 ) {
				ArrayList<String> substrings = new ArrayList<String>();
				int endIndex = index;
				index = 0;
				do {
					if (endIndex > index)
						substrings.add(line.substring(index, endIndex));
					index = endIndex + 18;
					endIndex = line.indexOf('@', index);
					if (endIndex <= index) {
						// Error in the template file!!!
						OutputManager.printError(insName, 
							"Can\'t find closing `@\' for @replaceVarString#...@ tag. This is an error in the " + 
							sifbInfo.getBlockName() + " template file.", 
							OutputLevel.FATAL);
						System.exit(-1);
					}
					else {
						substrings.add( sifbInfo.getFromVarStringMap(
								line.substring(index, endIndex)) );
						index = endIndex + 1;
					}
				} while ( (endIndex = line.indexOf("@replaceVarString#", index)) >= index);
				substrings.add(line.substring(index));
				for (String str : substrings)
					out.write(str);
				out.newLine();
			}
			else if (line.indexOf("@includeUserTypes@") >= 0) {
				ArrayList<String> userDefinedTypes = 
					sifbInfo.getUserTypesForPorts(fullyQualifiedInsName);
				for (String userType : userDefinedTypes) {
					out.write("#include \"" + userType + ".h\"");
					out.newLine();
				}
			}
			else if (line.indexOf("@declToANY@") >= 0) {
				Iterator<String> i = getToAnyConverterDecls().iterator();
				while (i.hasNext()) {
					out.write(i.next());
					out.newLine();
				}
			}
			else if (line.indexOf("@declANYto@") >= 0) {
				Iterator<String> i = getAnyToConverterDecls().iterator();
				while (i.hasNext()) {
					out.write(i.next());
					out.newLine();
				}
			}
			else if ( (index = line.indexOf("@begin_varlist#")) >= 0 ) {
				line = line.substring(index + 15);
				line = line.substring(0, line.indexOf("@"));
				// NB: this line is not printed --- multi line
				//LinkedHashMap<String, String> varlist = 
				//		sifbInfo.getOrderedList(fullyQualifiedInsName, line); 
				LinkedHashMap<String, DataType> varMap = 
						sifbInfo.getOrderedMap(fullyQualifiedInsName, line);
				printVarListLines(in, varMap, out); 
			}
			else {
				printLineWithVars(line, out);	
			}
		}
		out.close();
		in.close();
	}
	
	// TODO: A better way to do templates?
	// Full file find and replace??
	// Then VarListLines things...
	/**
	 * Print multiple lines per var in varlist
	 * until '@end_varlist@' is found.
	 * @param in
	 * @param line
	 * @param out
	 * @throws IOException 
	 */
	private void printVarListLines(BufferedReader in, HashMap<String, DataType> varlist, 
			BufferedWriter out) throws IOException {		
		String inline = "";
		LinkedList<String> varListLines = new LinkedList<String>();
		while((inline = in.readLine()) != null)
		{
			if( inline.contains("@end_varlist@") )
				break;
			varListLines.add(inline);
		}
		if (varlist == null)
			return;	// no ANY mapping information---this should NEVER happen!
		if (varlist.size() == 0)
			return;	// no ANY mapping information---this should NEVER happen!
		
		// For each Var in SOME list... print varList, replacing # with the var
		int index = 0;
		for(String var : varlist.keySet())
		{
			String varNumber = var.replaceAll("[A-Z_]", "");
			int varnum = 0;
			try
			{
				varnum = Integer.parseInt(varNumber);
			}
			catch(NumberFormatException e)
			{
				OutputManager.printError(fullyQualifiedInsName, 
						"Unexpected identifier name for port `" + var + "\'.", OutputLevel.FATAL);
				System.exit(0);
			}
			String varNumMinus = Integer.toString(varnum-1);
			String varNumPlus = Integer.toString(varnum+1);
			
			for(String line : varListLines)
			{
				String outLine = line.replace("@v#@", varNumber);// number suffix of var
				outLine = outLine.replace("@v#+1@", varNumPlus); 
				outLine = outLine.replace("@v#-1@", varNumMinus);
				// the index... vars not necessarily in this order :(
				outLine = outLine.replace("@#-1@", Integer.toString(index-1));
				outLine = outLine.replace("@#+1@", Integer.toString(index+1));
				DataType value = varlist.get(var);
				if (value != null) // varlist.get is null if not set
				{
					outLine = outLine.replace("@value@", value.getType());
					outLine = outLine.replace("@type@", value.getType());
					if (value.getType().equalsIgnoreCase("ANY")) {
						// Data type is still of type ANY. This means that the port is not bound:
						// Default unbound ANY port to BOOL.
						value.setType("BOOL");
					}
					if (outLine.contains("@type#C@")) {
						if ( !value.getArraySize().isEmpty() ) 
							outLine = outLine.replace("@var@", "@var@[" +  value.getArraySize() + "]");
					}
					outLine = outLine.replace("@type#C@", value.getType()); // using value... return the IEC 61131 type :(
				}
				outLine = outLine.replace("@var@", var);
				printLineWithVars(outLine, out); // Process other vars
				//out.write(outLine+'\n');
			}
			index++;
		}
	}

	private void printLineWithVars(String line, BufferedWriter out) throws IOException 
	{
		int index = 0;
		line = line.replace("@index@", Integer.toString(sifbInfo.getIndex()) );
		line = line.replace("@unique@", sifbInfo.getUniquePostfix());
		line = line.replace("@queue@", Integer.toString(sifbInfo.getPositiveQLen()));
		line = line.replace("@_queue@", sifbInfo.get_QLen());
		line = line.replace("@postfix@", sifbInfo.getPostfix());
		line = line.replace("@globalType@", sifbInfo.getGlobalVarType());
		line = line.replace("@sifbname@", fb.getCompiledType());
		//line = line.replace("@dataIO@;", dataIO.toString());
		//line = line.replace("@updatedata@;", updatedata.toString());
		index = line.indexOf("@CNT");
		if ( index >= 0 ) {
			String countName = line.substring(index+1);
			countName = countName.substring(0, countName.indexOf("@"));
			//line = line.replace("@"+countName+"@", ""+sifbInfo.getCountValue(countName));
			String countValue = FBtoStrl.varSizeMap.get(fullyQualifiedInsName+"."+countName);
			if( countValue != null)
				line = line.replace("@"+countName+"@", countValue);
			else {
				OutputManager.printError(fullyQualifiedInsName, "Could not find count value for variable " +
						countName, OutputLevel.ERROR);
			}
		}
		index = line.indexOf("@indexRange#", 0);
		if (index >= 0) {
			TokenReplacer[] indices = new TokenReplacer[8];
			int len = 0;
			while (index >= 0) {
				index += 12;
				int i = index;
				while (Character.isDigit(line.charAt(i))) {
					i++;
				}
				
				int num = Integer.parseInt(line.substring(index, i));
				if (line.charAt(i) == '#') {
					i++;
					if (line.substring(i, i+4).equals("ANY#"))
					{
						String varname = line.substring(i+5,line.indexOf("@", i+5));
						indices[len] = new AnyReplacer(num, fullyQualifiedInsName+"."+varname);
					}
					else if (line.substring(i, i+6).equals("toANY#")) {
						i += 6;
						index = line.indexOf('@', i);
						if (index > i) 
							indices[len] = new ToAnyReplacer(num, line.substring(i, index));
					}
					else if (line.substring(i, i+6).equals("ANYto#")) {
						i += 6;
						index = line.indexOf('@', i);
						if (index > i) 
							indices[len] = new AnyToReplacer(num, line.substring(i, index));
					}
				}
				else
					indices[len] = new IntegerReplacer(num);
				len++;
				index = line.indexOf("@indexRange#", i);
			}

			ArrayList<String> allTokens = new ArrayList<String>();
			String[] toks0 = line.split("@indexRange#\\d+@");
			for (int j = 0; j < toks0.length; j++) {
				String[] toks1 = toks0[j].split("@indexRange#\\d+#ANY#\\w+@");
				for (int k = 0; k < toks1.length; k++) {
					String[] toks2 = toks1[k].split("@indexRange#\\d+#toANY#\\?\\w+@");
					if (toks2.length == 1)
						toks2 = toks1[k].split("@indexRange#\\d+#ANYto#\\w+@");
					for (int l = 0; l < toks2.length; l++)
						allTokens.add(toks2[l]);
				}
			}
			
			for (int j = 0; j < sifbInfo.getIndex(); j++) {
				int size = allTokens.size();
				for (int k = 0; k < size; k++) {
					out.write(allTokens.get(k));
					if (len > k) {
						String output = indices[k].replace(j, sifbInfo);
						out.write(output);
					}
				}
				out.newLine();
			}
		}
		else
			out.write(line + CodePrinter.newLine);
	}

	public static void plainCopyFile(BufferedWriter out, BufferedReader in)
			throws IOException {
		String line;
		while ((line = in.readLine()) != null) {
			out.write(line + CodePrinter.newLine);
		}
		out.close();
		in.close();
	}
	
	/* DESCRIPTION: Public interface to "parseScalarParam" to handle arrays 
	 * PARAMETERS: type - data type
	 *             param - parameter value
	 *             array - TRUE if "param" is an array; otherwise, FALSE
	 * RETURNS: Formatted parameter
	 */
	// Gareth added stuff to this, it doesn't NEED to be this complicated anymore,
	// return parameter.toString(); SHOULDNT be reached
	protected static String parseParam(String type, String param, boolean array, int arrayIndex) {
		if (array) {
			//StringBuilder parameter = new StringBuilder("{");
			String tokens[] = param.split("[\\[\\]\\,\\s]");
			int i = 0;
			while (i < tokens.length) {
				if (!tokens[i].isEmpty())
					break;
				i++;
			}
			if( arrayIndex == 0 ) return parseScalarParam(type, tokens[i]);
			
			//parameter.append(parseScalarParam(type, tokens[i]));
			int valueIndex = 1;
			
			i++;
			for (; i < tokens.length; i++) {
				if (tokens[i].isEmpty())
					continue;
				if( valueIndex == arrayIndex )
					return parseScalarParam(type, tokens[i]);
				valueIndex++;
				//parameter.append(", " + parseScalarParam(type, tokens[i]));
			}
			//parameter.append("}");
			//return parameter.toString();
			assert(false):"[parseParam] Error: returning NULL";
			return null;
		}
		else
			return parseScalarParam(type, param);
	}
	
	/* DESCRIPTION: Parse parameter values to ensure that they are in the 
	 *              correct format
	 * PARAMETERS: type - data type
	 *             param - parameter value
	 * RETURNS: Formatted parameter if parse succeeds; otherwise "param" is 
	 *          return as is
	 */
	public static String parseScalarParam(String type, String param) {
		if (type.equalsIgnoreCase("TIME")) {
			int index = param.indexOf("#") + 1;
			if (index > 0) {
				// Parse param for a duration literal
				String modParam = param.toLowerCase();
				if (modParam.startsWith("t#") || modParam.startsWith("time#")) {
					long value = 0;
					StringBuilder unit = new StringBuilder();
					StringBuilder number = new StringBuilder();
					int len = modParam.length();
					for (int i = index; i < len; i++) {
						char c = modParam.charAt(i);
						if ( Character.isDigit(c) || c == '.' || c == '-')
							number.append(c);
						else if (c == 'd' || c == 'h' || c == 'm' || c == 's') {
							unit.append(c);
							if (c == 'm') {
								i++;
								if (i < len) {
									c = modParam.charAt(i);
									if (c == 's')
										unit.append(c);
									else
										i--;
								}
							}
							String num = number.toString();
							if ( !Condition.isNumeric(num) ) {
								// This is an error - we can't handle it, returning original param
								return param;
							}
							double fvalue = Double.parseDouble(num);
							String u = unit.toString();
							if (u.equals("d"))
								fvalue = fvalue * 24 * 3600 * 1000000;
							else if (u.equals("h"))
								fvalue = fvalue * 3600 * 1000000;
							else if (u.equals("m"))
								fvalue = fvalue * 60 * 1000000;
							else if (u.equals("s"))
								fvalue = fvalue * 1000000;
							else // u.equals("ms")
								fvalue = fvalue * 1000;
							value += (long)fvalue;
							
							// Reset temporary string buffers
							unit.delete(0, unit.length());
							number.delete(0, number.length());
						}
						else if (c != '_') {
							// This is an error - we can't handle it, returning original param
							return param;
						}
					}
					modParam = Long.toString(value);
				}
				return modParam;
			}
			else
				return param;
		}
		else if (type.equalsIgnoreCase("BOOL")) {
			if (param.equals("1") || param.equalsIgnoreCase("TRUE"))
				return "true";
			else if (param.equals("0") || param.equalsIgnoreCase("FALSE"))
				return "false";
			return param;
		}
		else
			return param;
	}
	
	public String getName() {
		return insName;
	}
	
	public String getFQInsName() {
		return fullyQualifiedInsName;
	}
	
	public String getFileName() {
		return myFileName;
	}
	
	public void setFileName(String fileName) {
		myFileName = fileName;
	}
	
	public String toString() {
		return fullyQualifiedInsName + " : " + myFileName;
	}
	
	/* Returns the number of input events */
	protected int getNumOfIEvents() {
		int num = 0;
		
		if (inputEvents != null)
			num = inputEvents.length;
		if (resume != null)
			num++;
		else if(inheritedResumes != null)
			num += inheritedResumes.length;
		
		return num;
	}
	
	// Returns true if the given block type is a local communication block, 
	// false otherwise
	public static boolean isLocalComBlock(String blockType) {
		if (blockType.startsWith("SEND_") || blockType.startsWith("RECV_")) {
			String tail = blockType.substring(5);
			int idx = tail.indexOf('_');
			if (idx > 0) {
				if (SIPath.isAllDigit(tail.substring(idx + 1)) && 
					SIPath.isAllDigit(tail.substring(0, idx))) {
					return true;
				}
			}
		}
		else if (blockType.startsWith("SENDD3_") || blockType.startsWith("RECVD3_")) {
			int idx = blockType.lastIndexOf('_');
			if (idx > 7) {
				String tail = blockType.substring(7, idx);
				if (SIPath.isAllDigit(tail))
					return true;
			}			
		}
		return false;
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
		gotoStates.add(state);
		
		if (state.children == null)
			return;
		
		for (int i = 0; i < state.conditions.length; i++) {
			if (state.conditions[i].equalsType(ConditionType.DATA))
				state.childConditions.add(state.conditions[i]);
		}
				
		for (int i = 0; i < state.children.length; i++)
			formatStateTree(state.children[i]);
	}
	
	/* DESCRIPTION: Formats the synchronous H-C state tree for printing 
	 * PARAMETERS: state - currently visited state 
	 */
	// TODO: Gareth temp... I have removed setting the index, instead I just "dumb-ly" print whole ECCs
	// Also note that 'state.childConditions' for top level ECC will be set by both this function AND formatStateTree() :(
	protected void formatHCECCStateTree(SyncState state) {
		if (visited.contains(state))
			return;
		visited.add(state);
		if( state == null )
		{
			OutputManager.printNotice("", "[formatHCECCStateTree] Error: state == null", OutputLevel.DEBUG);
			
			return;
		}
		
		/*
		// Update the index
		state.index = stateIndex;
		stateIndex++;
		int savedIndex = stateIndex;
		*/
		// If it doesnt have children... you can skip other processing
		if (state.children == null)
			return;
		
		
		// Actually DO something
		// set childConditions
		
		for (int i = 0; i < state.conditions.length; i++) {
			if (state.conditions[i].equalsType(ConditionType.DATA))
				state.childConditions.add(state.conditions[i]);
		}
		
		if( state.refiningStates != null)
		{
			//stateIndex = 0;
			formatHCECCStateTree(state.refiningStates[0]);
		}
		if( state.parallel != null)
		{
			for(Entry<String, SyncState[]> entry : state.parallel.entrySet())
			{
				//stateIndex = 0;
				SyncState[] states = entry.getValue();
				for (int s = 0; s < states.length; s++)
					formatHCECCStateTree(states[s]);
			}
		}
		//stateIndex = savedIndex;
		
		// Process children
		for (int i = 0; i < state.children.length; i++)
			formatHCECCStateTree(state.children[i]);
	}
	
	public void setSIFBCountValue(String countName, int anyCount) {
		if( sifbInfo != null )
			sifbInfo.setCountValue(countName, anyCount);
	}
		
	protected Instance getInstance(String name) {
		for(Instance b : this.fb.compositeFB.runs)
		{
			if( name.equals(b.getName()) )
				return b;
		}
		return null;
	}
	
	public void setLateCode(String s) {
		lateCode = s;
	}
	
	/**
	 * Set various mappings to match messages with their respective periods. 
	 * @param messages map containing the mapping of messages and their periods
	 */
	public void setMsgPeriodMap(HashMap<String, MsgPeriods> messages) {
		msgPeriodMap = messages;
		addToSIFBVarStringMap(new HashSet<CodeGenerator>());
	}
	
	/**
	 * Add message period information to the variable-string map of SIFBs.
	 */
	private void addToSIFBVarStringMap(HashSet<CodeGenerator> visited) {
		for (CodeGenerator cg : deferredBlocks) {
			if (visited.contains(cg))
				return;
			visited.add(cg);
			cg.addToSIFBVarStringMap(visited);
			if (cg.sifbInfo != null) {
				MsgPeriods periods = msgPeriodMap.get(cg.sifbInfo.getGlobalVar()
						.getRawTypeName());
				int recvPeriod = periods.getRecvPeriod();
				int gcd = periods.getGCD();
				cg.sifbInfo.addToVarStringMap( "sendPeriod", 
						Integer.toString(periods.getSendPeriod()) );
				cg.sifbInfo.addToVarStringMap( "recvPeriod", 
						Integer.toString(recvPeriod) );
				cg.sifbInfo.addToVarStringMap( "gcd", 
						Integer.toString(gcd) );
				cg.sifbInfo.addToVarStringMap( "TrOverGCD", 
						Integer.toString(recvPeriod/gcd) );
			}
		}
	}
	
	public static int validateArraySize(String arraySize) {
		if (SIPath.isAllDigit(arraySize))
			return Integer.parseInt(arraySize);
		return -1;
	}
	
	public final static String getArrayIndex(String arraySize, String blockID, String name) {
		if (arraySize.isEmpty())
			return "";
		int size = validateArraySize(arraySize);
		if (size <= 0) {
			OutputManager.printError(blockID, "Variable `" + name + 
					"' has an invalid array size.", OutputLevel.FATAL);
			System.exit(0);
		}
		return "[" + arraySize + "]";
	}
}
