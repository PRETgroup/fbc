/* VarDeclaration.java
 * Defines the VarDeclaration object (I/O variable in function blocks).
 * 
 * Written by: Li Hsien Yoong
 */

package fb;

import java.io.*;

public class VarDeclaration {
	protected String name;
	protected String type;
	protected String arraySize;
	protected String initial;
	protected String comment;
	
	public VarDeclaration(String n, String t, String a, String i, String c) {
		name = n;
		type = t;
		if (a != null)
			arraySize = a.trim();
		else
			arraySize = "";
		if (i != null)
			initial = i.trim();
		comment = c;
	}
	
	public VarDeclaration(VarDeclaration v) {
		name = v.name;
		type = v.type;
		arraySize = v.arraySize;
		initial = v.initial;
		comment = v.comment;
	}
	
	public VarDeclaration(String n, String t, String a) {
		name = n;
		type = t;
		if (a != null)
			arraySize = a.trim();
		else
			arraySize = "";
	}
	
	//public void changeName(String newName) {
	//	name = newName;
	//}
	
	public String getName() {
		return name;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String t) {
		type = t;
	}
	
	final public String getArraySize() {
		return arraySize;
	}
	
	final public void setArraySize(String a) {
		arraySize = a.trim();
	}
	
	public int getArrayLength() {
		if( arraySize.equals("") )
			return 1;
		else
			return Integer.valueOf(arraySize).intValue();
	}
	
	public String getInitial() {
		return initial;
	}
	
	public final void setInitial(String init) {
		initial = init;
	}
	
	public String getComment() {
		return comment;
	}
	
	public void print(int indent) {
		Static.format(indent);
		System.out.println("Name: " + name);
		Static.format(indent);
		System.out.println("Type: " + type);
		if (initial != null) {
			Static.format(indent);
			System.out.println("InitialValue: " + initial);
		}
	}
	
	public void toXML(BufferedWriter printer, String indent) throws IOException {
		printer.write(indent+"<VarDeclaration ");
		//if( comment != null )
		//	printer.append("Comment=\""+comment+"\" ");
		printer.write("Name=\""+name+"\" ");
		if( initial != null )
		{
			printer.write("InitialValue=\""+initial+"\" ");
		}
		printer.write("Type=\""+type+"\" ");
		if( arraySize != null && arraySize.length() >= 1 )
			printer.write("ArraySize=\""+arraySize+"\" ");
		printer.write("/>\n");
		
	}
	
	public String toString()
	{
		return name + " (" + type + ")"; 
	}
}