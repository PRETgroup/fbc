/**
 * This class manages properties for extended TTP primitive data types.
 */
package d3;

/**
 * @author lihsien
 *
 */
public class MsgProperties {
	public String length;
	public String typeCat;
	public String typedef;
	public String typeLength;
	
	MsgProperties(String len, String cat, String def, String varLen) {
		length = len;
		typeCat = cat;
		typedef = def;
		typeLength = varLen;
	}
}
