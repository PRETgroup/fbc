/* GlobalVarType.java
 * Enumeration of the possible types of global variables that can be generated
 * 
 * Written by: Li Hsien Yoong
 */

package ir;

public enum GlobalVarType {
	PLAIN,	// for generating unwrapped global variables (e.g., D3) 
	UCOS, 	// for generating global variables accessed via methods (e.g., uC/OS)
	PUBSUB	// for generating global variables accessed via pointers (e.g., PUBL/SUBL)	
}
