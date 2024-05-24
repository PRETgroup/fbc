package fb;

public class AdapterType extends FBType{

	// <Service RightInterface="PLUG" LeftInterface="SOCKET"> etc
	public AdapterType(String n, String c, String[] h, InterfaceList i) {
		super(n, c, h, i);
		// set sifb back to false :|
		sifb = false;
		
		
	}

}
