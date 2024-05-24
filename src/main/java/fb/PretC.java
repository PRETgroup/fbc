package fb;

import fbtostrl.FBtoStrl;

public class PretC {
	public String interfaceCode;
	public String code;
	
	public PretC(InterfaceList iface, String c) {
		FBtoStrl.pretCUsed = true;
		
		StringBuilder ifaceCode = new StringBuilder();
		if (iface != null) {
			if (iface.eventInputs != null) {
				for (int i = 0; i < iface.eventInputs.length; i++) {
					String eventName = iface.eventInputs[i].getName();
					
					ifaceCode.append("Event " + eventName + "\n");
		
				}
			}

			if (iface.eventOutputs != null) {
				for (int i = 0; i < iface.eventOutputs.length; i++) {
					String eventName = iface.eventOutputs[i].getName();
					
					ifaceCode.append("Event " + eventName + "\n");
				}
			}

			if (iface.inputVars != null) {
				for (int i = 0; i < iface.inputVars.length; i++) {
					ifaceCode.append("Var " + iface.inputVars[i].getType() + " " + iface.inputVars[i].getName() + "\n");
				}
			}

			if (iface.outputVars != null) {
				for (int i = 0; i < iface.outputVars.length; i++) {
					ifaceCode.append("Var " + iface.outputVars[i].getType() + " " + iface.outputVars[i].getName() + "\n");
				}
			}
		}
		
		interfaceCode = ifaceCode.toString();
		
		code = c;
	}
}
