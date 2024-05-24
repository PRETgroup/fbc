/* Arc.java
 * Helper class to describe data/event arcs between function blocks.
 * 
 * Written by: Li Hsien Yoong
 */

package ir;

import fb.*;
import fbtostrl.OutputLevel;
import fbtostrl.OutputManager;

public class Arc {
	public PortName source;
	public PortName destination;
	public boolean isDelayed;
	
	// Constructor for event arcs
	Arc(Connection c, FBInstance[] instances) {
		extractNames(c.source, c.destination, instances);
		isDelayed = c.isDelayed;
	}

	// Constructor for data arcs
	Arc(InterfaceList iface, Connection c, FBInstance[] instances, FBType[] subs) {
		extractNames(c.source, c.destination, instances);
		isDelayed = c.isDelayed;

		if ( source.instance.isEmpty() ) {
			for (int i = 0; i < instances.length; i++) {
				if (destination.instance.equals(instances[i].name)) {
					String typeInfo[] = Instance.getSignalInfo(instances[i].type, 
                                                               destination.signal, subs);
					destination.sigType = typeInfo[0];
					destination.arraySize = typeInfo[1];
					break;
				}
			}
			
			if (iface.inputVars != null) {
				String sourceSigName = source.signal;
				if ( source.signal.contains("[") ) {
					sourceSigName = source.signal.substring(0,source.signal.indexOf("["));
				} 
				for (int i = 0; i < iface.inputVars.length; i++) {
					if ( sourceSigName.equals(iface.inputVars[i].getName()) ) {
						source.sigType = iface.inputVars[i].getType();
						source.arraySize = iface.inputVars[i].getArraySize();
						break;
					}
				}
				
				String[] arrays = new String[2];
				if ( !isCompatibleDataType(arrays) ) {
					OutputManager.printError(source.insType, "Port `" + source.signal + 
							"\' of type " + source.sigType + arrays[0] + " is bound to port `" + 
							destination.signal + "\' of type " + destination.sigType + 
							arrays[1] + " in " + destination.instance, OutputLevel.FATAL);
					System.exit(0);
				}
				if ( !isCoherentBinding(source.sigType, destination.sigType) ) {
					OutputManager.printError(source.insType, "Port `" + source.signal + 
							"\' of type " + source.sigType + " is bound to port `" + 
							destination.signal + "\' of type " + destination.sigType +
							" in " + destination.instance +
							".\nANY ports in parent blocks cannot be bound to non-ANY ports in child blocks.", 
							OutputLevel.FATAL);
					System.exit(0);
				}
			}
			else {
				source.sigType = destination.sigType;
				source.arraySize = destination.arraySize;
			}
		}
		else if ( destination.instance.isEmpty() ) {
			for (int i = 0; i < instances.length; i++) {
				if (source.instance.equals(instances[i].name)) {
					String[] typeInfo = Instance.getSignalInfo(instances[i].type, 
                                                               source.signal, subs); 
					source.sigType = typeInfo[0];
					source.arraySize = typeInfo[1];
					break;
				}
			}
			
			if (iface.outputVars != null) {
				for (int i = 0; i < iface.outputVars.length; i++) {
					if ( destination.signal.equals(iface.outputVars[i].getName()) ) {
						destination.sigType = iface.outputVars[i].getType();
						destination.arraySize = iface.outputVars[i].getArraySize();
						break;
					}
				}
				
				String[] arrays = new String[2];
				if ( !isCompatibleDataType(arrays) ) {
					OutputManager.printError(destination.insType, "Port `" + destination.signal + 
							"\' of type " + destination.sigType + arrays[1] + " is bound to port `" + 
							source.signal + "\' of type " + source.sigType + arrays[0] + " in " + 
							source.instance, OutputLevel.FATAL);
					System.exit(0);
				}
				if ( !isCoherentBinding(destination.sigType, source.sigType) ) {
					OutputManager.printError(destination.insType, "Port `" + destination.signal + 
							"\' of type " + destination.sigType + " is bound to port `" +
							source.signal + "\' of type " + source.sigType + " in " + 
							source.instance +
							".\nANY ports in parent blocks cannot be bound to non-ANY ports in child blocks.", 
							OutputLevel.FATAL);
					System.exit(0);
				}
			}
			else {
				destination.sigType = source.sigType;
				destination.arraySize = source.arraySize;
			}
		}
		else {
			for (int i = 0; i < instances.length; i++) {
				if (destination.instance.equals(instances[i].name)) {
					String[] typeInfo = Instance.getSignalInfo(instances[i].type, 
                                                               destination.signal, subs);
					if( typeInfo == null ) {
						OutputManager.printNotice(destination.instance, "typeInfo == null for " + 
								destination.signal, OutputLevel.DEBUG);
						continue;
					}
					destination.sigType = typeInfo[0];
					destination.arraySize = typeInfo[1];
				}
				if (source.instance.equals(instances[i].name)) { // Gareth removed 'else if' // for loop back connections :|
					String[] typeInfo = Instance.getSignalInfo(instances[i].type, 
                                                               source.signal, subs);
					if( typeInfo == null ){
						OutputManager.printNotice(source.instance, "typeInfo == null for " + 
								source.signal, OutputLevel.DEBUG);
						break;
					}
					source.sigType = typeInfo[0];
					source.arraySize = typeInfo[1];
				}
				if (source.sigType != null && destination.sigType != null)
					break;
			}
			
			String[] arrays = new String[2];
			if ( !isCompatibleDataType(arrays) ) {
				OutputManager.printError(source.instance, "Port `" + source.signal + 
						"\' of type " + source.sigType + arrays[0] + " in " + source.instance + 
						" is bound to port `" + destination.signal + "\' of type " + 
						destination.sigType + arrays[1] + " in " + destination.instance, 
						OutputLevel.FATAL);
				System.exit(0);
			}
		}
	}
	
	/* DESCRIPTION: Extract the function block instance and port names for the 
	 *              given connection 
	 * ARGUMENTS: source - source of the connection pair
	 *            destination - destination of the connection pair
	 */
	private void extractNames(String src, String dst, FBInstance[] instances) {
		if ( src.contains(".") ) {
			int index = src.lastIndexOf('.');
			String insName = src.substring(0, index);
			source = new PortName(insName, getInstanceType(insName, instances), 
					              src.substring(index + 1));
		}
		else {
			source = new PortName("", "", src);
		}
		
		if ( dst.contains(".") ) {
			int index = dst.lastIndexOf('.');
			String insName = dst.substring(0, index);
			destination = new PortName(insName, getInstanceType(insName, instances),
					                   dst.substring(index + 1));
		}
		else {
			destination = new PortName("", "", dst);
		}
	}
	
	private String getInstanceType(String insName, FBInstance[] instances) {
		for (int i = 0; i < instances.length; i++) {
			if (insName.equals(instances[i].name))
				return instances[i].type;
		}
		return "";
	}
	
	/**
	 * Checks that the connected source and destination data ports are of compatible types.
	 * @return TRUE if data ports are compatible; FALSE otherwise
	 */
	private boolean isCompatibleDataType(String[] arrays) {
		if ( source.getSigType().equalsIgnoreCase("ANY")		|| 
			 destination.getSigType().equalsIgnoreCase("ANY") ) {
			return true;
		}
		else if ( source.getSigType().equals(destination.getSigType()) ) {
			String srcArray = source.getArraySize();
			String dstArray = destination.getArraySize();
			if ( !srcArray.equals(dstArray) ) {
				arrays[0] = srcArray.isEmpty() ? "" : " with array size " + srcArray;  
				arrays[1] = dstArray.isEmpty() ? "" : " with array size " + dstArray;
				return false;
			}
			return true;
		}
		return false;
	}
	
	private boolean isCoherentBinding(String parentType, String childType) {
		if (parentType.equalsIgnoreCase("ANY")) {
			if ( !childType.equalsIgnoreCase("ANY") )
				return false;
		}
		return true;
	}
	
	public String toString() {
		return "Src: " + source.toString() + "  Dst: " + destination.toString();
	}
}
