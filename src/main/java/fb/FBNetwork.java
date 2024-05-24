/* FBNetwork.java
 * Defines the composite function block.
 * 
 * Written by: Li Hsien Yoong
 */

package fb;

import java.io.BufferedWriter;
import java.io.IOException;


public class FBNetwork {
	public FBInstance[] instances;
	public Connection[] eventConnections;
	public Connection[] dataConnections;
	// Kyle: public Connection[] methodConnections;
	
	public FBNetwork(FBInstance[] inst, Connection[] ec, Connection[] dc) {
		instances = inst;
		eventConnections = ec;
		dataConnections = dc;
	}
	
	/* Kyle:
	public FBNetwork(FBInstance[] inst, Connection[] ec, Connection[] dc, Connection[] mc) {
		instances = inst;
		eventConnections = ec;
		dataConnections = dc;
		methodConnections = mc;
	}
	*/

	public void toXML(BufferedWriter printer, String indent) throws IOException 
	{
		printer.append(indent+"<FBNetwork>\n");
		if( instances != null )
		{
			for( FBInstance inst : instances)
			{
				inst.toXML(printer, indent+"\t");
			}
		}
		if( eventConnections != null )
		{
			printer.append(indent+"\t<EventConnections>\n");
			for( Connection con : eventConnections )
			{
				con.toXML(printer, indent+"\t\t");
			}
			printer.append(indent+"\t</EventConnections>\n");
		}
		if( dataConnections != null )
		{
			printer.append(indent+"\t<DataConnections>\n");
			for( Connection con : dataConnections )
			{
				con.toXML(printer, indent+"\t\t");
			}
			printer.append(indent+"\t</DataConnections>\n");
		}
		printer.append(indent+"</FBNetwork>\n");
		
	}
}