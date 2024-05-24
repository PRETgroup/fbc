/**
 * Helper class to store information of a resource as an element in a DOM tree.
 */
package fbtostrl;

/**
 * @author Li Hsien Yoong
 */

import org.jdom2.*;
import fb.Parameter;

class ResourceDOMInfo {
	Element dom;		// resource element in the DOM
	String name;		// resource name
	String type;		// resource type
	String baseName;	// path to generate code to
	Parameter[] params;	// parameters to the resource
	
	ResourceDOMInfo(Element d, String n, String t, String b, Parameter[] p) {
		dom = d;
		name = n;
		type = t;
		baseName = b;
		params = p;
	}
	
	/**
	 * Searches the list of existing base resources (baseInfos) for a resource of a 
	 * particular name (resName). If a match is found, the base resource is returned; 
	 * otherwise, null is returned.
	 * @param baseInfos list of base resources 
	 * @param resName resource name to search for
	 * @return The base resource corresponding to the given resource name if found; 
	 *         otherwise null.
	 */
	static ResourceDOMInfo getBaseResource(ResourceDOMInfo[] baseInfos, String resName) {
		for (ResourceDOMInfo baseRes : baseInfos) {
			if (baseRes.name.equals(resName)) {
				return baseRes;
			}
		}
		return null;
	}
}
