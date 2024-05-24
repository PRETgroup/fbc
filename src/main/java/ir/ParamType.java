/* ParamType.java
 * Describes the type of parameter used in a function
 * 
 * Written by: Li Hsien Yoong
 */

package ir;

public class ParamType {
	String type;
	boolean isWrite;
	
	ParamType(String t, boolean w) {
		type = t;
		isWrite = w;
	}
	
	public String getType() {
		return type;
	}
	
	public boolean isWrite() {
		return isWrite;
	}
}
