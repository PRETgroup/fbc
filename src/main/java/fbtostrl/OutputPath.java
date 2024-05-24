/**
 * Helper class to for storing and managing the output path to generate code to.
 */
package fbtostrl;

import java.io.File;

/**
 * @author lihsien
 *
 */
public class OutputPath {
	private String path;		// the path specified by the --O option
	private String subfolder;	// the device subfolder
	
	OutputPath() {}
	
	final void setPath(String p) {
		if (p == null)
			return;
		if ( !p.isEmpty() && !p.endsWith(File.separator) )
			p += File.separator;
		if (path == null)
			path = p;
		else
			subfolder = p;
	}
	
	public final String getPath() {
		if (path == null)
			return null;
		String subdir = (subfolder == null) ? "" : subfolder;
		return path + subdir;
	}
	
	public final String getPathOnly() {
		return path;
	}
	
	protected OutputPath clone() {
		OutputPath o = new OutputPath();
		o.path = path;
		o.subfolder = subfolder;
		return o;
	}
}
