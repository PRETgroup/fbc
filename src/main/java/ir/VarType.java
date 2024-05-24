/* VarType.java
 * Defines the variable type object.
 * 
 * Written by: Li Hsien Yoong
 */

package ir;

import fb.*;
import fbtostrl.OutputLevel;
import fbtostrl.OutputManager;

import java.util.*;

public class VarType extends VarDeclaration {
	private Category category;
	private String alias;
	
	public VarType(Category c, VarDeclaration var) {
		super(var);
		category = c;
		alias = name;
	}
	
	public VarType(Category c, VarDeclaration var, HashSet<String> types) {
		super(var);
		category = c;
		registerType(type, types);
		alias = name;
	}
	
	public VarType(Category c, String n, String t, String i) {
		super(n, t, null, i, null);
		category = c;
		registerType(type, null);
		alias = name;
	}
	
	public VarType(VarType v) {
		super(v);
		category = Category.INTERNAL;
		alias = v.alias;
	}
	
	public String getAlias() {
		return alias;
	}
	
	public void setAlias(String a) {
		alias = a;
	}
	
	// Returns true if the category of this variable matches that of "c", 
	// otherwise false
	public boolean isCategory(Category c) {
		return (category == c);
	}
	
	/* DESCRIPTION: Registers user-defined data types
	 * ARGUMENTS: type - data type
	 *            types - set of user-defined types identified
	 */
	private void registerType(String type, HashSet<String> types) {
		if (type.equals("BOOL")    || type.equals("SINT")  || type.equals("INT")    || 
			type.equals("DINT")    || type.equals("LINT")  || type.equals("USINT")  ||
			type.equals("UINT")	   || type.equals("UDINT") || type.equals("ULINT")  ||
			type.equals("REAL")    || type.equals("LREAL") || type.equals("STRING") ||
			type.equals("WSTRING") || type.equals("BYTE")  || type.equals("WORD")   ||
			type.equals("DWORD")   || type.equals("LWORD"))
			return;
			
		if (types != null) {
			if (!types.contains(type) && FunctionBlock.globalNames.contains(type)) {
				OutputManager.printError(name, "Multiple definitions of type `" +
						type + "\'.", OutputLevel.FATAL);
				System.exit(0);
			}
			else {
				FunctionBlock.globalNames.add(type);
				types.add(type);
			}
		}
	}
}
