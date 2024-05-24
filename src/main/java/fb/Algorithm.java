/* Algorithm.java
 * Defines the Algorithm object.
 * 
 * Written by: Li Hsien Yoong
 */

package fb;

import java.io.BufferedWriter;
import java.io.IOException;

import fbtostrl.FBtoStrl;

public class Algorithm {
	protected String comment;
	protected String name;
	protected String language;
	protected String prototype;
	protected String text;
	
	public Algorithm(String c, String n, String l, String p, String t) {
		comment = c;
		name = n;
		language = l;
		prototype = p;
		text = t;
	}
	
	public String getComment() {
		return comment;
	}
	
	public String getName() {
		return name;
	}
	
	public String getLanguage() {
		return language;
	}
	
	public String getPrototype() {
		return prototype;
	}
	
	public String getText() {
		return text;
	}
	
	void print(int indent) {
		Static.format(indent);
		System.out.println("Name: " + name);
		Static.format(indent);
		System.out.println("Text: " + text);
	}
	
	public void toXML(BufferedWriter printer, String indent) throws IOException {
		printer.append(indent+"<Algorithm Name=\""+name+"\">\n");
		printer.append(indent+"\t"+"<Other Language=\""+language+"\"");
		if( !FBtoStrl.opts.standardXMLOnly && prototype != null && prototype.length() > 0 )
			printer.append(" Prototype=\""+FBtoStrl.XmlEncode(prototype)+"\"");
		
		printer.append(" Text=\""+FBtoStrl.XmlEncode(text)+"\" />\n");
		
		printer.append(indent+"</Algorithm>\n");
		
	}
}