/* DataType.java
 * Helper class to store information on data type
 *
 * Written by: Li Hsien Yoong
 */

package ir;

public class DataType {
	String type;
	String arraySize;
	
	public DataType(String t, String a) {
		type = t;
		arraySize = a.trim();
	}
	
	public final String getType() {
		return type;
	}
	
	public final void setType(String t) {
		type = t;
	}
	
	public final String getArraySize() {
		return arraySize;
	}
	
	public boolean equals(Object o) {
		if (o instanceof DataType) {
			DataType d = (DataType) o;
			return (type.equals(d.type) && arraySize.equals(d.arraySize));
		}
		return false;
	}
}
