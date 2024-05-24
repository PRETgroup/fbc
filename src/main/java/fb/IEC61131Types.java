/**
 * IEC61131Types.java
 * Lists the various IEC 61131 data types.
 * 
 * Written by: Li Hsien Yoong
 */

package fb;

import ir.CodeGenerator;

import java.util.*;
import fbtostrl.Options;

public class IEC61131Types {
	private static HashMap<String, Integer> primitives = new HashMap<String, Integer>();	
	private static HashMap<String, VarDeclaration[]> structured = new HashMap<String, VarDeclaration[]>();
	
	static {
		primitives.put("ANY", 8);	// there is no rational reason for making the size of ANY 8!
		primitives.put("BOOL", 1);
		primitives.put("SINT", 1);
		primitives.put("INT", 2);
		primitives.put("DINT", 4);
		primitives.put("LINT", 8);
		primitives.put("USINT", 1);
		primitives.put("UINT", 2);
		primitives.put("UDINT", 4);
		primitives.put("ULINT", 8);
		primitives.put("REAL", 4);
		primitives.put("LREAL", 8);
		primitives.put("STRING", Options.STRLEN);
		primitives.put("WSTRING", Options.STRLEN);
		primitives.put("BYTE", 1);
		primitives.put("WORD", 2);
		primitives.put("DWORD", 4);
		primitives.put("LWORD", 8);
		primitives.put("TIME", 8);
		/* For now, the following types will be treated as user-defined types:
		 * "DATE", "TIME_OF_DAY", "TOD", "DATE_AND_TIME", "DT"
		 */
	}
	
	public static final void setSTRINGlen(int len) {
		primitives.put("STRING", len);
		primitives.put("WSTRING", len);
	}
	
	public static final boolean isIEC61131Type(String type) {
		Set<String> types = primitives.keySet();
		for (String t : types) {
			if (t.equalsIgnoreCase(type))
				return true;
		}
		return false;
	}
	
	public static final void addStructuredType(String name, VarDeclaration[] vars) {
		structured.put(name, vars);
	}
	
	public static final VarDeclaration[] getStructuredType(String type) {
		Set<String> types = structured.keySet();
		for (String t : types) {
			if (t.equalsIgnoreCase(type))
				return structured.get(t);
		}
		return new VarDeclaration[0];
	}
	
	public final static boolean processedStructuredType(String type) {
		Set<String> types = structured.keySet();
		for (String t : types) {
			if (t.equalsIgnoreCase(type))
				return true;
		}
		return false;
	}
	
	/**
	 * Computes the size of a particular data type, like the sizeof operator in C.
	 * @param type data type
	 * @param arraySize size of the array (1 if data type is not an array)
	 * @return Size of a particular data type
	 */
	public static int getSizeof(String type, int arraySize) {
		Set<String> types = primitives.keySet();
		for (String t : types) {
			if (t.equalsIgnoreCase(type))
				return primitives.get(t) * arraySize;
		}
		
		int sizeof = 0;
		types = structured.keySet();
		for (String t : types) {
			if (t.equalsIgnoreCase(type)) {
				VarDeclaration[] vars =  structured.get(t);
				for (VarDeclaration var : vars) {
					int array = CodeGenerator.validateArraySize(var.getArraySize());
					if (array <= 0)
						array = 1;
					sizeof += getSizeof(var.getType(), array);
				}
			}
		}
		return sizeof;
	}
}
