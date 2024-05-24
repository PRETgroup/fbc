/* Token.java
 * Helper class to store different kinds of tokens while parsing transition conditions
 * 
 * Written by: Li Hsien Yoong
 */

package ir;

public class Token {
	String token;
	TokenType type;
	
	Token(String t, TokenType p) {
		token = t;
		type = p;
	}
	
	public String getToken() {
		return token;
	}
	
	public TokenType getType() {
		return type;
	}
}
