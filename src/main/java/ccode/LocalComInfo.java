package ccode;

import ir.*;

public class LocalComInfo {
	public String event;
	public String testEvent;
	public String insName;
	public String blockType;
	public String partnerType;
	public int index = -1;
	public int qlen;
	
	public LocalComInfo(String e, String test, String n, String t) {
		event = e;
		testEvent = test;
		insName = n;		
		blockType = t.substring(0, 5);
		if (blockType.equals("SEND_"))
			partnerType = "RECV_";
		else if (blockType.equals("RECV_"))
			partnerType = "SEND_";
		String tail = t.substring(5);
		if (SIPath.isAllDigit(tail))
			index = Integer.parseInt(tail);
		else {
			int idx = tail.indexOf('_');
			if (idx > 0) {
				index = Integer.parseInt(tail.substring(0, idx));
				qlen = Integer.parseInt(tail.substring(idx + 1));
			}
		}
	}
}
