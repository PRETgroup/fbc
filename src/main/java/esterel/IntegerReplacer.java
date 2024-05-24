package esterel;

import ir.*;

public class IntegerReplacer extends TokenReplacer {	
	public IntegerReplacer(int i) {
		startIndex = i;
	}
	
	public String replace (int index, SIPath sifbInfo) {
		return Integer.toString(startIndex + index);
	}
}