/* BasicFB.java
 * Defines the basic function block.
 * 
 * Written by: Li Hsien Yoong
 */

package fb;

import java.io.BufferedWriter;
import java.io.IOException;

import fbtostrl.OutputLevel;
import fbtostrl.OutputManager;

public class BasicFB {
	public VarDeclaration[] internalVars;
	protected ECC ecc;
	

	public Algorithm[] algorithms;
	
	public BasicFB(VarDeclaration[] v, ECC e, Algorithm[] a) {
		internalVars = v;
		ecc = e;
		algorithms = a;
		
		if (algorithms != null && ecc != null)
			ecc.mapAlgo(algorithms);
	}
	
	public ECC getECC() {
		return ecc;
	}
	
	public ECState getECCRoot() {
		if( ecc == null ) 
		{
			OutputManager.printError("[BasicFB]", "ecc == null in BasicFB", OutputLevel.ERROR);
			return null;
		}
		return ecc.root;
	}
	
	public ECState[] getAllECStates() {
		return ecc.states;
	}
	

	
	public void print(int indent) {
		if (internalVars != null) {
			Static.format(indent);
			System.out.println("InternalVars::");
			for (int i = 0; i < internalVars.length; i++) {
				Static.format(indent + 1);
				System.out.println("VarDeclaration::");				
				internalVars[i].print(indent + 2);
			}
		}
		
		Static.format(indent);
		System.out.println("ECC::");
		ecc.print(indent + 1);
		
		if (algorithms != null) {
			for (int i = 0; i < algorithms.length; i++) {
				Static.format(indent);
				System.out.println("Algorithm::");
				algorithms[i].print(indent + 1);
			}
		}
			
	}
	
	public void printECC() {
		ecc.printECC();
	}
	
	public void toXML(BufferedWriter printer, String indent) throws IOException {
		printer.append(indent+"<BasicFB>\n");
		if( internalVars != null )
		{
			printer.append(indent+"\t<InternalVars>\n");
			for( VarDeclaration var : internalVars)
			{
				var.toXML(printer, indent+"\t\t");
			}
			printer.append(indent+"\t</InternalVars>\n");
		}
		ecc.toXML(printer, indent+"\t");
		if( algorithms != null )
		{
			for( Algorithm alg : algorithms )
			{
				alg.toXML(printer, indent+"\t");
			}
		}
		printer.append(indent+"</BasicFB>\n");
		
	}

	public ECTransition[] getAllECTransitions() {
		return ecc.transitions;
	}

	public Algorithm getAlgorithm(String algname) {
		for(Algorithm al : algorithms)
		{
			if( al.name.equalsIgnoreCase(algname))
				return al;
		}
		return null;
		
	}
}