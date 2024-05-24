/* NameDB.java
 * Defines the Name database.
 * 
 * Written by: Li Hsien Yoong
 */

package ir;

import java.util.*;

public class NameDB {
	private HashSet<String> names;	// stores the names entered in this database
	private ArrayList<NameDB> commonDBs;	// other databases whose names are 
											// also of interest  
	
	public NameDB(NameDB common) {
		names = new HashSet<String>();
		commonDBs = new ArrayList<NameDB>();
		addCommonDB(common);
	}
	
	void addCommonDB(NameDB common) {
		if (common != null) {
			commonDBs.add(common);
		}
	}
	
	void addCommonDB(NameDB[] commons) {
		for (int i = 0; i < commons.length; i++)
			addCommonDB(commons[i]);
	}
	
	public String getUniqueName(String name) {
		String newName = name;
		int index = 1;
		while (contains(newName)) {
			newName = String.format(name + "%d", index);
			index++;
		}
		return newName;
	}
	
	// If "name" is unique, it is added to the database. Otherwise, a unique name
	// is generated and added to the database. The name that is added to the 
	// database is returned.
	public String addUniqueName(String name) {
		String newName = getUniqueName(name);
		add(newName);
		return newName;
	}
	
	public String addLocalUniqueName(String name) {
		String newName = name;
		int index = 1;
		while (containsLocal(newName)) {
			newName = String.format(name + "%d", index);
			index++;
		}
		add(newName);
		return newName;
	}
	
	public void add(String name) {
		names.add(name);
	}
	
	boolean containsLocal(String name) {
		return names.contains(name);
	}
	
	boolean contains(String name) {
		if (containsLocal(name))
			return true;
		ListIterator<NameDB> i = commonDBs.listIterator();
		while (i.hasNext()) {
			if (i.next().names.contains(name))
				return true;
		}
		return false;
	}
	
	void clear() {
		names.clear();
		commonDBs.clear();
	}
}
