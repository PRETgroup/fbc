/* Transition.java
 * Represents a transition condition in a Kripke structure
 *
 * Written by: Li Hsien Yoong
 */

package rmc;

import ir.*;

import java.util.*;

public class Transition {
	private static boolean eventless;	// set to true if any transition is eventless
	private StringBuilder guard = new StringBuilder();
	private ArrayList<StringBuilder> data = new ArrayList<StringBuilder>();
	
	public Transition(Condition c) {
		int bracket = 0;
		boolean foundData = false;
		StringBuilder tmpData = null;
		LinkedList<Token> cond = c.getConditionTokens();
		Iterator<Token> i = cond.iterator();
		while (i.hasNext()) {
			Token t = i.next();
			switch (t.getType()) {
				case EVENT:
					guard.append(t.getToken());
					break;
				case IFVAR:
				case INTVAR:
				case LITERAL:
					String op = applyTextOperator(t.getType(), t.getToken());
					if (tmpData == null)
						tmpData = new StringBuilder();
					foundData = true;
					tmpData.append(op);
					guard.append(op);
					break;
				case ARITHOP:
				case COMPARISON:
					assert (tmpData != null): "tmpData == null.\n";
					op = applyTextOperator(t.getType(), t.getToken());
					tmpData.append(op);
					guard.append(op);
					break;
				case LOGICOP:
					if (foundData) {
						data.add(tmpData);
						tmpData = null;
						foundData = false;
					}
					guard.append(applyEntityReference(t.getType(), t.getToken()));
					break;
				case OPARENTHESIS:				
					if (foundData) {
						assert (tmpData != null): "tmpData == null.\n";
						op = applyTextOperator(t.getType(), t.getToken());
						tmpData.append(op);
						bracket++;
					}
					else
						op = t.getToken();
					guard.append(t.getToken());
					break;
				case CPARENTHESIS:
					if (foundData && bracket > 0) {
						assert (tmpData != null): "tmpData == null.\n";
						op = applyTextOperator(t.getType(), t.getToken());
						tmpData.append(op);
						bracket--;
					}
					else
						op = t.getToken();
					guard.append(t.getToken());
					break;
			}
		}
		
		if (tmpData != null)
			data.add(tmpData);			
		if (data.size() == 1) {
			String dat = data.get(0).toString();
			if (dat.equalsIgnoreCase("true") || dat.equals("1")) {
				data.remove(0);
				guard.delete(0, guard.length());
				guard.append(".");
				eventless = true;
			}
		}
	}
	
	public Transition(StringBuilder dataCond, Condition c) {
		int start = 0;
		int end = 0;
		boolean eventful = false;
		boolean dataful = false;
		int state = 0;		// Bit0: EVENT, Bit1: DATA, Bit2: PREFIX, Bit3: SUFFIX
		int bracket = 0;
		LinkedList<Boolean> brackets = new LinkedList<Boolean>();	// false: EVENT, true: DATA 
		StringBuilder prefix = new StringBuilder();
		LinkedList<Token> cond = c.getConditionTokens();
		Iterator<Token> i = cond.iterator();
		while (i.hasNext()) {
			Token t = i.next();
			switch (t.getType()) {
				case EVENT:
					// If some prefix exists
					if ((state & 0x04) != 0) {
						// If a logical operator exists
						if (end != 0) {
							// If this is a data-event boundary and no previous event exist
							if ((state & 0x02) != 0 && !eventful)
								prefix.delete(start, end);	// remove the logical operator
							end = 0;
						}
						guard.append(prefix);
						prefix.delete(0, prefix.length());
						for (int j = 0; j < bracket; j++)
							brackets.addFirst(false);
						bracket = 0;
					}
					guard.append(t.getToken());
					eventful = true;
					state = 0x01;					
					break;
				case IFVAR:
				case INTVAR:
				case LITERAL:
					// If some prefix exists
					if ((state & 0x04) != 0) {
						// If a logical operator exists
						if (end != 0) {
							// If this is a event-data boundary and no previous data exist
							if ((state & 0x01) != 0 && !dataful)
								prefix.delete(start, end);	// remove the logical operator
							end = 0;
						}
						dataCond.append(prefix);
						prefix.delete(0, prefix.length());
						for (int j = 0; j < bracket; j++)
							brackets.addFirst(true);
						bracket = 0;
					}
					dataCond.append(t.getToken());
					dataful = true;
					state = 0x02;
					break;
				case ARITHOP:
				case COMPARISON:
					if ((state & 0x04) != 0) {
						dataCond.append(prefix);
						prefix.delete(0, prefix.length());
					}
					dataCond.append(applyEntityReference(t.getType(), t.getToken()));
					break;
				case LOGICOP:
					start = prefix.length();
					prefix.append(applyEntityReference(t.getType(), t.getToken()));
					end = prefix.length();
					state |= 0x04;
					break;
				case OPARENTHESIS:
					bracket++;
					prefix.append(t.getToken());
					state |= 0x04;
					break;
				case CPARENTHESIS:
					if (brackets.removeFirst())
						dataCond.append(t.getToken());
					else
						guard.append(t.getToken());
					break;
			}
		}
		
		if (!eventful) {
			guard.delete(0, guard.length());
			guard.append(".");
			eventless = true;
		}
	}
	
	/* Replace special characters in XML with entity references */
	private static String applyEntityReference(TokenType type, String token) {
		switch (type) {
			case COMPARISON:
				token = token.replace("<", "&lt;");
				token = token.replace(">", "&gt;");
				break;
			case LOGICOP:
				token = token.replace("&", "&amp;");
				break;
		}
		return token;
	}
	
	/* Replace special tokens with text representation */
	private static String applyTextOperator(TokenType type, String token) {
		switch (type) {
			case LITERAL:
				return token.replace(".", "_PT_");
			case COMPARISON:
				if (token.equals("="))
					return "_EQ_";
				else if (token.equals("<"))
					return "_LT_";
				else if (token.equals("<="))
					return "_LEQ_";
				else if (token.equals("<>"))
					return "_NEQ_";
				else if (token.equals(">"))
					return "_GT_";
				else if (token.equals(">="))
					return "_GEQ_";
				break;
			case ARITHOP:
				if (token.equalsIgnoreCase("MOD"))
					return "_MOD_";
				else if (token.equals("+"))
					return "_PLUS_";
				else if (token.equals("-"))
					return "_MINUS_";
				else if (token.equals("/"))
					return "_DIV_";
				else if (token.equals("*"))
					return "_MUL_";
				else if (token.equals("**"))
					return "_EXPO_";
				break;
			case OPARENTHESIS:
				if (token.equals("("))
					return "_OP_";
				break;
			case CPARENTHESIS:
				if (token.equals(")"))
					return "_CP_";
				break;
		}
		return token;
	}
	
	static boolean isEventless() {
		return eventless;
	}
	
	String getGuard() {
		return guard.toString();
	}
	
	ArrayList<StringBuilder> getDataConditions() {
		return data;
	}
}
