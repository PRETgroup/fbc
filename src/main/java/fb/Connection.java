/* Connection.java
 * Helper class to describe function block connections.
 * 
 * Written by: Li Hsien Yoong
 */

package fb;

import java.io.BufferedWriter;
import java.io.IOException;

public class Connection {
	public String source;
	public String destination;
	public boolean isDelayed;
	
	public Connection(String src, String dst) {
		source = src;
		destination = dst;
	}
	
	public Connection(String src, String dst, boolean delay) {
		source = src;
		destination = dst;
		isDelayed = delay;
	}

	public void toXML(BufferedWriter printer, String indent) throws IOException {
		printer.append(indent+"<Connection Source=\""+source+"\" Destination=\""+destination+"\" />\n");
	}
}