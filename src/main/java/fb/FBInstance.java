/* FBInstance.java
 * Helper class to describe a function block instance.
 * 
 * Written by: Li Hsien Yoong
 */

package fb;

import java.io.BufferedWriter;
import java.io.IOException;

public class FBInstance {
	public String name;
	public String type;
	public Parameter[] params;
	
	public FBInstance(String n, String t, Parameter[] p) {
		name = n;
		type = t;
		params = p;
	}
	
	public String getName() {
		return name;
	}
	
	public int hashCode() {
		int code = name.hashCode() + type.hashCode();
		if (params != null) {
			for (int i = 0; i < params.length; i++)
				code += params.hashCode();
		}
		return code;
	}
	
	public boolean equals(Object o) {
		if (o instanceof FBInstance) {
			FBInstance i = (FBInstance)o;
			if (params == null && i.params == null)
				return (name.equals(i.name) && type.equals(i.type));
			else if (params == null || i.params == null)
				return false;
			else if (params.length != i.params.length)
				return false;
			else if (!name.equals(i.name) || !type.equals(i.type))
				return false;
			else {
				for (int j = 0; j < params.length; j++) {
					if (!params[j].equals(i.params[j]))
						return false;
				}
				return true;
			}
		}
		return false;
	}
	
	public String getParamValue(String paramName) {
		if (params == null)
			return null;
		for (Parameter param : params) {
			if (param.name.equals(paramName))
				return param.value;
		}
		return null;
	}
	
	public String toString() {
		StringBuilder s = new StringBuilder();
		if (params != null) {
			for (int i = 0; i < params.length; i++) {
				s.append("\"" + params[i].toString() + "\" ");
			}
		}
			
		return name + " : " + type + " (" + s.toString() + ")";
	}

	public void toXML(BufferedWriter printer, String indent) throws IOException {
		printer.append(indent+"<FB Name=\""+name+"\" Type=\""+type+"\" x=\"50\" y=\"50\" ");
		if( params != null )
		{
			printer.append(">\n");
			for( Parameter p : params )
			{
				p.toXML(printer, indent+"\t");
			}
			printer.append(indent+"</FB>\n");
		}
		else
			printer.append("/>\n");
		
	}
}