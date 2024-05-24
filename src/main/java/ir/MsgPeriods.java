/**
 * MsgPeriods.java
 * Helper class to hold send/receive periods of messages for multirate execution.
 * @author Li Hsien Yoong
 */

package ir;

public class MsgPeriods {
	private int sendPeriod;
	private int recvPeriod;
	private int gcd;
	
	public MsgPeriods(int tsend, int trecv) {
		sendPeriod = tsend;
		recvPeriod = trecv;
	}
	
	private void computeGCD(int u, int v) {
		/* Simple cases */
		if (u == 0 || u == v) {
			gcd = v;
			return;
		}
		if (v == 0) {
			gcd = u;
			return;
		}
	
		/* Let shift := lg K, where K is the greatest power of 2 dividing both u and v. */
		int shift;
		for (shift = 0; ((u | v) & 1) == 0; shift++) {
			u >>= 1;
			v >>= 1;
		}
	
		while ((u & 1) == 0)
			u >>= 1;
			
		/* From here on, u is always odd. */
		do {
			while ((v & 1) == 0)  /* Loop X */
				v >>= 1;
				
			/* Now u and v are both odd, so diff(u, v) is even. Let u = min(u, v), v = diff(u, v)/2. */
			if (u < v)
				v -= u;
			else {
				int diff = u - v;
				u = v;
				v = diff;
			}
			v >>= 1;
		} while (v != 0);
	 
	    gcd = (u << shift);
	}
	
	public void setSendPeriod(int period) {
		sendPeriod = period;
		computeGCD(sendPeriod, recvPeriod);
	}
	
	public void setRecvPeriod(int period) {
		recvPeriod = period;
		computeGCD(sendPeriod, recvPeriod);
	}
	
	public int getSendPeriod() {
		return sendPeriod;
	}
	
	public int getRecvPeriod() {
		return recvPeriod;
	}
	
	public int getGCD() {
		return gcd;
	}
	
	public String toString() {
		return sendPeriod + "," + recvPeriod;
	}
}
