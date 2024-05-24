/* Composite.java
 * Defines the composite function block.
 *
 * Written by: Li Hsien Yoong
 */

package ir;

import java.util.*;

import ccode.CGenerator;
import fb.*;
import fbtostrl.OutputLevel;
import fbtostrl.OutputManager;

public class Composite {
	public Instance[] runs;
	String fqinstanceName;
	// TODO: Handled in CGenerator.. not other's yet
	// Direct connections from Composite's Interface to Interface
	HashMap<Signal,Signal> directConnections = new HashMap<Signal,Signal>(); 
	// Method connections from Composite's Interface to Interface
	// Kyle: HashMap<Signal,Signal> directMethodConnections = new HashMap<Signal,Signal>();

	Composite(FBType fbt, String fqInstName) {
		fqinstanceName = fqInstName;
		FBNetwork f = fbt.getFBNetwork();
		if (f.instances == null) {
			runs = new Instance[0];	// empty network
			return;
		}
		int len = f.instances.length;
		runs = new Instance[len];
		ArrayList<Arc> arcs = new ArrayList<Arc>();
		// Kyle: ArrayList<Arc> arcs2 = new ArrayList<Arc>();
		if (f.eventConnections != null) {
			for (int j = 0; j < f.eventConnections.length; j++)
				arcs.add(new Arc(f.eventConnections[j], f.instances));
		}
		
		/* Kyle:
		if (f.methodConnections != null) {
			for (int j = 0; j < f.methodConnections.length; j++)
				arcs2.add(new Arc(f.methodConnections[j], f.instances));
		}
		*/
		
		FBType[] blocks = fbt.getSubBlocks();
		if (f.dataConnections != null) {
			for (int j = 0; j < f.dataConnections.length; j++) {
				arcs.add(new Arc(fbt.getInterfaceList(), f.dataConnections[j], 
						         f.instances, blocks));
			}
		}
		
		for (int i = 0; i < len; i++)
			runs[i] = new Instance(f.instances[i], arcs, blocks, fqinstanceName);
		
		for(Arc a : arcs) {
			if( a.destination.instance.length() == 0 && a.source.instance.length() ==0 ) {
				Signal src = new Signal(a.source, PortType.INPUT);
				Signal dest = new Signal(a.destination, PortType.OUTPUT);
				directConnections.put(src, dest);
			}
		}
		
		/* Kyle:
		for(Arc a : arcs2) {
			Signal src = new Signal(a.source, PortType.INPUT);
			Signal dest = new Signal(a.destination, PortType.OUTPUT);
			directMethodConnections.put(src, dest);
		}
		*/
		
		// Attempt to remove ANY type from encapsulated instances and from this.interface
		Composite.removeAny(runs, null);
	}
	
	public static void removeAny(Instance[] instances, Signal resolved) {
		for (Instance inst : instances) {
			HashSet<String> anyPorts = inst.getAnyPorts();
			Set<Signal> signals = inst.getSignalSet();
			for (Signal sig : signals) {
				String sigType = sig.getSigType(); 
				if (sigType == null)
					continue;
				if ( !sigType.equalsIgnoreCase("ANY") )
					continue;
				
				String sigName = sig.getSignal();
				if ( anyPorts.contains(sigName) )
					anyPorts.remove(sigName);
				
				HashSet<Signal> bindings = inst.getBindings(sig);
				for (Signal bind : bindings) {
					if (bind.getSigType() == null)
						continue;
					if (bind.equals(resolved)) {
						inst.removeAnyType(sig, 
								new DataType(resolved.getSigType(), resolved.getArraySize()));
						// For the case where an ANY port is bound to a concrete type as
						// well as to other ANY ports, those other ANY ports can be 
						// resolved here as well.
						resolveAnyPeers(instances, bindings, bind);
						break;
					}
					else if ( !bind.getSigType().equalsIgnoreCase("ANY") ) {
						inst.removeAnyType(sig, 
								new DataType(bind.getSigType(), bind.getArraySize()));
						// For the case where an ANY port is bound to a concrete type as
						// well as to other ANY ports, those other ANY ports can be 
						// resolved here as well.
						resolveAnyPeers(instances, bindings, bind);
						break;
					}
					else if ( bind.getPortType().equals(PortType.PARAM) ) {
						// The ANY port is bound to a parameter. We now have to guess the
						// concrete type based on the parameter.
						// If parameter is not a primitive type, we fail with a clean error!
						inst.removeAnyType(sig, guessParamType(bind, sigName, inst.getFQName()));
						break;
					}
				}
			}
			
			// If all ANY ports have been bound, "anyPorts" would be empty. Otherwise, 
			// some ANY ports remain unresolved. If the unresolved port belongs to an SIFB,
			// we still need to add it to the ANY resolution map.
			for (String sigName : anyPorts)
				inst.registerAnyType(sigName);
		}
		// TODO: Also Check if an input Port is Connected to an output port
		//directConnections
	}
	
	/**
	 * This method is called to resolve ANY ports that are bound to a peer ANY port that
	 * has already been resolved to a concrete type.
	 * @param instances List of all function block instances in this network.
	 * @param bindings Set of peer signal ports (bound to a mutual source)
	 * @param bind Peer ANY port that has been resolved
	 */
	private static void resolveAnyPeers(Instance[] instances, HashSet<Signal> bindings, Signal bind) {
		for (Signal other : bindings) {
			if (other == bind || other.getSigType() == null)
				continue;
			
			// Search for the function block instance that contains the "other" signal.
			Instance inst = null;
			for (int i = 0; i < instances.length; i++) {
				if ( instances[i].name.equals(other.getInstance()) ) {
					inst = instances[i];
					break;
				}
			}
			if (inst == null) {
				// If the function block instance that contains the "other" signal cannot
				// be found, we simply give up.
				return;
			}
			
			if ( other.getSigType().equalsIgnoreCase("ANY") )
				inst.removeAnyType(other, new DataType(bind.getSigType(), bind.getArraySize()));
		}
	}
	
	/**
	 * Attempts to guess the type of a parameter.
	 * @param bind the parameter
	 * @param sigName port to which the parameter is bound
	 * @param fqName fully qualified function block instance name to which the port belongs
	 * @return The data type that has been guessed from the given parameter.
	 */
	private static DataType guessParamType(Signal bind, String sigName, String fqName) {
		// Check if the parameter is an array type
		ArrayList<String> params = new ArrayList<String>();
		String arraySize;
		if ( parseParamArray(params, bind, sigName) )
			arraySize = Integer.toString(params.size());
		else {
			arraySize = "";
			params.add(bind.getSignal().trim());
		}
		
		String type;
		DataType dt = new DataType(null, arraySize);
		for (String param : params) {
			// First, check for string types.
			if (param.startsWith("'") && param.endsWith("'"))
				type = "STRING";
			else if (param.startsWith("\"") && param.endsWith("\""))
				type = "WSTRING";
			else {				
				// Next, check for numeric types and duration type.  
				// Since parameters can now only be of numeric or duration types, we 
				// convert everything to upper case to simplify checking.
				param = param.toUpperCase();
				if (param.startsWith("BOOL#") || param.equals("TRUE") || param.equals("FALSE"))
					type = "BOOL";
				else if (param.startsWith("SINT#"))
					type = "SINT";
				else if (param.startsWith("INT#"))
					type = "INT";
				else if (param.startsWith("DINT#"))
					type = "DINT";
				else if (param.startsWith("LINT#"))
					type = "LINT";
				else if (param.startsWith("USINT#"))
					type = "USINT";
				else if (param.startsWith("UINT#"))
					type = "UINT";
				else if (param.startsWith("UDINT#"))
					type = "UDINT";
				else if (param.startsWith("ULINT#"))
					type = "ULINT";
				else if (param.startsWith("REAL#"))
					type = "REAL";
				else if (param.startsWith("LREAL#"))
					type = "LREAL";
				else if (param.startsWith("BYTE#"))
					type = "BYTE";
				else if (param.startsWith("WORD#"))
					type = "WORD";
				else if (param.startsWith("DWORD#"))
					type = "DWORD";
				else if (param.startsWith("LWORD#") || param.startsWith("2#") ||
						 param.startsWith("8#") || param.startsWith("16#"))
					type = "LWORD";
				else if (param.startsWith("TIME#") || param.startsWith("T#"))
					type = "TIME";
				else {
					// We are now left with plain numeric literals. First, we check for
					// integers.
					boolean parsed = true;
					try {
						Long.parseLong(param);
					} catch (NumberFormatException e) {
						parsed = false;
					}
					
					if (parsed)
						type = "LINT";
					else {
						parsed = true;	// reset parse status
						try {
							Double.parseDouble(param);
						} catch (NumberFormatException e) {
							parsed = false;
							
							// We have exhausted our ability to guess the type of parameter.
							// Give up with an error message.
							OutputManager.printError(fqName, "Port `" + sigName + 
									"' is bound to a parameter of unknown type.", OutputLevel.FATAL);
							System.exit(0);
						}
						type = "LREAL";
					}
				}
			}
			String prevType = dt.getType(); 
			if (prevType == null)
				dt.setType(type);
			else if ( !prevType.equals(type) ) {
				// We have guessed the type differently for different elements of the 
				// parameter array. 
				// We check if this is just a confusion between LINT and LREAL before 
				// treating this as an error.
				if ( prevType.equals("LINT") && type.equals("LREAL") ) {
					dt.setType("LREAL");
				}
				else if ( !(prevType.equals("LREAL") && type.equals("LINT")) ) {
					// If the previous type is an LREAL and now we guess an LINT, we can 
					// keep it as an LREAL. Otherwise, we report an error. 
					OutputManager.printError(fqName, "Port `" + sigName + 
							"' is bound to a parameter of unknown type.", OutputLevel.FATAL);
					System.exit(0);
				}
			}
		}
		
		return dt;
	}
	
	/**
	 * Checks if a parameter is an array. If it is, it tokenizes each array element and
	 * stores each one in the "params" array list. 
	 * @param params array list containing each element of the parameter array
	 * @param bind signal representing the parameter
	 * @param sigName name of the port that is initialized by the parameter
	 * @return TRUE if the parameter is an array; FALSE otherwise.
	 */
	public static boolean parseParamArray(ArrayList<String> params, Signal bind, 
			String sigName) {
		String paramValue = bind.getSignal().trim();
		int lastChar = paramValue.length() - 1;
		if (paramValue.charAt(0) != '[' || paramValue.charAt(lastChar) != ']') {
			// This is not an array
			return false;
		}
		
		paramValue = paramValue.substring(1, lastChar).trim();
		String[] tokens;
		
		// Check for string types
		char firstChar = paramValue.charAt(0);
		if (firstChar == '\'') {
			tokens = CGenerator.getStringInitializers(paramValue, '\'', sigName);
			StringBuilder str = new StringBuilder();
			// Need to replace the quotes around each string, since they would be using 
			// C-style quotes.
			for (int i = 0; i < tokens.length; i++) {
				str.replace(0, str.length(), tokens[i]);
				str = new StringBuilder(tokens[i]);
				str.setCharAt(0, '\'');
				str.setCharAt(tokens[i].length()-1, '\'');
				tokens[i] = str.toString();
			}
		}
		else if (firstChar == '\"') {
			tokens = CGenerator.getStringInitializers(paramValue, '\"', sigName);
		}
		else
			tokens = paramValue.split("[\\,\\s]");
		
		for (String token : tokens) {
			if ( !token.trim().isEmpty() )
				params.add(token);
		}
		return true;
	}
		
	public HashMap<Signal,Signal> getDirectConnections() {
		return directConnections;
	}
	
	/* Kyle:
	public HashMap<Signal,Signal> getDirectMethodConnections() {
		return directMethodConnections;
	}
	*/
}
