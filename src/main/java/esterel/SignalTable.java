/* SignalTable.java
 * Stores the mapping for all signals in the composite block
 *
 * Written by: Li Hsien Yoong
 */

package esterel;

import fbtostrl.OutputLevel;
import fbtostrl.OutputManager;
import ir.*;

import java.util.*;

public class SignalTable {
	private static final int PARAMBIT = 0x01;
	private static final int INPUTBIT = 0x02;
	private static final int OUTPUTBIT = 0x04;
	private static final int LOCALBIT = 0x08;
	private static final int MULTIBIT = 0x10;
	private LinkedHashMap<Signal, SignalInfo> map = new LinkedHashMap<Signal, SignalInfo>();
	private PortMap portMap = new PortMap();
	private PortMap autoMap = new PortMap();
	
	public SignalTable(Instance[] runs, NameDB sigDB, LinkedList<CodeGenerator> deferredBlocks) {
		for (int i = 0; i < runs.length; i++) {
			CodeGenerator cg = null;
			Set<Signal> signals = runs[i].getSignalSet();
			Iterator<Signal> j = signals.iterator();
			while (j.hasNext()) {
				Signal sig = j.next();
				HashSet<Signal> sigList = runs[i].getBindings(sig);
				if (sig.getSigType() != null) {
					if (cg == null) {
						Iterator<CodeGenerator> k = deferredBlocks.iterator();
						while (k.hasNext()) {
							cg = k.next();
							if (cg.getName().equals(runs[i].getName()))
								break;
						}
					}
					resolveAnyType(sig, sigList, (StrlGenerator)cg);
				}
				portMap.put(sig, sigList);
				map.put(sig, null);
			}
		}
		
		int index = 0;
		Set<Signal> signals = map.keySet();
		Iterator<Signal> i = signals.iterator();
		
		// Process all the output ports first
		while (i.hasNext()) {
			Signal sig = i.next();
			if (sig.getPortType() != PortType.OUTPUT)
				continue;
			HashSet<Signal> sigList = portMap.getBindings(sig);
			int size = sigList.size();
			if (size > 1) { 
				String local = sigDB.addUniqueName(String.format("signal%d_", index));
				index++;
				SignalInfo entry = new SignalInfo(local, PortType.LOCAL, 
						                          sig.getSigType(), sig.getArraySize()); 
				map.put(sig, entry);
				Iterator<Signal> j = sigList.iterator();
				while (j.hasNext()) {
					Signal s = j.next();
					if (s.getPortType() == PortType.OUTPUT)
						autoMap.add(s, entry);	// create drivers for top-level outputs
				}
			}
			else if (size == 1) {
				Signal s = sigList.iterator().next();
				if (s.getPortType() == PortType.OUTPUT)
					map.put(sig, new SignalInfo(s));
				else {
					String local = sigDB.addUniqueName(String.format("signal%d_", index));
					index++;
					map.put(sig, new SignalInfo(local, PortType.LOCAL, 
							sig.getSigType(), sig.getArraySize()));
				}
			}
		}
		
		// Next, process all the input ports
		i = signals.iterator();
		while (i.hasNext()) {
			Signal sig = i.next();
			if (sig.getPortType() != PortType.INPUT)
				continue;
			HashSet<Signal> sigList = portMap.getBindings(sig);
			int size = sigList.size();
			if (size > 1) {
				String local = sigDB.addUniqueName(String.format("signal%d_", index));
				index++;
				SignalInfo entry = new SignalInfo(local, PortType.AUTO, 
						                          sig.getSigType(), sig.getArraySize()); 
				map.put(sig, entry);
				Iterator<Signal> j = sigList.iterator();
				while (j.hasNext()) {
					Signal s = j.next();
					if (s.getPortType() == PortType.INPUT)
						autoMap.add(entry, s);
					else
						autoMap.add(entry, map.get(s));
				}
			}
			else if (size == 1) {
				Signal s = sigList.iterator().next();
				PortType pt = s.getPortType();
				switch (pt) {
					case PARAM:
						String local = 
							sigDB.addUniqueName(String.format("signal%d_", index));
						index++;
						map.put(sig, new SignalInfo(local, pt, sig.getSigType(), 
								sig.getArraySize(), s.getSignal()));
						break;
					case INPUT:
						map.put(sig, new SignalInfo(s));
						break;
					case LOCAL:
						map.put(sig, map.get(s));
						break;
				}	
			}
		}
	}
	
	/* DESCRIPTION: Returns a bit vector indicating the type of bindings on a 
	 *              particular port
	 * ARGUMENTS: connections - list of port bindings 
	 * RETURNS: bit vector specifying all binding types in the given port bindings list
	 */
	static int bindingType(HashSet<Signal> connections) {
		int type = 0;
		
		Iterator<Signal> i = connections.iterator();
		while (i.hasNext()) {
			Signal bind = i.next();
			switch (bind.getPortType()) {
				case PARAM:
					type |= PARAMBIT;
					break;
				case INPUT:
					if ((type & INPUTBIT) == INPUTBIT)
						type |= MULTIBIT;
					else
						type |= INPUTBIT;
					break;
				case OUTPUT:
					if ((type & OUTPUTBIT) == OUTPUTBIT)
						type |= MULTIBIT;
					else
						type |= OUTPUTBIT;
					break;
				case LOCAL:
				case LOCALDELAYED:
					type |= LOCALBIT;
					break;
			}
		}
		
		if ((type & (INPUTBIT | OUTPUTBIT)) == (INPUTBIT | OUTPUTBIT)) {
			String error = "Input/Output conflict on port connections: ";
			String blockid = "";
			i = connections.iterator();
			while (i.hasNext()) {
				error += i.next().getFullName() + " ";
				blockid = i.next().getInstance();
			}
			OutputManager.printError(blockid, error, OutputLevel.FATAL);
			System.exit(0);
		}
		
		return type;
	}
	
	Collection<SignalInfo> getSigBindings() {
		return map.values();
	}
	
	public HashSet<SignalInfo> getUniqueSigBindings() {
		HashSet<SignalInfo> set = new HashSet<SignalInfo>();
		Iterator<SignalInfo> i = map.values().iterator();
		while (i.hasNext())
			set.add(i.next());
		return set;
	}
	
	public SignalInfo getBinding(Signal sig) {
		return map.get(sig);
	}
	
	public Set<Signal> getAuxSignalSet() {
		return autoMap.getSignalSet();
	}
	
	public HashSet<Signal> getAuxRelationSet(Signal sig) {
		return autoMap.getBindings(sig);
	}
	
	/* Resolve the ANY data type to a concrete type */ 
	private void resolveAnyType(Signal sig, HashSet<Signal> sigList, StrlGenerator cg) {
		String type = sig.getSigType(); 

		if (type.equalsIgnoreCase("ANY")) {
			Iterator<Signal> i = sigList.iterator();
			while (i.hasNext()) {
				Signal s = i.next();
				type = s.getSigType();
				if (!type.equalsIgnoreCase("ANY")) {
					sig.setSigType(type);
					setAllAnyType(type, sigList);
					break;
				}
			}
			if (cg != null)
				cg.defineAnyPort(sig.getSignal(), new DataType(type, sig.getArraySize()));
		}
		else {
			setAllAnyType(type, sigList);
		}
	}
	
	private void setAllAnyType(String type, HashSet<Signal> sigList) {
		Iterator<Signal> i = sigList.iterator();
		while (i.hasNext()) {
			Signal s = i.next();
			if (s.getSigType().equalsIgnoreCase("ANY"))
				s.setSigType(type);
		}
	}
}
