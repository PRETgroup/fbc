package ir;

public abstract class TokenReplacer {
	protected int startIndex;
	
	public TokenReplacer() {};
	public abstract String replace(int index, SIPath sifbInfo);
}