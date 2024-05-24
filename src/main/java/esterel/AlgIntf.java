/* AlgIntf.java
 * Defines a common interface to be extended by all modules implementing algorithms
 * 
 * Written by: Li Hsien Yoong
 */

package esterel;

import ir.*;
import java.util.*;

public class AlgIntf {
	String name;
	private LinkedList<VarType> elems = new LinkedList<VarType>();
	
	AlgIntf() {}
	
	void add(VarType e) {
		elems.add(e);
	}
	
	ListIterator<VarType> listIterator() {
		return elems.listIterator();
	}
	
	boolean isEmpty() {
		if (name != null) {
			return name.isEmpty();
		}
		return true;
	}
}