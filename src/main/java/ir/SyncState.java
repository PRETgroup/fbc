/* SyncState.java
 * Defines the synchronous state object.
 * 
 * Written by: Li Hsien Yoong
 */

package ir;

import java.util.*;

import fb.*;

// Group of conditions(s)-destination states
/*class ConditionsDst {
	SyncState destination;
	ArrayList<Condition> conditions;
	
	ConditionsDst(SyncState dst, Condition cond) {
		destination = dst;
		conditions = new ArrayList<Condition>();
		conditions.add(cond);
	}
}*/

// List of parent states with common children
/*class CommonDestination {
	LinkedList<ConditionsDst> dstGroups;
	
	CommonDestination () {
		dstGroups = new LinkedList<ConditionsDst>();
	}
	
	// Adds a new condition for a given destination state. If the destination 
	// had already been added, the new condition will be appended to the list. 
	// Otherwise, a new entry will be created for both the destination state and
	// condition.
	void add(SyncState dst, Condition cond) {
		ListIterator<ConditionsDst> i = dstGroups.listIterator();
		while (i.hasNext()) {
			ConditionsDst cd = i.next();
			if (cd.destination == dst) {
				ListIterator<Condition> j = cd.conditions.listIterator();
				while (j.hasNext()) {
					Condition prevCond = j.next();
					if ( prevCond.guardCondition.equals(cond.guardCondition) &&
						 prevCond.inputEvent.equals(cond.inputEvent) )
						return;
				}
				cd.conditions.add(cond);
				return;
			}
		}
		dstGroups.add(new ConditionsDst(dst, cond));
	}
	
	// Remove entries that have only a single condition
	void removeSingleConds() {
		ListIterator<ConditionsDst> i = dstGroups.listIterator();
		while (i.hasNext()) {
			if (i.next().conditions.size() < 2)
				i.remove();
		}
	}
	
	ArrayList<Condition> getConditions(SyncState dst) {
		ListIterator<ConditionsDst> i = dstGroups.listIterator();
		while (i.hasNext()) {
			ConditionsDst cd = i.next(); 
			if (cd.destination == dst)
				return cd.conditions;
		}
		return null;
	}
	
	int size() {
		return dstGroups.size();
	}
}*/

public class SyncState {
	private FunctionBlock module;		// container funciton block 
	String name;
	public String comment;
	public Action[] actions;
	public Condition[] conditions;
	public SyncState[] children;
	public SyncState[] parents;
	
	// For HCECC
	public String refiningECCName;
	public SyncState[] refiningStates;
	public LinkedHashMap<String,SyncState[]> parallel; // LinkedHashMap because they need to be ordered
	
	// Pure data conditions of child states
	public HashSet<Condition> childConditions = new HashSet<Condition>();
	public int index = -1;
	
	public SyncState(FunctionBlock m, ECState e) {
		module = m;
		name = e.getName();
		comment = e.getComment();
		if (e.actions != null) {
			actions = new Action[e.actions.length];
			for (int i = 0; i < e.actions.length; i++)
				actions[i] = new Action(module, e.actions[i]);
		}
		
		/*if (e.conditions != null) {			
			conditions = new Condition[e.conditions.length];
			for (int i = 0; i < e.conditions.length; i++)
				conditions[i] = new Condition(e.conditions[i]);			
		}*/
		if (e.transitions != null) {			
			conditions = new Condition[e.transitions.length];
			for (int i = 0; i < e.transitions.length; i++)
			{
				Action[] tActions = null;
				if (e.transitions[i].actions != null) {
					tActions = new Action[e.transitions[i].actions.length];
					for (int a = 0; a < e.transitions[i].actions.length; a++)
						tActions[a] = new Action(module, e.transitions[i].actions[a]);
				}
				
				conditions[i] = new Condition(e.transitions[i].getGuardCondition(), tActions);
			}
		}
		
	}
	
	// Parse every transition condition
	void parseConditions() {
		if (conditions != null) {
			for (int i = 0; i < conditions.length; i++)
				conditions[i].parseCondition(module);
		}
	}
	
	public String getName() {
		return name;
	}
	
	public String toString() {
		return "SyncState: " + name;
	}
}
