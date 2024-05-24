package fb;

public class EnumeratedValue {
	protected String name;
	protected String comment;
	
	public EnumeratedValue(String n, String c) {
		name = n;
		comment = c;
	}
	
	public String getName()
	{
		return name;
	}
	
	public String getComment()
	{
		return comment;
	}
}
