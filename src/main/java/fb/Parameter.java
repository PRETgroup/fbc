/* Parameter.java
 * Describes a function block parameter.
 * 
 * Written by: Li Hsien Yoong
 */

package fb;

import java.io.BufferedWriter;

public class Parameter {
	public String name;
	public String value;
	
	public Parameter(String n, String v) {
		name = n;
		value = v;
	}
	
	public String toString() {
		return name + " = " + value;
	}
	
	public boolean equals(Object o) {
		if (o instanceof Parameter) {
			Parameter p = (Parameter)o;
			return (name.equals(p.name) && value.equals(p.value));
		}
		return false;
	}

	public void toXML(BufferedWriter printer, String indent) {
		// TODO: PARAMETER.toXML
		System.err.println("TODO: PARAMETER.toXML()");
		
	}
}