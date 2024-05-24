package fb;

// Plugs are used at the interface as a single port.
// But within the network they are just like a normal FB
// Socket == Adapter
// Plug == Adapter but reversed I/O
public class Socket extends FBInstance {

	public Socket(String n, String t) { // t = adapter type
		super(n, t, null);
	}

}
