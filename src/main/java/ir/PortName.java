/* PortName.java
 * Helper class to store the parsed name of a port connection
 *
 * Written by: Li Hsien Yoong
 */

package ir;

public class PortName {
	public String instance;
	protected String insType;
	protected String signal;
	protected String sigType;
	protected String arraySize;
	
	public PortName(String i, String it, String s, String t, String a) {
		instance = i;
		insType = it;
		signal = s;
		sigType = t;
		
		if (a != null)
			arraySize = a.trim();
		else
			arraySize = "";
	}
	
	public PortName(String i, String it, String s) {
		instance = i;
		insType = it;
		signal = s;
		arraySize = "";
	}
	
	public int hashCode() {
		return instance.hashCode() + signal.hashCode();
	}
	
	public boolean equals(Object o) {
		if (o instanceof PortName) {
			PortName p = (PortName)o;
			return (instance.equals(p.instance) && signal.equals(p.signal));
		}
		return false;
	}
	
	public String getFullName() {
		if (instance.isEmpty())
			return signal;
		return (instance + "." + signal);
	}
	
	final public String getInstance() {
		return instance;
	}
	
	final public String getInsType() {
		return insType;
	}
	
	final public String getSignal() {
		return signal;
	}
	
	final public String getSigType() {
		return sigType;
	}
	
	final public String getArraySize() {
		return arraySize;
	}
	
	final public void setSigType(String type) {
		sigType = type;
	}
	
	final public void setArraySize(String array) {
		arraySize = array.trim();
	}
	
	public String toString() {
		StringBuilder name = new StringBuilder(signal);
		if (!instance.isEmpty())
			name.insert(0, instance + ".");
		if (sigType != null)
			name.append(":" + sigType);
		if (!arraySize.isEmpty())
			name.append("[" + arraySize + "]");
		return name.toString();
	}
}
