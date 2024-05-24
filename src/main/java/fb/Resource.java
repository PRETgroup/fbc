/* Resource.java
 * Defines a Resource
 * 
 * Written by: Li Hsien Yoong
 */

package fb;

public class Resource {
	public String name;
	public FBType resType;
	public Parameter[] params;

	public Resource(String n, FBType r, Parameter[] p) {
		name = n;
		resType = r;
		params = p;
	}
	
	public String toString() {
		StringBuilder str = new StringBuilder(name + " : " + resType.name + "\n");
		for (Parameter p : params)
			str.append("  Param: " + p.name + " = " + p.value + "\n");
		return str.toString();
	}
}