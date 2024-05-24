/* Condition.java
 * Defines the transition condition object.
 * 
 * Written by: Li Hsien Yoong
 */

package ir;

import java.util.*;

import fbtostrl.OutputLevel;
import fbtostrl.OutputManager;

class TokenSet {
	HashSet<Token> set = new HashSet<Token>();
	
	TokenSet() {}
	
	void add(Token token) {
		set.add(token);
	}
	
	Token getToken(String t) {
		Iterator<Token> i = set.iterator();
		while (i.hasNext()) {
			Token tok = i.next();
			if (tok.token.equals(t))
				return tok;
		}
		return null;
	}
}

public class Condition {
	private String condition;
	private ConditionType type;
	private LinkedList<Token> guard = new LinkedList<Token>();
	private Action[] actions;
	
	public Action[] getActions() {
		return actions;
	}

	public Condition(String c, Action[] a) {
		condition = c.trim();
		actions = a;
	}
	
	public static boolean isNumeric(String str) {
		try {
			Double.parseDouble(str);
			return true;
		} catch (NumberFormatException nfe) {
			return false;
		} 
	}
	
	private static boolean isLiteral(String str) {
		if (str.equalsIgnoreCase("TRUE") || str.equalsIgnoreCase("FALSE"))
			return true;
		else
			return isNumeric(str);
	}
	
	// Sets the condition type to indicate that it involves an event
	private void setEventType() {
		if (type != ConditionType.EVENTDATA) {
			if (type == ConditionType.DATA)
				type = ConditionType.EVENTDATA;
			else
				type = ConditionType.EVENT;
		}
	}
	
	// Sets the condition type to indicate that it involves a data
	private void setDataType() {
		if (type != ConditionType.EVENTDATA) {
			if (type == ConditionType.EVENT)
				type = ConditionType.EVENTDATA;
			else
				type = ConditionType.DATA;
		}
	}
	
	public boolean isDataType() {
		return (type == ConditionType.DATA || type == ConditionType.EVENTDATA);
	}
	
	public boolean equalsType(ConditionType t) {
		return (type == t);		
	}
	
	public String getInputEvent() {
		ListIterator<Token> i = guard.listIterator();
		while (i.hasNext()) {
			Token tok = i.next();
			if (tok.getType() == TokenType.EVENT)
				return tok.getToken();
		}
		assert(false): "Event token not found.\n";
		
		return null;
	}
	
	// Parse the transition condition to extract the guard condition if any 
	public void parseCondition(FunctionBlock module) {
		if (condition.equals("1") || condition.equalsIgnoreCase("TRUE")) {
			type = ConditionType.IMMEDIATE;
			guard.add(new Token("1", TokenType.LITERAL));
			return;
		}
			
		TokenSet tokenSet = new TokenSet();
		tokenizeAlphaNumeric(tokenSet, module);
		tokenize(tokenSet);
	}
	
	public LinkedList<Token> getConditionTokens() {
		return guard;
	}
	
	public String getCondition() {
		return condition;
	}
	
	/* DESCRIPTION: Tokenize alpha-numberic elements in the transition condition
	 * PARAMETERS: tokenSet - set to store tokens of identifiers, keywords and 
	 *                        literals
	 *             fb - parent function block
	 *             
	 */ 
	// TODO: Add support for UDTs (inc check if var.element is valid) 
	
	private void tokenizeAlphaNumeric(TokenSet tokenSet, FunctionBlock fb) {
		// Extract all strings
		String[] tokens = condition.split("[&!\\(\\)\\+\\-\\*/<>=\\s]");
		for (int j = 0; j < tokens.length; j++) {
			if (tokens[j].equalsIgnoreCase("NOT") || tokens[j].equalsIgnoreCase("OR") || 
				tokens[j].equalsIgnoreCase("AND") || tokens[j].equalsIgnoreCase("XOR")) {
				// Match tokens with ST string logical operators
				tokenSet.add(new Token(tokens[j], TokenType.LOGICOP));
			}
			else if (tokens[j].equalsIgnoreCase("MOD")) {
				// Match tokens with ST string arithmetic operators
				tokenSet.add(new Token(tokens[j], TokenType.ARITHOP));
			}
			else if (fb.ieNames.containsLocal(tokens[j])) {
				// Match tokens with input events
				setEventType();
				tokenSet.add(new Token(tokens[j], TokenType.EVENT));
			}
			else if (isGuardCondition(tokens[j], fb.idNames) || 
				isGuardCondition(tokens[j], fb.odNames)) {
				// Match tokens with input/output variables
				setDataType();
				tokenSet.add(new Token(tokens[j], TokenType.IFVAR));
			}
			else if (isGuardCondition(tokens[j], fb.varNames)) {
				// Match tokens with internal variables
				setDataType();
				tokenSet.add(new Token(tokens[j], TokenType.INTVAR));
			}
			// TODO: Temp for UDTs
			else if (tokens[j].indexOf(".") >= 0 && (
				isGuardCondition(tokens[j].substring(0,tokens[j].indexOf(".")), fb.idNames) || 
				isGuardCondition(tokens[j].substring(0,tokens[j].indexOf(".")), fb.odNames)
				) ) {
				// Match tokens with input/output variables
				setDataType();
				tokenSet.add(new Token(tokens[j], TokenType.IFVAR));
			}
			// TODO: Temp for UDTs
			else if (tokens[j].indexOf(".") >= 0 &&
					isGuardCondition(tokens[j].substring(0,tokens[j].indexOf(".")), fb.varNames)) {
				// Match tokens with internal variables
				setDataType();
				tokenSet.add(new Token(tokens[j], TokenType.INTVAR));
			}
			else if (isLiteral(tokens[j])) {
				// Match tokens that are numeric literals
				setDataType();
				tokenSet.add(new Token(tokens[j], TokenType.LITERAL));
			}
			// GARETH ADDED FOR Array indexes in vars
			else if ( tokens[j].contains("[") )
			{
				// Try I/O
				String varname = tokens[j].substring(0,tokens[j].indexOf("["));
				if (isGuardCondition(varname, fb.idNames) || 
					isGuardCondition(varname, fb.odNames)) {
					// Match tokens with input/output variables
					setDataType();
					tokenSet.add(new Token(tokens[j], TokenType.IFVAR));
				}
				// Try Internal
				else if (isGuardCondition(varname, fb.varNames)) {
					// Match tokens with internal variables
					setDataType();
					tokenSet.add(new Token(tokens[j], TokenType.INTVAR));
				}
				
				
			}
			else if( tokens[j].matches("[A-Z_]+") )
			{
				// Its a global Constant..
				// Add it anyway
				OutputManager.printNotice(fb.compiledType, tokens[j] + " assumed to be a global constant.",
						OutputLevel.INFO);
				
				setDataType();
				tokenSet.add(new Token(tokens[j], TokenType.LITERAL));
			}
				
		}
	}
	
	/* DESCRIPTION: Tokenize the elements in the transition condition
	 * PARAMETERS: tokenSet - set containing tokens of identifiers, keywords and 
	 *                        literals
	 */ 
	private void tokenize(TokenSet tokenSet) {
		StringBuilder id = new StringBuilder();
		boolean isToken = false;
		char[] cond = condition.toCharArray();
		for (int i = 0; i < cond.length; i++) {
			// Gareth Added Array STUFF
			if (Character.isLetterOrDigit(cond[i]) || cond[i] == '_' || cond[i] == '[' || cond[i] == ']' || cond[i] == '.') { // '.' for floats
				isToken = true;
				id.append(cond[i]);
			}
			else {
				if (isToken) {
					Token tok = tokenSet.getToken(id.toString());
					if (tok == null) {
						OutputManager.printError("", "Token `" + id.toString() + "\' cannot be found.\nIn Condition: " +
								condition, OutputLevel.FATAL);
						System.exit(0);
					}
					guard.add(tok);
					id.delete(0, id.length());
					isToken = false;
				}
				if (cond[i] == '(')
					guard.add(new Token(String.valueOf(cond[i]), TokenType.OPARENTHESIS));
				else if (cond[i] == ')')
					guard.add(new Token(String.valueOf(cond[i]), TokenType.CPARENTHESIS));
				else if (cond[i] == '&' || cond[i] == '!' ) // TODO: multi-char tokens :( || cond[i] == '&&' || cond[i] == '||')
					guard.add(new Token(Character.toString(cond[i]), TokenType.LOGICOP));
				else if (cond[i] == '+' || cond[i] == '-' || cond[i] == '/')
					guard.add(new Token(String.valueOf(cond[i]), TokenType.ARITHOP));
				else if (cond[i] == '*') {
					assert (i < cond.length - 1): "Condition ends with an operator.\n";
					if (cond[i+1] == '*') {
						guard.add(new Token("**", TokenType.ARITHOP));
						i++;
					}
					else
						guard.add(new Token("*", TokenType.ARITHOP));						
				}
				else if (cond[i] == '=')
					guard.add(new Token("=", TokenType.COMPARISON));
				else if (cond[i] == '<') {
					assert (i < cond.length - 1): "Condition ends with an operator.\n";
					if (cond[i+1] == '=') {
						guard.add(new Token("<=", TokenType.COMPARISON));
						i++;
					}
					else if (cond[i+1] == '>') {
						guard.add(new Token("<>", TokenType.COMPARISON));
						i++;
					}
					else
						guard.add(new Token("<", TokenType.COMPARISON));
				}
				else if (cond[i] == '>') {
					assert (i < cond.length - 1): "Condition ends with an operator.\n";
					if (cond[i+1] == '=') {
						guard.add(new Token(">=", TokenType.COMPARISON));
						i++;
					}
					else
						guard.add(new Token(">", TokenType.COMPARISON));
				}
			}
		}
		if (isToken) {
			Token tok = tokenSet.getToken(id.toString());
			if (tok == null) {
				OutputManager.printError("", "Token `" + id.toString() + "\' cannot be found.\nIn Condition: " +
						condition, OutputLevel.FATAL);
				System.exit(0);
			}
			guard.add(tok);
		}
		if( guard.size() == 0 || guard.get(0) == null )
			return;
	}
	
	/* DESCRIPTION: Search "nameDB" for "token"
	 * PARAMETERS: token - the string token to search for
	 *             nameDB - the name database to search from
	 * RETURNS: TRUE if match if found, otherwise FALSE                
	 */ 
	private boolean isGuardCondition(String token, NameDB nameDB) {
		if (nameDB.containsLocal(token))
			return true;
		return false;
	}
	
	// Scrub tokens to remove FBDK's java-type syntax
	String scrubToken(String token) {
		int index = token.indexOf(".value"); 
		if (index > 0)
			return token.substring(0, index);
		return token;
	}
}
