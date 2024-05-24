package esterel;

import ir.*;

public class ToAnyReplacer extends TokenReplacer {
	private String prefix;
	
	public ToAnyReplacer(int i, String p) {
		startIndex = i;
		prefix = p;
	}
	
	public String replace (int index, SIPath sifbInfo) {
		int i = startIndex + index;
		return sifbInfo.getToAnyConverterCall(i) + "(" + prefix + Integer.toString(i+1) + ")";
	}
}