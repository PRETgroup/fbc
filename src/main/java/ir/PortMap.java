/* PortMap.java
 * Helper class to maintain port mappings
 *
 * Written by: Li Hsien Yoong
 */

package ir;

import java.util.*;

import fbtostrl.OutputLevel;
import fbtostrl.OutputManager;

public class PortMap {
	private HashMap<Signal, HashSet<Signal>> map = new HashMap<Signal, HashSet<Signal>>();
	
	public PortMap() {}
	
	/* DESCRIPTION: Adds connection information to the port map
	 * ARGUMENTS: mport - name of the port being mapped
	 *            mtype - the type of port being mapped
	 *            port - name of the connecting port
	 *            type - the type of mapping
	 */
	public void add(PortName mport, PortType mtype, PortName port, PortType type) {
		Signal sig = new Signal(mport, mtype);
		if (map.containsKey(sig)) {
			Signal bind = map.get(sig).iterator().next(); 
			if (bind.getPortType() == PortType.PARAM || type == PortType.PARAM) {
				OutputManager.printError(bind.instance, "Error binding " + port.getFullName() + " to " + 
				           mport.getFullName() + ": previously bound to " + 
				           bind.getFullName(), OutputLevel.FATAL);
				System.exit(0);
			}			
			map.get(sig).add(new Signal(port, type));
		}
		else {
			HashSet<Signal> connections = new HashSet<Signal>();
			connections.add(new Signal(port, type));
			map.put(sig, connections);
		}
	}
	
	public void add(Signal key, Signal sig) {
		HashSet<Signal> signals = map.get(key);
		if (signals == null) {
			signals = new HashSet<Signal>();
			signals.add(sig);
			map.put(key, signals);
		}
		else
			signals.add(sig);
	}
	
	public void put(Signal sig, HashSet<Signal> sigList) {
		map.put(sig, sigList);
	}
	
	public Set<Signal> getSignalSet() {
		return map.keySet();
	}
	
	public HashSet<Signal> getBindings(Signal sig) {
		return map.get(sig);
	}
	
	public int size() {
		return map.size();
	}
	
	/* Prints to string the map entry for the given key "sig" */
	public String stringEntry(Signal sig) {
		StringBuilder entry = new StringBuilder(sig.toString() + " -> ");
		HashSet<Signal> signals = map.get(sig); 
		if (signals != null) {
			Iterator<Signal> i = signals.iterator();
			entry.append(i.next().toString());
			while (i.hasNext())
				entry.append(", " + i.next().toString());
		}
		return entry.toString();
	}
}
