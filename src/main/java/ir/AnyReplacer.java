package ir;

import esterel.StrlGenerator;
import fbtostrl.FBtoStrl;
//import ir.*;

public class AnyReplacer extends TokenReplacer {	
	String fqvarname; // needed to lookup the type
	public AnyReplacer(int i, String name) {
		startIndex = i;
		fqvarname = name;
	}
	
	public String replace (int index, SIPath sifbInfo) {
		String fqSig = fqvarname+(index+1); // indexes from 0+ are for ports _1+
		String type = FBtoStrl.anyTypeMap.get(fqSig);
		if( type == null )
			return "// Unset";
		if( FBtoStrl.opts.ccode )
			return type;
		else if( FBtoStrl.opts.strl )
			return StrlGenerator.getDataType(type);
		return "";
	}
}