/* GlobalVar.java
 * Class for representing global variables in an abstract manner.
 * 
 * Written by: Li Hsien Yoong
 */

package ir;

import fb.VarDeclaration;
import fbtostrl.OutputLevel;
import fbtostrl.OutputManager;

import java.util.*;

public class GlobalVar {
	private String givenName;			// global variable name (corresponds to ID parameter)
	private String typeName;			// global variable type
	private ArrayList<ArrayList<VarDeclaration>> members;	// members of the global variable type
	private VarDeclaration status;		// for variables with not only value, but status
	private Prototype readMethod;		// read method
	private Prototype writeMethod;		// write method 
	private Prototype writeNullMethod;	// method to write null data (data is absent)
	
	public GlobalVar(String t, VarDeclaration[] m, VarDeclaration s, Prototype read, 
			Prototype write, Prototype writeNull) {
		givenName = t;
		typeName = t;
		if (m != null) {
			members = new ArrayList<ArrayList<VarDeclaration>>(m.length);
			for (int i = 0; i < m.length; i++) {
				members.add(new ArrayList<VarDeclaration>());
				members.get(i).add(new VarDeclaration(m[i]));
			}
		}
		status = new VarDeclaration(s);
		readMethod = read;
		writeMethod = write;
		writeNullMethod = writeNull;
	}
	
	public GlobalVar(String t, VarDeclaration s) {
		givenName = t;
		typeName = t;
		status = new VarDeclaration(s);
	}
	
	public GlobalVar() {}
	
	final public String getRawTypeName() {
		return givenName;
	}
		
	final public String getTypeName() {
		return "T" + typeName;
	}
	
	final public void setTypeName(String t) {
		if (typeName.equals(givenName))
			typeName = t;
	}
	
	final public void setMembers(VarDeclaration[] fields) {
		if (members == null) {
			// Members have not been initialized: create a list to contain all members
			members = new ArrayList<ArrayList<VarDeclaration>>(fields.length);
			for (int i = 0; i < fields.length; i++) {
				members.add(new ArrayList<VarDeclaration>());
				members.get(i).add(new VarDeclaration(fields[i]));
			}
		}
		else {
			// Members have been initialized: ensure that the created list is large enough
			// to hold all incoming members.
			int size = members.size();
			for (int i = size; i < fields.length; i++)
				members.add(new ArrayList<VarDeclaration>());
			
			// Now, add all new members
			for (int i = 0; i < fields.length; i++)
				members.get(i).add(new VarDeclaration(fields[i]));
		}
	}
	
	/**
	 * Creates sets for each member of this global variable, where a set may consists of 
	 * conflicting versions of a particular member. 
	 * @return Sets for each member of this global variable
	 */
	public ArrayList<ArrayList<VarDeclaration>> makeMemberSets() {
		for (ArrayList<VarDeclaration> set : members) {
			int i = 0;
			if (i < set.size() - 1) {
				do {
					VarDeclaration left = set.get(i);
					// Ensure that all data ports have concrete types. If it is still an
					// ANY at this stage, it means that that data port is not bound to some
					// other non-ANY port. In this case, we default to BOOL.
					if (left.getType().equals("ANY"))
						left.setType("BOOL");
					
					int j = i+1;
					while (j < set.size()) {
						VarDeclaration right = set.get(j);
						if (right.getType().equals("ANY"))
							right.setType("BOOL");
						if ( left.getType().equals(right.getType()) && 
							 left.getArraySize().equals(right.getArraySize()) ) {
							set.remove(j);
						}
						else
							j++;
					}
					i++;
				} while (i < set.size() - 1);
			}
			else {
				VarDeclaration var = set.get(0);
				if (var.getType().equals("ANY"))
					var.setType("BOOL");
			}
		}
		return members;
	}
	
	/**
	 * This function returns the list of members for this global variable. If any member
	 * has a conflicting data type in the function block description, this function will
	 * throw an error.
	 * @return The list of members for this global variable.
	 */
	final public VarDeclaration[] getMembers(String devName) {
		makeMemberSets();	// create member sets and remove any unresolved ANY types
		
		VarDeclaration[] fields = new VarDeclaration[members.size()];
		for (int i = 0; i < fields.length; i++) {
			ArrayList<VarDeclaration> vars = members.get(i);
			if (vars.size() > 1) {
				OutputManager.printError(devName, "Data element " + Integer.toString(i+1) +
						" of `" + givenName + "' has conflicting data type.\n", 
						OutputLevel.FATAL);
				System.exit(-1);
			}
			fields[i] = vars.get(0);
		}
		return fields;
	}
	
	public void setMemberType(String index, DataType dt) {
		for (ArrayList<VarDeclaration> memberSet : members) {
			// Just check the first member in the set for name consistency
			VarDeclaration m = memberSet.get(0); 
			int i = m.getName().lastIndexOf('_');
			if (i < 0)
				continue;
			if (m.getName().substring(i + 1).equals(index)) {
				for (VarDeclaration var : memberSet) {
					if (var.getType() == null) {
						var.setType(dt.getType());
						var.setArraySize(dt.getArraySize());
						break;
					}
				}
				break;
			}
		}
	}
	
	final public VarDeclaration getStatus() {
		return status;
	}
	
	final public Prototype getReadMethod() {
		return readMethod;
	}
	
	final public Prototype getWriteMethod() {
		return writeMethod;
	}
	
	final public Prototype getWriteNullMethod() {
		return writeNullMethod;
	}
	
	final public String getReadMethodRetType() {
		return readMethod.getType();
	}
	
	final public String getWriteMethodRetType() {
		return writeMethod.getType();
	}
	
	final public String getWriteNullMethodRetType() {
		return writeNullMethod.getType();
	}
	
	public boolean isDirectAccess() {
		return (givenName == null);
	}
	
	public GlobalVarType getVarKind() {
		if (givenName == null)
			return GlobalVarType.PLAIN;
		else if (writeMethod == null || readMethod == null)
			return GlobalVarType.PUBSUB;
		else
			return GlobalVarType.UCOS;
	}
	
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append(hashCode() + "\n");
		int size = members.size();
		for (int i = 0; i < size; i++) {
			str.append(" members[" + i +"]:");
			for (VarDeclaration var : members.get(i)) {
				str.append("\n  " + var.getName() + " -- " + var.getType());
			}
		}
			
		return str.toString();
	}
}
