package esterel;

import ir.*;

public class AnyToReplacer extends TokenReplacer {
	private String prefix;
	
	public AnyToReplacer(int i, String p) {
		startIndex = i;
		prefix = p;
	}
	
	public String replace (int index, SIPath sifbInfo) {
		int i = startIndex + index;
		return sifbInfo.getAnyToConverterCall(i) + "(" + prefix + "[" + Integer.toString(i) + "])";
	}
}