/* FB.java
 * Function block interface class.
 *
 * Written by: Li Hsien Yoong
 */

package fb;

import java.io.*;
import java.util.*;

import fbtostrl.FBtoStrl;
import ir.Block;
import ir.CodeGenerator;
import ir.VarType;
import ir.DataType;

public class FBType {
	public String name;
	private String subType;
	private String targetType;	// concrete type to be generated in the host language 
	private String comment;
	private String[] headers;
	private InterfaceList iface;
	private BasicFB basic;
	private PretC pretc;
	public FBNetwork composite;
	public FBType[] subBlocks;
	public Parameter[] params;	// parameters from the parent network
	private LinkedList<CodeGenerator> deferredBlocks = new LinkedList<CodeGenerator>();
	protected boolean sifb;
	
	// These fields contain SIFB-related information set within FunctionBlock.
	// This is a quick hack, which spoils the hierarchy of objects which should  
	// typically be refined as objects get converted from the fb to ir packages.
	// This hack reverses the order and uses objects from the ir package to  
	// refine an object in the fb package instead. 
	private String sifbName;
	private int sifbIndex;
	private int sifbQlen;
	
	public FBType(String n, String c, String[] h, InterfaceList i, BasicFB b) {
		name = n;
		targetType = n;
		comment = c;
		headers = h;
		iface = i;
		basic = b;
		if (b == null)  
			sifb = true;	// treat empty BasicFB as a service interface function block
	}
	
	public FBType(String n, String c, String[] h, InterfaceList i, PretC pc) {
		name = n;
		targetType = n;
		comment = c;
		headers = h;
		iface = i;
		pretc = pc;
	}
	
	public FBType(String n, String c, String[] h, InterfaceList i, FBNetwork comp,
			      ArrayList<FBType> sub) {
		name = n;
		targetType = n;
		comment = c;
		headers = h;
		iface = i;
		composite = comp;
		int len = sub.size();
		subBlocks = new FBType[len];
		for (int j = 0; j < len; j++)
			subBlocks[j] = sub.get(j);
	}
	
	public FBType(String n, String c, String[] h, InterfaceList i) {
		name = n;
		targetType = n;
		comment = c;
		headers = h;
		iface = i;
		sifb = true;
	}
	
	public FBType(FBType archetype) {
		name = archetype.name;
		targetType = archetype.targetType;
		comment = archetype.comment;
		headers = archetype.headers;
		iface = archetype.iface;
		basic = archetype.basic;
		pretc = archetype.pretc;
		sifb = archetype.sifb;
		subType = archetype.subType;
	}

	public String getName() {
		return name;
	}
	
	public void setSubType(String s) {
		subType = s;
	}
	
	public String getSubType() {
		return subType;
	}
	
	public void setTargetType(String t) {
		targetType = t;
	}
	
	public String getTargetType() {
		return targetType;
	}

	public String getComment() {
		return comment;
	}
	
	public String[] getHeaders() {
		return headers;
	}

	public InterfaceList getInterfaceList() {
		return iface;
	}

	public BasicFB getBasicFB() {
		return basic;
	}

	public FBNetwork getFBNetwork() {
		return composite;
	}

	public FBType[] getSubBlocks() {
		return subBlocks;
	}
	
	public boolean isSIFB() {
		return sifb;
	}
	
	public boolean isPretC() {
		return pretc != null;
	}
	
	public PretC getPretC() {
		return pretc;
	}
	
	public void addDeferredBlock(CodeGenerator cg) {
		deferredBlocks.add(cg);
	}
	
	public void addDeferredBlock(String ancestor, LinkedList<CodeGenerator> cgs) {
		if ( !cgs.isEmpty() ) {
			CodeGenerator cg = cgs.removeLast();
			int index = cg.getFQInsName().lastIndexOf('.');
			if (index >= 0) {
				if (ancestor.equals(cg.getFQInsName().substring(0, index+1)))
					deferredBlocks.add(cg);
			}
			else {
				// There are no ancestors: this means that the deferred block belongs to root.
				// So, ensure that we are really at root!
				if (ancestor.isEmpty())
					deferredBlocks.add(cg);
			}
		}
	}
	
	public LinkedList<CodeGenerator> getDeferredBlocks() {
		return deferredBlocks;
	}
	
	/**
	 * This method is called in the event that the top-most function block code generator
	 * gets deferred. If that function block is a service interface function block with
	 * ANY ports, those ports will remain unresolved. This method will ensure that 
	 * information about the unresolved ANY ports gets stored in the ANY resolution map. 
	 * This enables the default conversion of unbound ANY ports to take place.
	 * @param top TRUE if this is the top-most artefact given to the compiler; FALSE otherwise
	 * @param comBlocks set of local communication blocks (if any)
	 * @throws IOException
	 */
	public void runLastDeferredCG(boolean top, HashSet<Block> comBlocks) throws IOException {
		if ( !deferredBlocks.isEmpty() ) {
			CodeGenerator cg = deferredBlocks.removeLast();
			if (cg.sifbInfo != null) {
				if (cg.inputData != null) {
					for (VarType var : cg.inputData) {
						if (var.getType().equalsIgnoreCase("ANY"))
							cg.sifbInfo.defineAnyPort("", var.getName(), new DataType("ANY", ""));
					}
				}
				if (cg.outputData != null) {
					for (VarType var : cg.outputData) {
						if (var.getType().equalsIgnoreCase("ANY"))
							cg.sifbInfo.defineAnyPort("", var.getName(), new DataType("ANY", ""));
					}
				}
			}
			cg.generateCode(cg.getFileName(), top, comBlocks);
		}
	}
	
	public void setSIFBname(String n) {
		sifbName = n;
	}
	
	public String getSIFBname() {
		return sifbName;
	}
	
	public void setSIFBindex(int i) {
		sifbIndex = i;
	}
	
	public int getSIFBindex() {
		return sifbIndex;
	}
	
	public int getSIFBQlen() {
		return sifbQlen;
	}
	
	public void setSIFBQlen(int i) {
		sifbQlen = i;
	}
	
	public void print() {
		System.out.println("FBType::");
		if (name != null)
			System.out.println("  Name: " + name);
		if (comment != null)
			System.out.println("  Comment: " + comment);
		System.out.println("  InterfaceList::");
		iface.print(2);
		if (basic != null) {
			System.out.println("  BasicFB::");
			basic.print(2);
		}
	}

	public void printECC() {
		if (basic != null)
			basic.printECC();
	}
	
	public void toXML(BufferedWriter out) throws IOException {
		// Build XML Tree
		//StringBuilder printer = new StringBuilder();
		
		out.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		out.append("<!DOCTYPE FBType SYSTEM \"HCECC.dtd\">\n");
		out.append("<FBType Comment=\"Basic HCECC Function Block Type\" Name=\""+this.name+"\">\n");
		out.append("	<Identification Standard=\"61499-2\"/>\n");
		out.append("	<VersionInfo Author=\"GDS\" Date=\"2011-11-18\" Organization=\"University of Auckland\" Version=\"0.0\"/>\n");
		out.append("	<CompilerInfo header=\"");
		if( headers != null )
		{
			for( String header : headers )
			{
				out.append(FBtoStrl.XmlEncode(header) + ";");
			}
		}
		out.append("\"/>\n");
		iface.toXML(out, "\t");
		if( basic != null)
			basic.toXML(out,"\t");
		else if( composite != null )
		{
			composite.toXML(out, "\t");
		}
		else
		{
			// TODO: The other versions
		}
		
		out.append("</FBType>");
	}
	
	/**
	 * Return a copy of this FBType
	 */
	public FBType clone() {
		return new FBType(this);
	}

	/**
	 * Unique FB type is needed if I/O ports are of ANY type OR contains "@CNT".
	 * @return TRUE if FB type needs to be unique; FALSE otherwise
	 */
	public boolean needsUnique() { 

		if (iface == null)
			return false;
		
		// Check variables
		if (iface.inputVars != null) {
			for (VarDeclaration v : iface.inputVars) {
				if ( v.type.equalsIgnoreCase("ANY") )
					return true;
				if ( v.name.contains("@CNT") || v.arraySize.contains("@CNT") )
					return true;
				if ( v.name.contains("${") || v.arraySize.contains("${") )
					return true;
			}
		}
		if (iface.outputVars != null) {
			for (VarDeclaration v : iface.outputVars) {
				if ( v.type.equalsIgnoreCase("ANY") )
					return true;
				if ( v.name.contains("@CNT") || v.arraySize.contains("@CNT") )
					return true;
				if ( v.name.contains("${") || v.arraySize.contains("${") )
					return true;
			}
		}
		
		// Check events
		if (iface.eventInputs != null) {
			for (Event e : iface.eventInputs) {
				if ( e.name.contains("@CNT") )
					return true;
				if ( e.name.contains("${") )
					return true;
			}
		}
		if (iface.eventOutputs != null) {
			for (Event e : iface.eventOutputs) {
				if ( e.name.contains("@CNT") )
					return true;
				if ( e.name.contains("${") )
					return true;
			}
		}
		return false;
	}
	
	/**
	 * Checks if this particular FBType requires global variables to be generated for it. 
	 * This method of handling special cases that are not covered by needsUnique() is a 
	 * real annoyance and should be done differently the moment someone figures out how!
	 * @return The global variable type if this function block type requires a unique 
	 * global variable to be generated for it, but has not been covered by the 
	 * needsUnique() method; otherwise, null. 
	 */
	public static String getGlobalVarType(String fbtype) {
		if (fbtype.equals("PUBL_0") || fbtype.equals("SUBL_0"))
			return "PUBLSUBL_0";
		return null;
	}
	
	/** 
	 * @return String representation of object, useful for debugging
	 * 
	 */
	public String toString()
	{
		StringBuilder str = new StringBuilder(name);
		if (params != null) {
			str.append(": <Parameters> ");
			for (int i = 0; i < params.length; i++)
				str.append(params[i].toString() + ", ");
		}
		return str.toString();
	}
}
