/* Prototype.java
 * Helper class for specifying function prototypes.
 * 
 * Written by: Li Hsien Yoong
 */

package ir;

import fb.VarDeclaration;

public class Prototype {
	private String type;
	private String name;
	private VarDeclaration[] arguments;
	
	Prototype(String t, String n, VarDeclaration[] a) {
		type = t;
		name = n;
		arguments = a;
	}
	
	final public String getType() {
		return type;
	}
	
	final public String getName() {
		return name;
	}
	
	final public VarDeclaration[] getArguments() {
		return arguments;
	}
	
	final public VarDeclaration getArgument(int index) {
		if (index >= 0 && index < arguments.length)
			return arguments[index];
		return null;
	}
}
