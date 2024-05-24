/* Options.java
 * Convenience class for passing along user options to the compiler.
 *
 * Written by: Li Hsien Yoong
 */

package fbtostrl;

import fb.IEC61131Types;

public class Options {
	public final static int STRLEN = 32;
	public boolean altSim;
	public boolean strl;
	public boolean sysj;
	public boolean ccode;
	public boolean pretc;
	public boolean l5x;
	public boolean cpp;
	public boolean runnable;
	public boolean simul;
	private static String simulIPaddr;
	public boolean qtsimul;
	public boolean rmc;
	public boolean sorted;
	//public boolean acyc;
	public boolean nxtControl;
	public boolean gnu = true;
	public boolean gnuUCOS;
	public boolean ms;
	public boolean ttpos;
	public boolean d3;
	public boolean iotapps;
	public String deploymentServer;
	public String deploymentUsername;
	public String deploymentKey;
	public boolean testBench;
	public boolean noMake;

	private OutputPath outputPath = new OutputPath();
	private static int strlen = STRLEN;
	public boolean verbose;
	public boolean machineInterface;
	public boolean timeAnnotate;
	public int sleepValue;
	
	public boolean standardXMLOnly = true;

	
	public Options() {}
	
	final public boolean isStrl() {
		return strl;
	}
	
	final public boolean isSysJ() {
		return sysj;
	}
	
	final public boolean isCCode() {
		return ccode;
	}
	
	final public boolean isCpp() {
		return cpp;
	}
	
	final public boolean isRunnable() {
		return runnable;
	}
	
	final public boolean isSimul() {
		return simul;
	}
	
	final public static void setSimulIPaddr(String ipaddr) {
		if (ipaddr == null)
			simulIPaddr = "127.0.0.1";
		else
			simulIPaddr = ipaddr;
	}
	
	final public static String getSimulIPaddr() {
		return simulIPaddr;
	}
	
	final public boolean isRmc() {
		return rmc;
	}
	
	final public boolean isSorted() {
		return sorted;
	}
	
	/*final public boolean isAcyclic() {
		return acyc;
	}*/
	
	final public static int strlen() {
		return strlen;
	}
	
	final public static void setStrlen(int len) {
		strlen = len;
		IEC61131Types.setSTRINGlen(len);
	}
	
	final public boolean isNxtControl() {
		return nxtControl;
	}
	
	final public boolean isGNU() {
		return gnu;
	}
	
	final public boolean isGNUuCOS() {
		return gnuUCOS;
	}
	
	final public boolean isMS() {
		return ms;
	}
	
	final public boolean isTTPOS() {
		return ttpos;
	}
	
	final public boolean isD3() {
		return d3;
	}
	
	final public void selectPlatform(String target) {
		if (target.equals("gnu")) {
			gnu = true;
			gnuUCOS = false;
			ms = false;
			ttpos = false;
			d3 = false;
			iotapps = false;
		}
		else if (target.equals("gnu-ucos")) {
			gnu = false;
			gnuUCOS = true;
			ms = false;
			ttpos = false;
			d3 = false;
			iotapps = false;
		}
		else if (target.equals("ms")) {
			gnu = false;
			gnuUCOS = false;
			ms = true;
			ttpos = false;
			d3 = false;
			iotapps = false;
		}
		else if (target.equals("ttpos")) {
			gnu = false;
			gnuUCOS = false;
			ms = false;
			ttpos = true;
			d3 = false;
			iotapps = false;
			System.out.println("tttttttt");
		}
		else if (target.equals("d3")) {
			gnu = false;
			gnuUCOS = false;
			ms = false;
			ttpos = false;
			d3 = true;
			iotapps = false;
		}else if (target.equals("iotapps")) {
			gnu = false;
			gnuUCOS = false;
			ms = false;
			ttpos = false;
			d3 = false;
			iotapps = true;
		}
	}
	
	final public String outputpath() {
		return outputPath.getPath(); 
	}
	
	final public String sysOutputPath() {
		return outputPath.getPathOnly();
	}
	
	final public void setOutputPath(String path) {
		outputPath.setPath(path);
	}
	
	final OutputPath getOutputPath() {
		return outputPath.clone();
	}
	
	final public boolean isVerbose() {
		return verbose;
	}

	final public boolean isTestBench() {
		return testBench;
	}
	
	final public boolean isNoMake() {
		return noMake;
	}

	public boolean isQTSimul() {
		return qtsimul;
	}

	final public int getSleepValue() {
		return sleepValue;
	}

	public boolean isL5X() {
		return l5x;
	}
}
