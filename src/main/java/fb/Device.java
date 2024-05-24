/* DeviceType.java
 * Defines a DeviceType
 * 
 * Written by: Li Hsien Yoong
 */

package fb;

public class Device {
	public String name;
	public String type;
	public String comment;
	public Resource[] resources;
	public Parameter[] parameters;
	public static final String TYPE_PTARM = "PTARM";
	public Device(String n, String t, String c, Resource[] r, Parameter[] p) {
		name = n;
		type = t;
		comment = c;
		resources = r;
		parameters = p;
	}
	
	public Resource[] getResources() {
		if (resources == null)
			return new Resource[0];
		return resources;
	}
}