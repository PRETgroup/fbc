/* SignalInfo.java
 * Helper class to store signal information.
 *
 * Written by: Li Hsien Yoong
 */

package esterel;

import ir.*;

public class SignalInfo extends Signal {
	private String init;
	
	SignalInfo(String s, PortType bt, String st, String a) {
		super(s, st, a, bt);
	}
	
	SignalInfo(String s, PortType bt, String st, String a, String i) {
		super(s, st, a, bt);
		init = i;
	}
	
	SignalInfo(Signal sig) {
		super(sig);
	}
	
	String getInit() {
		return init;
	}
	
	public String toString() {
		return getFullName() + " : " + sigType + " := " + init + 
		       " [" + Integer.toHexString(hashCode()) + "]";
	}
}
