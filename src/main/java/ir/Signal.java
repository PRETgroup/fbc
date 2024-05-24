/* Signal.java
 * Helper class to describe a signal.
 *
 * Written by: Li Hsien Yoong
 */

package ir;

public class Signal extends PortName {
	private PortType portType;

	public Signal(PortName p, PortType pt) {
		super(p.instance, p.insType, p.signal, p.sigType, p.arraySize);
		portType = pt;
	}
	
	public Signal(String i, String it, String s, String t, String a, PortType pt) {
		super(i, it, s, t, a);
		portType = pt;
	}
	
	public Signal(String s, String t, String a, PortType pt) {
		super("", "", s, t, a);
		portType = pt;
	}
	
	public Signal(Signal s) {
		super(s.instance, s.insType, s.signal, s.sigType, s.arraySize);
		portType = s.portType;
	}
	
	public PortType getPortType() {
		return portType;
	}
}
