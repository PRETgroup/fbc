/* HCECC.java
 * Defines the HCECC object.
 * 
 * Written by: Gareth Shaw
 */

package fb;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.LinkedList;

import fb.Algorithm;
import fbtostrl.OutputLevel;
import fbtostrl.OutputManager;

public class HCECC extends ECC {
	public ECC[] eccs;

	public HCECC(ECC[] e) {
		super(e[0].name,e[0].states,e[0].transitions);
		if( name == null )
		{
			OutputManager.printError(name, "HCECC without a name", OutputLevel.FATAL);
			System.exit(-1);
		}
		eccs = e;
		// Get parallel starting states:
		ECState[] parallelStates = new ECState[eccs.length - 1];
		for(int pS = 1; pS < eccs.length; pS++) // Doesn't include 1st ECC
		{
			parallelStates[pS-1] = eccs[pS].root;
		}
		
		// Set the Array of parallel states
		root.parallelStates = parallelStates;
		
	}
	
	
	
	
	
	
	public HCECC(ECC ecc) {
		super(ecc.name,ecc.states,ecc.transitions);
		eccs = new ECC[1];;
		eccs[0] = ecc;
	}






	// Functions to Override
	public void mapAlgo(Algorithm[] algs) {
		for (ECC e : eccs)
			e.mapAlgo(algs);
	}
	
	public void toXML(BufferedWriter printer, String indent) throws IOException {
		printer.append(indent+"<HCECC>\n");
		for (ECC e : eccs) 
			e.toXML(printer, indent+"\t");
		printer.append(indent+"</HCECC>\n");
	}






	public void getECCNames(LinkedList<String> eccNames) {
		for (ECC e : eccs) // Parallel ECCs
			e.getECCNames(eccNames);
	}

	
}
