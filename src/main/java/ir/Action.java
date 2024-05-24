/* Action.java
 * Defines Esterel's abstraction of the ECAction object.
 * 
 * Written by: Li Hsien Yoong
 */

package ir;

import fb.*;
import fbtostrl.OutputLevel;
import fbtostrl.OutputManager;

public class Action {
	private static String className;
	Procedure procedure;
	String arguments;
	Event output;
	// Kyle: boolean isExitAction;
	
	public Action(FunctionBlock m, ECAction a) {
		String algName = a.getAlgorithm();
		if (algName != null && m.procedures != null) {			
			for (int i = 0; i < m.procedures.length; i++) {
				if ( algName.equals(m.procedures[i].getName()) ) {
					procedure = m.procedures[i];
					break;
				}
			}
		}
		
		arguments = a.getArguments();
		// Kyle: isExitAction = a.getIsExitAction();
		
		String outputName = a.getOutput();
		if (outputName != null && outputName.length() > 0 ) { // outputName != ""
			for (int i = 0; i < m.outputEvents.length; i++) {
				if ( outputName.equals(m.outputEvents[i].getName()) ) {
					output = m.outputEvents[i];
					break;
				}
			}
			if (output == null) {
				OutputManager.printError(m.getCompiledType(), "Output `" + outputName + 
						"\' in ECC is not defined.", OutputLevel.FATAL);
				System.exit(0);
			}
		}
	}
	
	public Procedure getProcedure() {
		return procedure;
	}
	
	public String getArguments() {
		return arguments;
	}
	
	public Event getOutput() {
		return output;
	}
	
	/* Kyle:
	public boolean getIsExitAction() {
		return isExitAction;
	}
	*/
	
	static void setClassName(String n) {
		className = n;
	}
	
	static String getClassName() {
		if (className == null)
			return "";
		else
			return className;
	}
}
