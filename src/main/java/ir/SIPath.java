/* SIPath.java
 * Helper class to create correct paths for service interface blocks.
 *
 * Written by: Li Hsien Yoong
 */

package ir;

import fbtostrl.*;
import fb.*;

import java.io.*;
import java.util.*;

public class SIPath {
	private static int counter = 0;	// running counter for deriving unique ID
	private String uniqueID;		// unique ID for publisher/subscriber blocks when 
									// compiling to Esterel
	private String blockName;
	private int index;
	private File path;
	private DataType[] anyType;
	private int qlen;				// queue length (used in SEND and RECV blocks)
	private GlobalVar globalVar;	// used for SIFBs that generate global variables
	private HashMap<String, String> varStringMap;	// mapping of code generation 
													// variables with arbitrary strings
	
	// For Templates
	boolean needsUnique;
	private HashMap<String,Integer> counts = new HashMap<String, Integer>();
	// 2011-04-05 Gareth Changed to be static instead of per INSTANCE 
	// (per instance made me sad, and should never have been done that way :| )
	private static LinkedHashMap<String,LinkedHashMap<String,String>> varLists = 
		new LinkedHashMap<String,LinkedHashMap<String,String>>();	
	
	// This is a more complete and simpler map as the varLists map above. 
	private static HashMap<String, HashMap<String, DataType>> anyVarsMap = 
		new HashMap<String, HashMap<String, DataType>>(); 
	
	SIPath(File p, String n, Options o, boolean makeUnique, Parameter[] params) {
		needsUnique = makeUnique;
		String tail = null;
		String queue = null;
		String globalVarName = null;
		String memberName = null;
			
		String codePath;
		if (o.isStrl())
			codePath = "strl";
		else if (o.isL5X())
			codePath = "l5x";
		else
			codePath = "c";
		
		if (n.startsWith("PUBL_")) {
			tail = n.substring(5);
			blockName = "PUBL";
			globalVarName = paramToGlobalVar(n, params, GlobalVarType.PUBSUB);
			if (globalVarName != null)
				memberName = "data_";
		}
		else if (n.startsWith("SUBL_")) {
			tail = n.substring(5);
			blockName = "SUBL";
			globalVarName = paramToGlobalVar(n, params, GlobalVarType.PUBSUB);
			if (globalVarName != null)
				memberName = "data_";
		}
		else if (n.startsWith("PUBLISH_")) {
			tail = n.substring(8);
			int idx = tail.indexOf('_');
			if (idx > 0) {
				queue = tail.substring(idx + 1);
				if (isAllDigit(queue)) {
					qlen = Integer.parseInt(queue); 
					tail = tail.substring(0, idx);
				}
			}
			blockName = "PUBLISH";
		}
		else if (n.startsWith("SUBSCRIBE_")) {
			tail = n.substring(10);
			int idx = tail.indexOf('_');
			if (idx > 0) {
				queue = tail.substring(idx + 1);
				if (isAllDigit(queue)) {
					qlen = Integer.parseInt(queue); 
					tail = tail.substring(0, idx);
				}
			}
			blockName = "SUBSCRIBE";
		}
		else if (n.startsWith("SEND_")) {
			tail = n.substring(5);
			int idx = tail.indexOf('_');
			if (idx > 0) {
				queue = tail.substring(idx + 1);
				if (isAllDigit(queue)) {
					qlen = Integer.parseInt(queue); 
					tail = tail.substring(0, idx);
					blockName = "SEND";
				}
				else
					qlen = -1;	// error state
			}
		}
		else if (n.startsWith("RECV_")) {
			tail = n.substring(5);
			int idx = tail.indexOf('_');
			if (idx > 0) {
				queue = tail.substring(idx + 1);
				if (isAllDigit(queue)) {
					qlen = Integer.parseInt(queue);
					tail = tail.substring(0, idx);
					blockName = "RECV";
				}
				else
					qlen = -1;	// error state
			}
		}
		else if (n.startsWith("SENDuCOS_")) {
			tail = n.substring(9);
			blockName = "SENDuCOS";
			globalVarName = paramToGlobalVar(n, params, GlobalVarType.UCOS);
			if (globalVarName != null)
				memberName = "data_";
		}
		else if (n.startsWith("RECVuCOS_")) {
			tail = n.substring(9);
			blockName = "RECVuCOS";
			globalVarName = paramToGlobalVar(n, params, GlobalVarType.UCOS);
			if (globalVarName != null)
				memberName = "data_";
		}
		else if (n.startsWith("SENDD3_")) {
			tail = n.substring(7);
			blockName = "SENDD3";
			globalVarName = paramToGlobalVar(n, params, GlobalVarType.PLAIN);
			if (globalVarName != null)
				memberName = globalVarName + "_D_";
		}
		else if (n.startsWith("RECVD3_")) {
			tail = n.substring(7);
			blockName = "RECVD3";
			globalVarName = paramToGlobalVar(n, params, GlobalVarType.PLAIN);
			if (globalVarName != null)
				memberName = globalVarName + "_D_";
		}
		else if ( o.isNxtControl() ) {
			if ( n.contains("LINKDATARECEIVER") || n.contains("LINKEVENTRECEIVER") || 
					n.contains("LINKPUBLISHER") || n.contains("OR") || 
					n.contains("AND") || n.contains("NOT") ) {
				index = -1;
				int idx = n.indexOf('_');
				if (idx >= 0) {
					blockName = n.substring(0,idx);
					uniqueID = n.substring(idx+1);
				}
				else {
					blockName = n;
					uniqueID = Integer.toString(getUniqueCount());
				}
				path = new File(p, "resources" + File.separator + "sifb" + File.separator + codePath + 
				        File.separator + blockName);
				return;
			}
		}
		
		if (isAllDigit(tail) && qlen >= 0) {
			if (needsUnique) // except for here... we can tell if it needs unique, though we could GUESS if index ==0 then the answer is no...
				uniqueID = Integer.toString(getUniqueCount());
			index = Integer.parseInt(tail);
			anyType = new DataType[index];
			if ( globalVarName != null && index >= 0 ) {
				if (index == 0) {
					String sharedType = FBType.getGlobalVarType(blockName + "_" + tail);
					if (sharedType != null)
						globalVar.setTypeName(sharedType);
				}
				VarDeclaration[] members = new VarDeclaration[index];
				for (int i = 1; i <= index; i++)
					members[i-1] = new VarDeclaration(memberName + i, null, ""); 
				globalVar.setMembers(members);
				CodeGenerator.globalVars.put(globalVarName, globalVar);
			}
		}
		else {
			blockName = n;
			index = -1;
		}
		
		path = new File(p, "resources" + File.separator + "sifb" + File.separator + codePath + 
		        File.separator + blockName);		
		if (!path.exists()) {
			OutputManager.printError(n, "SIFB: `" + blockName + "\' is undefined.", OutputLevel.FATAL);
			Exception e = new Exception(); e.printStackTrace();
			System.exit(-1);
		}
		/*
		else {
			// Check for SIFB dependencies
			File dependency = new File(path.getPath(), "dependencies.txt");
			try {
				if (dependency.exists()) {
					BufferedReader in = new BufferedReader(new FileReader(dependency.getPath()));
					String line;
					while ((line = in.readLine()) != null) {
						line = line.trim();
						if (!line.isEmpty())
							CodeGenerator.srcDependencies.add(line);
					}
				}
			}
			catch (FileNotFoundException e) {
				OutputManager.printError(n, dependency.getPath() + ": Could not be opened.\n", 
						OutputLevel.FATAL);
				System.exit(-1);
			}
			catch (IOException e) {
				OutputManager.printError(n, e.getMessage(), OutputLevel.FATAL);
				System.exit(-1);
			}
		}
		*/
	}
	
	/**
	 * Extract parameter information to generate the name of a global variable
	 * @param fb function block type that results in a global variable
	 * @param params list of parameters to the function block
	 * @param varKind type of global variable to be generated
	 * @return Name of the global variable generated.
	 */
	private String paramToGlobalVar(String fb, Parameter[] params, GlobalVarType varKind) {
		if (params == null) {
			OutputManager.printError(fb, "Expecting `ID\' parameter.", OutputLevel.FATAL);
			System.exit(-1);
		}
		for (Parameter param : params) {
			if (param.name.equals("ID")) {
				if (param.value == null) {
					OutputManager.printError(fb, "Expected valid identifier for `ID\' parameter.",
							OutputLevel.FATAL);
					System.exit(-1);
				}
				
				param.value = param.value.replace("\"", "");
				if (!param.value.matches("\\w+")) {
					// param.value must be a word [a-zA-Z_0-9]
					OutputManager.printError(fb, "Expected valid identifier for `ID\' parameter.",
							OutputLevel.FATAL);
					System.exit(-1);
				}
				else if (Character.isDigit(param.value.charAt(0))) {
					// param.value cannot start with a digit
					OutputManager.printError(fb, "Expected valid identifier for `ID\' parameter.",
							OutputLevel.FATAL);
					System.exit(-1);
				}
				
				globalVar = CodeGenerator.globalVars.get(param.value); 
				if (globalVar != null)
					return param.value;
				
				VarDeclaration status;
				switch (varKind) {
					case PLAIN:
						globalVar = new GlobalVar();
						break;
					case UCOS:
						status = new VarDeclaration("status", "USINT", "");
						VarDeclaration[] v1 = new VarDeclaration[1];
						v1[0] = new VarDeclaration("buf", "ANY", "");
						VarDeclaration[] v2 = new VarDeclaration[2];
						v2[0] = new VarDeclaration("msg", "ANY", "");
						v2[1] = new VarDeclaration("sample", "UINT", "");
						VarDeclaration[] v3 = new VarDeclaration[1];
						v3[0] = v2[1];
						String type = "T" + param.value;
						globalVar = new GlobalVar(param.value, null, status, 
								new Prototype("USINT", type + "_readMsg", v1), 
								new Prototype("", type + "_sendMsg", v2),
								new Prototype("", type + "_sendNull", v3));
						break;
					case PUBSUB:
						status = new VarDeclaration("status", "USINT", "");
						globalVar = new GlobalVar(param.value, status);
						break;
				}
				
				return param.value;
			}
		}
		if (globalVar == null) {
			OutputManager.printError(fb, "Expecting `ID\' parameter.", OutputLevel.FATAL);
			System.exit(-1);
		}
		return null;
	}
	
	/**
	 * Get the whole postfix inc:
	 * (index & queue length & uniqueID)
	 * @return
	 */
	public String getPostfix() {
		String postfix = "";
		if( index >= 0 )
			postfix += "_" + index;
		if (qlen > 0)
			postfix += "_" + get_QLen();
		if (uniqueID != null && uniqueID.length() > 0)
			postfix += "_" + uniqueID;
		return postfix;
	}
	
	/**
	 * Get the index as a postfix string:
	 * @return
	 */
	public String getIndexPostfix() {
		String postfix = "";
		if( index >= 0 )
			postfix += "_" + index;
		return postfix;
	}
	
	/**
	 * Get the part of the postfix that is UNIQUE (only uniqueID)
	 * not (index & queue length)
	 * @return
	 */
	public String getUniquePostfix() {
		String postfix = "";
		if (uniqueID != null) {
			if (uniqueID.length() > 0)
				postfix += "_" + uniqueID;
		}
		return postfix;
	}
	
	public int getIndex() {
		return index;
	}
	
	public String getPath() {
		return path.getPath() + File.separator;
	}
	
	public String getBlockName() {
		return blockName;
	}
	
	public int getQLen() {
		return qlen;
	}
	
	public int getPositiveQLen() {
		if (qlen < 0)
			return 0;
		return qlen;
	}
	
	public String get_QLen() {
		if (qlen <= 0)
			return "";
		return Integer.toString(qlen);
	}
	
	public static boolean isAllDigit(String number) {
		if (number == null)
			return false;
		if (number.isEmpty())
			return false;
		
		char[] num = number.toCharArray();
		for (int i = 0; i < num.length; i++) {
			if (!Character.isDigit(num[i]))
				return false;
		}
		return true;
	}

	/* Checks if the SIFB has any data ports of type ANY. If it does, this 
	 * function returns TRUE if the ANY type has been bound to a concrete type; 
	 * otherwise, FALSE. If the SIFB has no data ports of type ANY, this 
	 * function always returns TRUE.
	 */
	public boolean resolvedAny() {
		if (anyType == null)
			return true;
		if (anyType[0].getType() == null)
			return false;
		return true;
	}
	
	public static void clearAnyVarsMap() {
		anyVarsMap.clear();
	}
	
	public boolean defineAnyPort(String fqInstanceName, String portName, DataType dt) {
		if (anyType == null)
			return false;
		
		String[] tokens = portName.split("_");
		if (tokens.length != 2)
			return false;
		
		if (isAllDigit(tokens[1])) {
			//addToVarList(fqInstanceName, tokens[0]+"_", portName, dt.getType());
			addToAnyVarsMap(fqInstanceName, portName, dt);
			int portIndex = Integer.parseInt(tokens[1])-1; // [0] == SD_1 etc
			if( anyType.length <= portIndex) {
				DataType[] newArray = new DataType[portIndex+1];
				for(int t = 0; t < anyType.length; t++) {
					newArray[t] = anyType[t];
				}
				anyType = newArray;
			}
			anyType[portIndex] = dt;
			
			if (globalVar != null) 
				globalVar.setMemberType(tokens[1], dt);
		}		
		return true;
	}
	
	public static void addToVarList(String fqInstanceName, String identifier, String var, String value)
	{
		LinkedHashMap<String,String> varlist = varLists.get(fqInstanceName+"."+identifier);
		if( varlist == null )
		{
			varlist = new LinkedHashMap<String,String>();
			varLists.put(fqInstanceName+"."+identifier, varlist);
		}
		varlist.put(var,value); // order maintained.. but in case of connections.. is out of numbered order
	}
	
	private static void addToAnyVarsMap(String fqInstanceName, String portName, DataType dt) {
		HashMap<String, DataType> anyPortMap = anyVarsMap.get(fqInstanceName);
		if (anyPortMap == null) {
			anyPortMap = new HashMap<String, DataType>();
			anyVarsMap.put(fqInstanceName, anyPortMap);
		}
		anyPortMap.put(portName, dt);
	}
	
	public int numOfAny() {
		if (anyType != null)
			return anyType.length;
		return 0;
	}
	
	final public DataType[] getAnyTypes() {
		if (anyType == null)
			return new DataType[0];
		return anyType;
	}
	
	public String[] getToAnyConverterDecl(int i) {
		String type = anyType[i].getType();
		if (type == null)
			type = "ANY";
		String[] function = new String[2];
		function[0] = type + "toANY";
		function[1] = type;
		return function;
	}
	
	public String getToAnyConverterCall(int i) {
		String type = anyType[i].getType();
		if (type == null)
			type = "ANY";
		return type + "toANY";
	}
	
	public String[] getAnyToConverterDecl(int i) {
		String type = anyType[i].getType();
		if (type == null)
			type = "ANY";
		String[] function = new String[2];
		function[0] = "ANYto" + type;
		function[1] = type;
		return function;
	}
	
	public String getAnyToConverterCall(int i) {
		String type = anyType[i].getType();
		if (type == null)
			type = "ANY";
		return "ANYto" + type;
	}

	public void setCountValue(String countName, int countValue) {
		counts.put(countName, Integer.valueOf(countValue));
		OutputManager.printNotice(blockName, "SIFB index set to some counter's value", OutputLevel.DEBUG);
		index = countValue; // Keep this?
	}
	
	public int getCountValue(String countName)
	{
		return counts.get(countName);
	}
	
	public static int getUniqueCount() {
		return ++counter;
	}
	
	public LinkedHashMap<String,String> getNameOrderedList(LinkedHashMap<String,String> map, String varname)
	{
		LinkedHashMap<String,String> ordered = new LinkedHashMap<String,String>();
		Set<String> keys = map.keySet();
		
		int maxIndex = 0;
		for(String key : keys)
		{
			try
			{
				int id = Integer.valueOf( key.replace(varname, "") );
				maxIndex = Math.max(id, maxIndex);
			}
			catch( NumberFormatException e )
			{
				// Don't make it ordered
				return map;
			} 
		}
		for(int index = 1; index <= maxIndex; index++)
		{
			ordered.put(varname+index, map.get(varname+index));
		}
		
		
		return ordered;
	}
	
	public LinkedHashMap<String,String> getVarOrderedList(LinkedHashMap<String,String> map, String varname)
	{
		LinkedHashMap<String,String> ordered = new LinkedHashMap<String,String>();
		Set<String> keys = map.keySet();
		
		int maxIndex = 0;
		for(String key : keys)
		{
			try
			{
				int id = Integer.valueOf( key );
				maxIndex = Math.max(id, maxIndex);
			}
			catch( NumberFormatException e )
			{
				// Don't make it ordered
				return map;
			} 
		}
		for(int index = 1; index <= maxIndex; index++)
		{
			ordered.put(""+index, map.get(""+index));
		}
		
		return ordered;
	}
	
	public LinkedHashMap<String,String> getOrderedList(String fqInstanceName, String varname)
	{
		LinkedHashMap<String,String> map = varLists.get(fqInstanceName+"."+varname);
		if( map == null ) 
			return null;
		Set<String> keys = map.keySet();
		if( keys.contains(varname+"1") ) // Most common usage from template
			return getNameOrderedList(map, varname);
		if( keys.contains("1") ) // no prefix is used in the case of FB Parameters (See Instance.java:68)
			return getVarOrderedList(map, ""); // TODO: change to "getNameOrderedList(map, "");"?  
		else
			return map; // dunno
	}
	
	/**
	 * Creates an ordered map containing the list of ANY ports with their corresponding
	 * data type.
	 * @param fqInstanceName fully qualified instance name of a function block
	 * @param prefix prefix of the set of ANY ports
	 * @return Ordered map containing the list of ANY ports with their corresponding data 
	 * type
	 */
	public LinkedHashMap<String, DataType> getOrderedMap(String fqInstanceName, String prefix) {
		HashMap<String, DataType> anyPortMap = anyVarsMap.get(fqInstanceName);
		if (anyPortMap == null)
			return null;
		
		int min = -1;
		int max = -2;	// set max < min to prevent spurious port names if map is empty
		Set<String> allPorts = anyPortMap.keySet();
		for (String port : allPorts) {
			if (port.startsWith(prefix)) {
				String tail = port.substring(prefix.length());
				if (isAllDigit(tail)) {
					int num = Integer.parseInt(tail);
					if (min == -1) {
						min = num;
						max = num;
					}
					else if (num < min) {
						min = num;
					}
					else if (num > max) {
						max = num;
					}
				}
			}
		}
		
		LinkedHashMap<String, DataType> orderedMap = new LinkedHashMap<String, DataType>();
		for (int i = min; i <= max; i++) {
			String portName = prefix + Integer.toString(i);
			DataType dt = anyPortMap.get(portName);
			if (dt != null)
				orderedMap.put(portName, dt);
		}
		return orderedMap;
	}
	
	/**
	 * Returns the list of ANY types bound to user-defined types for this SIFB.
	 * @param fqInstanceName fully-qualified instance name of this SIFB
	 * @return List of user-defined types
	 */
	public ArrayList<String> getUserTypesForPorts(String fqInstanceName) {
		ArrayList<String> userDefinedTypes = new ArrayList<String>();
		HashMap<String, DataType> anyPortMap = anyVarsMap.get(fqInstanceName);
		if (anyPortMap != null) {
			Set<String> allPorts = anyPortMap.keySet();
			for (String port : allPorts) {
				String type = anyPortMap.get(port).getType();
				if ( !IEC61131Types.isIEC61131Type(type) )
					userDefinedTypes.add(type);
			}
		}
		return userDefinedTypes;
	}

	public boolean needsUnique() {
		return needsUnique;
	}
	
	public GlobalVar getGlobalVar() {
		return globalVar;
	}
	
	public final String getGlobalVarType() {
		if (globalVar == null)
			return "";
		return globalVar.getTypeName();
	}
	
	public void addToVarStringMap(String key, String value) {
		if (varStringMap == null)
			varStringMap = new HashMap<String, String>();
		varStringMap.put(key, value);
	}
	
	public String getFromVarStringMap(String key) {
		if (varStringMap == null)
			return "";
		return varStringMap.get(key);
	}
}
