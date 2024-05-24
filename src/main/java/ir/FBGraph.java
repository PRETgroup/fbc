/* FBGraph.java
 * Represents a function block network as a directed graph.
 *
 * Written by: Li Hsien Yoong
 */

package ir;

import fb.*;
import fbtostrl.OutputLevel;
import fbtostrl.OutputManager;

import java.util.*;

public class FBGraph {
	String name;	// instance name
	String type;	// type
	Block[] nodes;
	Block[] sortedNodes;
	private int sortedPos; 
	private HashSet<Block> visited = new HashSet<Block>();
	private boolean removeCycle;
	
	public FBGraph(String n, FBType fbt, boolean c) {
		name = n;
		removeCycle = c;
		type = fbt.getName();
		FBType[] blocks = fbt.getSubBlocks();
		FBNetwork f = fbt.getFBNetwork();
		if( f.instances == null )
		{
			return; // Nothing to do here
		}
		int len = f.instances.length;
		assert (len == blocks.length): "Length of sub-blocks != length of network instances.\n";
		nodes = new Block[len];
		sortedNodes = new Block[len];
		for (int i = 0; i < len; i++) {
			if (blocks[i].getFBNetwork() != null || blocks[i].getBasicFB() != null || 
				blocks[i] instanceof AdapterType) {
				nodes[i] = new Block(f.instances[i], blocks[i].getInterfaceList());
			}
			else {
				nodes[i] = new Block(f.instances[i], blocks[i].getInterfaceList(),
						             blocks[i].getSIFBname(), blocks[i].getSIFBindex(),
						             blocks[i].getSIFBQlen());
			}
		}
		
		LinkedList<Arc> arcs = new LinkedList<Arc>();
		if (f.eventConnections != null) {
			for (int j = 0; j < f.eventConnections.length; j++)
				arcs.add(new Arc(f.eventConnections[j], f.instances));
		}
		
		if (f.dataConnections != null) {
			for (int j = 0; j < f.dataConnections.length; j++) {
				arcs.add(new Arc(fbt.getInterfaceList(), f.dataConnections[j], 
						         f.instances, blocks));
			}
		}
				
		// Connect blocks to their successors
		ArrayList<Block> dest = new ArrayList<Block>();
		ArrayList<Arc> localArcs = new ArrayList<Arc>();
		for (int i = 0; i < len; i++) {
			dest.clear();
			localArcs.clear();
			ListIterator<Arc> j = arcs.listIterator();
			while (j.hasNext()) {
				Arc arc = j.next();
				if (arc.isDelayed)
					continue; // Don't need to worry about delayed connections
				if (arc.source.instance.equals(nodes[i].instance.name)) {
					localArcs.add(arc);
					if (arc.destination.instance.equals(""))
						dest.add(null);
					else {
						for (int k = 0; k < len; k++) {
							if (arc.destination.instance.equals(nodes[k].instance.name)) {
								dest.add(nodes[k]);
								break;
							}
						}
					}
				}
			}
			nodes[i].connectSuccs(dest, localArcs);
		}
		
		// Connect blocks to the predecessors
		for (int i = 0; i < len; i++) {
			dest.clear();
			localArcs.clear();
			ListIterator<Arc> j = arcs.listIterator();
			while (j.hasNext()) {
				Arc arc = j.next();
				if (arc.isDelayed)
					continue; // Don't need to worry about delayed connections
				if (arc.destination.instance.equals(nodes[i].instance.name)) {
					localArcs.add(arc);
					if (arc.source.instance.equals(""))
						dest.add(null);
					else {
						for (int k = 0; k < len; k++) {
							if (arc.source.instance.equals(nodes[k].instance.name)) {
								dest.add(nodes[k]);
								break;
							}
						}
					}
					j.remove();
				}
			}
			nodes[i].connectPreds(dest, localArcs);
		}
	}
	
	/* DESCRIPTION: Visits blocks recursively to topologically sort them
	 *              Updated to Support UnitDelayed Connections
	 * PARAMETERS: node - currently visited block
	 *             pos - position in the sorted list
	 *             stack - stack of visited nodes for a given recursion
	 * RETURNS: TRUE if cycle detected; FALSE otherwise
	 */
	boolean visit(Block node, LinkedList<Block> stack) {
		if (stack.contains(node)) {
			StringBuilder notice = new StringBuilder("Cycle detected between: ");
			// Note: No '--acyc' behaviour
			/*if (removeCycle)
				notice = "Cycle broken: ";
			else*/
			Iterator<Block> j = stack.iterator();
			notice.append( j.next().getInstanceName() );
			while (j.hasNext()) {
				notice.append( ", " + j.next().getInstanceName() );
			}

			if (!removeCycle) {
				OutputManager.printError(node.getIDname(), notice.toString(), OutputLevel.FATAL);
				System.exit(0);
			}
			return true;
		}
		if (node == null || visited.contains(node))
			return false;
		visited.add(node);
		
		stack.addLast(node);
		for (int i = 0; i < node.predecessors.length; i++) {
			if (visit(node.predecessors[i], stack))
				break;
		}
		stack.removeLast();
		
		sortedNodes[sortedPos] = node;
		sortedPos++;
		return false;
	}
	
	public Block[] sortTopologically() {
		if (nodes == null)
			return null;
		
		LinkedList<Block> stack = new LinkedList<Block>();
		for (int i = 0; i < nodes.length; i++) {
			visit(nodes[i], stack);
		}
		return sortedNodes;
	}
	
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append(name + " : " + type + "\n");
		if (nodes != null) {
			for (int i = 0; i < nodes.length; i++)
				str.append(nodes[i].toString());
		}
		return str.toString();
	}
}
