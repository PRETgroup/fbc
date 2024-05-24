/* Instance.java
 * Helper class to describe a module instance.
 *
 * Written by: Li Hsien Yoong
 */

package ir;

import java.util.*;

import fb.*;
import fbtostrl.FBtoStrl;
import fbtostrl.OutputLevel;
import fbtostrl.OutputManager;

public class Instance extends FBInstance {
	private FunctionBlock compiledFB;
	private String compiledType;
	
	private PortMap portMap = new PortMap();
	String parentName;
	String fqinstanceName;
	
	private boolean isSocket = false;
	private boolean isPlug = false;

	public Instance(FBInstance f, ArrayList<Arc> arcs, FBType[] subs, String pn) {
		super(f.name, f.type, f.params);
		parentName = pn;
		if( pn.length() > 0)
			fqinstanceName = pn + "." + f.name;
		else
			fqinstanceName = f.name;
		
		if( f instanceof Socket )
			isSocket = true;
		else if( f instanceof Plug )
			isPlug = true;
		
		Iterator<FunctionBlock> i = FunctionBlock.fbs.iterator();
		while (i.hasNext()) {
			FunctionBlock mod = i.next();
			
			// Superclass' TYPE now holds the generic name (e.g., SUBSCRIBE_5_
			// compiledType holds the ACTUAL name
			if (mod.isUnique()) {
				if ( mod.fqinstanceName.equals(fqinstanceName) && mod.fbType.equals(type) ) {
					compiledFB = mod;
					break;
				}
			}
			else {
				if (mod.compiledType.equals(type)) {
					compiledFB = mod;
					break;
				}
			}
		}
		if (compiledFB == null) {
			OutputManager.printError(fqinstanceName, "Cannot find instance type among known function blocks.\n", 
					OutputLevel.FATAL);
			System.exit(-1);
		}
		compiledType = compiledFB.getCompiledType();
		
		if (params != null) {
			for (int j = 0; j < params.length; j++) {
				Parameter param = params[j];
				String[] sigInfo = Instance.getSignalInfo(type, param.name, subs);
				portMap.add(new PortName(name, type, param.name, sigInfo[0], sigInfo[1]), PortType.INPUT, 
						    new PortName("", "", param.value, sigInfo[0], sigInfo[1]), PortType.PARAM);
				
			}
			if( compiledFB != null && compiledFB.isUnique() )
				addParamsToVarList();
		}
		
		ListIterator<Arc> j = arcs.listIterator();
		while (j.hasNext()) {
			Arc arc = j.next();
			PortType binding = PortType.LOCAL;
			if (arc.isDelayed)
				binding = PortType.LOCALDELAYED;
			if (arc.destination.instance.equals(name)) {
				if (arc.source.instance.isEmpty())
					binding = PortType.INPUT;
				portMap.add(arc.destination, PortType.INPUT, arc.source, binding);
			}
			else if (arc.source.instance.equals(name)) {
				if (arc.destination.instance.isEmpty())
					binding = PortType.OUTPUT;
				portMap.add(arc.source, PortType.OUTPUT, arc.destination, binding);
			}
		}
		
		if( compiledFB != null && compiledFB.isUnique() )
			getVariableSizeInputs();
	}
	
	private void addParamsToVarList() {
		String[] values = new String[1];
		
		for(Parameter param : params)
		{
			values[0] = param.value;
			// ALTHOUGH
			if( param.value.startsWith("[") ) // If it is an array split it up
			{
				values[0] = values[0].substring(1, values[0].length()-1);
				values = values[0].split(",");
			}
			for(int v = 0; v < values.length; v++ )
				SIPath.addToVarList(fqinstanceName, param.name, ""+(v+1), values[v]);
		}
	}

	private void getVariableSizeInputs() {
		// Find I/O with @CNT*@
		if( compiledFB.inputEvents != null )
			for(Event e : compiledFB.inputEvents )
			{
				if( e.getName().contains("@CNT") )
					setVariableInputSize(e.getName(), false);
				else if( e.getName().contains("${CNT}") )
					setVariableInputSize(e.getName(), true);									
			}
		
		if( compiledFB.inputData != null )
			for(VarType v : compiledFB.inputData )
			{
				if( v.getName().contains("@CNT") )
					setVariableInputSize(v.getName(), false);
				else if( v.getName().contains("${CNT}") )
					setVariableInputSize(v.getName(), true);									
			}
	
		if( compiledFB.outputEvents != null )
			for(Event e : compiledFB.outputEvents )
			{
				if( e.getName().contains("@CNT") )
					setVariableInputSize(e.getName(), false);
				else if( e.getName().contains("${CNT}") )
					setVariableInputSize(e.getName(), true);	
			}
		
		if( compiledFB.outputData != null )
			for(VarType v : compiledFB.outputData )
			{
				if( v.getName().contains("@CNT") )
					setVariableInputSize(v.getName(), false);
				else if( v.getName().contains("${CNT}") )
						setVariableInputSize(v.getName(), true);
			}	
	}

	public void setVariableInputSize(String portName, boolean nxtStudioFormat) {
		// Get CNT Name
		String cntPrefix, cntName, cntSuffix;
		int cntValue = 0;
		String tempStr = portName;
		
		if( nxtStudioFormat )
		{
			cntPrefix = tempStr.substring(0, tempStr.indexOf("${CNT}"));
			cntName = "CNT";
			cntSuffix = tempStr.substring(tempStr.lastIndexOf("${CNT}")+6);
		}
		else
		{
			cntPrefix = tempStr.substring(0, tempStr.indexOf("@"));
			cntName = tempStr.substring(tempStr.indexOf("@")+1, tempStr.lastIndexOf("@") );
			cntSuffix = tempStr.substring(tempStr.lastIndexOf("@")+1);
		}
		
		if( cntName.length() < 1 ) 
			return;
		
		// Update inputs for ANY types
		Set<Signal> signals = getSignalSet();
		Iterator<Signal> j = signals.iterator();
			
		while (j.hasNext()) {
			Signal sig = j.next();
			if( (cntPrefix.length() > 0 && sig.getSignal().startsWith(cntPrefix)) 
					|| 
				(cntSuffix.length() > 0 && sig.getSignal().endsWith(cntSuffix)) )
			{
				cntValue++;
				SIPath.addToVarList(fqinstanceName, cntPrefix, sig.getSignal(), "");
			}
		}
		
		FBtoStrl.varSizeMap.put(getFQName()+"."+cntName, ""+cntValue);
	}


	/* DESCRIPTION: Returns the type information for a given valued signal
	 * ARGUMENTS: fbType - the function block type of the given valued signal
	 *            sigName - the signal whose type is in question
	 *            subBlocks - list of all sub-blocks inside the function block
	 * RETURNS: the data type for sigName
	 */
	static String[] getSignalInfo(String fbType, String sigName, FBType[] subBlocks) {
		for (int j = 0; j < subBlocks.length; j++) {
			if ( fbType.equals(subBlocks[j].getName()) ) {
				InterfaceList iface = subBlocks[j].getInterfaceList();
				if (iface.inputVars != null) {
					for (int i = 0; i < iface.inputVars.length; i++) {
						if ( sigName.equals(iface.inputVars[i].getName()) ) {
							String[] typeInfo = {iface.inputVars[i].getType(), 
									             iface.inputVars[i].getArraySize()};
							return typeInfo;
						}
						else if( iface.inputVars[i].getName().contains("@CNT@") ) // TODO: Variable counter NAMEs @CNT<name>@
						{
							String testName = iface.inputVars[i].getName().split("@CNT@")[0];
							if(sigName.startsWith(testName))
							{	
								String[] typeInfo = {iface.inputVars[i].getType(), 
							             iface.inputVars[i].getArraySize()};
								return typeInfo;
							}
						}
						else if( iface.inputVars[i].getName().contains("${CNT}") ) // nxtStudio format
						{
							String testName = iface.inputVars[i].getName().split("\\$\\{CNT\\}")[0];
							if(sigName.startsWith(testName))
							{	
								String[] typeInfo = {iface.inputVars[i].getType(), 
							             iface.inputVars[i].getArraySize()};
								return typeInfo;
							}
						}
					}
				}
				if (iface.outputVars != null) {
					for (int i = 0; i < iface.outputVars.length; i++) {
						if ( sigName.equals(iface.outputVars[i].getName()) ) {
							String[] typeInfo = {iface.outputVars[i].getType(), 
						                         iface.outputVars[i].getArraySize()};
							return typeInfo;
						}
						else if( iface.outputVars[i].getName().contains("@CNT@") )
						{
							String testName = iface.outputVars[i].getName().split("@CNT@")[0];
							if(sigName.startsWith(testName))
							{	
								String[] typeInfo = {iface.outputVars[i].getType(), 
							             iface.outputVars[i].getArraySize()};
								return typeInfo;
							}
						}
						else if( iface.outputVars[i].getName().contains("${CNT}") )
						{
							String testName = iface.outputVars[i].getName().split("\\$\\{CNT\\}")[0];
							if(sigName.startsWith(testName))
							{	
								String[] typeInfo = {iface.outputVars[i].getType(), 
							             iface.outputVars[i].getArraySize()};
								return typeInfo;
							}
						}
					}
				}
				OutputManager.printError(fbType, "Signal `" + sigName + "\' cannot be found.\n", 
						OutputLevel.FATAL);
				System.exit(0);
			}
		}
		return null;
	}
	
	static String remove(String str, char c) {
		char[] oldString = str.toCharArray();
		char[] newString = new char[oldString.length];
		int j = 0;
		for (int i = 0; i < oldString.length; i++) {
			if (oldString[i] != c) {
				newString[j] = oldString[i];
				j++;
			}
		}
		return new String(newString, 0, j);
	}
	
	public String getType() {
		return compiledType;
	}
	
	/*public String getPostfix() {
		return postfix;
	}*/
	
	public Set<Signal> getSignalSet() {
		return portMap.getSignalSet();
	}
	
	public HashSet<Signal> getBindings(Signal sig) {
		return portMap.getBindings(sig);
	}

	public boolean isPlug() {
		return isPlug;
	}

	public boolean isSocket() {
		return isSocket;
	}

	/**
	 * Remove the ANY type of a signal (sig), by setting it to a non-ANY type.
	 * @param sig signal of type ANY that has been resolved to a concrete type
	 * @param dt concrete data type
	 */
	public void removeAnyType(Signal sig, DataType dt) {
		sig.setSigType(dt.getType());
		sig.setArraySize(dt.getArraySize());
		compiledFB.resolveAny(sig.getSignal(), sig.getSigType());
		if (compiledFB.sifbInfo != null)
			compiledFB.sifbInfo.defineAnyPort(fqinstanceName, sig.getSignal(), dt);
		if (compiledFB.compositeFB != null) {
			// We have to make a clone of the resolved ANY signal, as the instance and 
			// type information of the parent block is hidden from the child blocks in the
			// encapsulated network.
			Signal parentSig = new Signal(sig.getSignal(), dt.getType(), sig.getArraySize(), 
					sig.getPortType());
			Composite.removeAny(compiledFB.compositeFB.runs, parentSig);	// recurse
		}
	}
	
	/**
	 * Register the ANY type of a signal in a map if it's not bound to a concrete type.
	 * This is necessary only if the ANY signal belongs to an SIFB.
	 * @param sigName name of the signal of type ANY
	 */
	void registerAnyType(String sigName) {
		if (compiledFB.sifbInfo != null)
			compiledFB.sifbInfo.defineAnyPort(fqinstanceName, sigName, new DataType("ANY", ""));
	}
	
	/**
	 * Returns the names of all ANY ports of this function block instance.
	 * @return The set containing the names of all ANY ports for this instance.
	 */
	HashSet<String> getAnyPorts() {
		HashSet<String> anyPorts = new HashSet<String>();
		if (compiledFB.inputData != null) {
			for (VarType var : compiledFB.inputData) {
				if (var.getType().equalsIgnoreCase("ANY"))
					anyPorts.add(var.getName());
			}
		}
		if (compiledFB.outputData != null) {
			for (VarType var : compiledFB.outputData) {
				if (var.getType().equalsIgnoreCase("ANY"))
					anyPorts.add(var.getName());
			}
		}
		return anyPorts;
	}

	public String getFQName() {
		return fqinstanceName;	//parentName +"."+name; 
	}

	public void setType(String newType) {
		compiledType = newType;
	}
	
	public String toString() {
		StringBuilder s = new StringBuilder();
		/*if (params != null) {
			for (int i = 0; i < params.length; i++) {
				s.append("\"" + params[i].toString() + "\" ");
			}
		}*/
			
		return name + " : " + compiledType + " (" + s.toString() + ")";
	}
	
	public GlobalVar getGlobalVarForSIFB() {
		if (compiledFB == null)
			return null;
		if (compiledFB.sifbInfo == null)
			return null;
		return compiledFB.sifbInfo.getGlobalVar();
	}
	
	/**
	 * Returns the name of a global variable associated to this instance. A 
	 * global variable is associated to an instance only if this is an instance 
	 * of an SIFB with such properties. Otherwise, the empty string is returned.
	 */
	public String getGlobalVarName() {
		GlobalVar var = getGlobalVarForSIFB();
		Set<String> varNames = CodeGenerator.globalVars.keySet();
		for (String varName : varNames) {
			if (CodeGenerator.globalVars.get(varName).equals(var))
				return varName;
		}
		return "";
	}
}
