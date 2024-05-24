/**
 * The class contains properties and methods for dealing with TTP data types.
 */
package d3;

/**
 * @author lihsien
 *
 */

import java.util.*;
import ccode.CGenerator;
import ir.CodeGenerator;
import fbtostrl.OutputLevel;
import fbtostrl.OutputManager;

public class TTPDataTypes {
	private static final String[] primitives = {
		"float4", "sbyte1", "sbyte2", "sbyte4", "ubyte1", "ubyte2", "ubyte4" 
	};
	private static final HashMap<String, MsgProperties> extendedPrimitives = new HashMap<String, MsgProperties>();
	private static HashSet<String> structTypes = new HashSet<String>();
	
	static {
		extendedPrimitives.put("bool", new MsgProperties(":1", "INT", "char", "1"));
		extendedPrimitives.put("sbyte8", new MsgProperties("8", "INT", "long long", "8"));
		extendedPrimitives.put("ubyte8", new MsgProperties("8", "UINT", "unsigned long long", "8"));
		extendedPrimitives.put("float8", new MsgProperties("8", "REAL", "double", "8"));
	}
	
	public static boolean isPrimitive(String type) {
		for (String primitive : primitives) {
			if (primitive.equals(type))
				return true;
		}
		
		// Character arrays are primitives as well, since strings get map to them
		if (type.matches("sbyte1_\\d+"))
			return true;
		
		return false;
	}
	
	public static boolean isExtendedPrimitive(String type) {
		Set<String> extensions = extendedPrimitives.keySet();
		for (String primitive : extensions) {
			if (primitive.equals(type))
				return true;
		}
		return false;
	}
	
	public static boolean isStructType(String type) {
		return structTypes.contains(type);
	}
	
	public void addStructType(String type) {
		structTypes.add(type);
	}
	
	public static MsgProperties getMsgProperties(String type) {
		return extendedPrimitives.get(type);
	}
	
	public static String getTTPDataType(String type, int strlen) {
		if (type.equalsIgnoreCase("BOOL"))
			return "bool";
		else if (type.equalsIgnoreCase("SINT"))
			return "sbyte1";
		else if (type.equalsIgnoreCase("INT"))
			return "sbyte2";
		else if (type.equalsIgnoreCase("DINT"))
			return "sbyte4";
		else if (type.equalsIgnoreCase("LINT"))
			return "sbyte8";
		else if (type.equalsIgnoreCase("USINT"))
			return "ubyte1";
		else if (type.equalsIgnoreCase("UINT"))
			return "ubyte2";
		else if (type.equalsIgnoreCase("UDINT"))
			return "ubyte4";
		else if (type.equalsIgnoreCase("ULINT"))
			return "ubyte8";
		else if (type.equalsIgnoreCase("REAL"))
			return "float4";
		else if (type.equalsIgnoreCase("LREAL"))
			return "float8";
		else if (type.equalsIgnoreCase("STRING") || type.equalsIgnoreCase("WSTRING"))
			return "sbyte1_" + strlen;
		//else if (type.equalsIgnoreCase("WSTRING")) return "Wstring";
		else if (type.equalsIgnoreCase("BYTE"))
			return "ubyte1";
		else if (type.equalsIgnoreCase("WORD"))
			return "ubyte2";
		else if (type.equalsIgnoreCase("DWORD"))
			return "ubyte4";
		else if (type.equalsIgnoreCase("LWORD"))
			return "ubyte8";
		//else if (type.equalsIgnoreCase("DATE_AND_TIME")) return "struct timeval";
		else if (type.equalsIgnoreCase("TIME"))
			return "sbyte8";
		else
			return type;
	}
	
	public static String getArrayIndex(String arraySize, String blockID, String name) {
		if (arraySize.isEmpty())
			return "";
		int size = CodeGenerator.validateArraySize(arraySize);
		if (size <= 0) {
			OutputManager.printError(blockID, "Variable `" + name + 
					"' has an invalid array size.", OutputLevel.FATAL);
			System.exit(0);
		}
		return "_" + arraySize;
	}

	/**
	 * Creates an initializer for TTP messages.
	 * @param array true if the initializer is an array; false otherwise
	 * @param init the initializer string
	 * @param type data type of the initializer
	 * @param name name of the TTP message
	 * @return
	 */
	public static String getInitializer(boolean array, String init, String type, String name) {
		if (init == null)
			return "0";		// arbitrarily default the initial value to zero
		if (init.isEmpty())
			return "0";		// arbitrarily default the initial value to zero
		
		String initializer;
		if (array) {
			// I'm leaving this function alone for now because i just realize that it
			// may be altogether unnecessary!!!
		}
		else {
			initializer = CGenerator.parseScalarInitializer(init, type, name);
			if (type.equalsIgnoreCase("STRING") || type.equalsIgnoreCase("WSTRING"))
				initializer = stringToSbyte1Array(initializer);
			initializer = CodeGenerator.parseScalarParam(type, initializer);
		}
		return init;
	}
	
	/**
	 * Converts a string into a Python-style tuple of sbyte1's, which is null terminated.
	 * @param cString a C-style string
	 * @return A tuple of sbyte1's corresponding to the characters of the string.
	 */
	public static String stringToSbyte1Array(String cString) {
		StringBuilder xmlStr = new StringBuilder("(");
		boolean escape = false;
		
		// Skip the opening and closing quotes ("...") 
		int len = cString.length() - 1;
		for (int i = 1; i < len; i++) {
			int c = cString.charAt(i); 
			if (!escape) {
				if (c == '\\')
					escape = true;
				else
					xmlStr.append(c);
			}
			else {
				switch (c) {
					case '\'':
					case '"':
					case '\\':
						xmlStr.append(c);
						break;
					case 'n':
						c = '\n';
						xmlStr.append(c);
						break;
					case 'f':
						c = '\f';
						xmlStr.append(c);
						break;
					case 'r':
						c = '\r';
						xmlStr.append(c);
						break;
					case 't':
						c= '\t';
						xmlStr.append(c);
						break;
					default:
						// Skip everything else! This is not actually correct, but
						// ignore for now.
						break;
				}
				escape = false;
			}
			xmlStr.append(',');
		}
		xmlStr.append(0);	// this is the null terminator for the string
		xmlStr.append(')');
		
		return xmlStr.toString();
	}
}
