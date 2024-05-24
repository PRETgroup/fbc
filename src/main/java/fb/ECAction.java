/* ECAction.java
 * Defines the ECAction object.
 * 
 * Written by: Li Hsien Yoong
 */

package fb;

import java.io.BufferedWriter;
import java.io.IOException;

public class ECAction {
	private String algorithm;
	private String arguments;
	private String output;
	// Kyle: private boolean isExitAction;
	String code;
	
	public ECAction(String a, String o) {
		if (a != null) {
			int begin = a.indexOf('('); 
			int end = a.lastIndexOf(')');
			if (begin > -1 && end > -1) {
				algorithm = a.substring(0, begin).trim();
				arguments = a.substring(begin + 1, end).trim();
			}
			else {
				algorithm = a;
				arguments = null;
			}
		}
		output = o;	
	}
	
	/* Kyle:
	public ECAction(String a, String o, boolean e) {
		if (a != null) {
			int begin = a.indexOf('('); 
			int end = a.lastIndexOf(')');
			if (begin > -1 && end > -1) {
				algorithm = a.substring(0, begin).trim();
				arguments = a.substring(begin + 1, end).trim();
			}
			else {
				algorithm = a;
				arguments = null;
			}
		}
		output = o;	
		isExitAction = e;
	}
	*/
	
	void mapAlgo(Algorithm[] algs) {
		for (int i = 0; i < algs.length; i++) {
			if (algorithm != null) {
				if (algorithm.compareTo(algs[i].name) == 0) {
					code = algs[i].text;
					return;
				}
			}
		}
	}
	
	public String getAlgorithm() {
		return algorithm;		
	}
	
	public String getArguments() {
		return arguments;
	}
	
	public String getOutput() {
		return output;
	}
	
	/* Kyle:
	public boolean getIsExitAction() {
		return isExitAction;
	}
	*/
	
	public String getCode() {
		return code;
	}
	
	public void print(int indent) {
		if (algorithm != null) {
			Static.format(indent);
			System.out.println("Algorithm: " + algorithm);
		}
		if (output != null) {
			Static.format(indent);
			System.out.println("Output: " + output);
		}
	}
	
	public void printECC() {
		if (algorithm != null)
			System.out.print(algorithm);
		System.out.print(',');
		if (output != null)
			System.out.print(output + " ");
	}
	
	public void toXML(BufferedWriter printer, String indent) throws IOException {
		printer.append(indent+"<ECAction ");
		if( algorithm != null )	
		{
			printer.append("Algorithm=\""+algorithm.trim());
			if( arguments != null )
				printer.append("("+arguments.trim()+")");
			printer.append("\" ");
		}
		if( output != null )
		{
			printer.append("Output=\""+output.trim()+"\" ");
		}
		printer.append("/>\n");
		
	}
	
	public String toString()
	{
		return "ECAction: "+algorithm!=null?algorithm:""+output!=null?output:"";
		
	}
	
	public boolean equals(ECAction a)
	{
		if( algorithm == null && a.algorithm != null) return false; 
		if( algorithm != null && !algorithm.equals(a.algorithm) ) return false;
		if( output == null && a.output != null) return false; 
		if( !output.equals(a.output) ) return false;
		
		return true;
	}
}