package fb;

public class BasicHCECCFB extends BasicFB {

	public Event[] internalSignals; // pure internal signals
	public HCECC hcecc;
	public BasicHCECCFB(VarDeclaration[] v, HCECC h, Algorithm[] a) {
		super(v, h, a); // Sets ECC root etc
		internalVars = v;
		//internalSignals = s;
		hcecc = h;
		
		algorithms = a;
		if (algorithms != null)
			hcecc.mapAlgo(algorithms);
	}
	
	public HCECC getHCECC() {
		return hcecc;
	}
	
	public String rootECCName() {
		return hcecc.name;
	}

}
