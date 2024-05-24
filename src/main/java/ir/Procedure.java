/* Procedure.java
 * Defines the Procedure object.
 * 
 * Written by: Li Hsien Yoong
 */

package ir;

import java.util.*;
import fb.*;

public class Procedure extends Algorithm {
	private String fullName;
	private ArrayList<VarType> arguments = new ArrayList<VarType>();
	private ArrayList<ParamType> paramTypes = new ArrayList<ParamType>();
	
	public Procedure(Algorithm alg, VarType[] inputs, VarType[] outputs, 
			         VarType[] internals, String fbName) {
		super(alg.getComment(), alg.getName(), alg.getLanguage(), 
			  alg.getPrototype(), alg.getText());
		fullName = fbName + "_" + name;
		
		if (language.equals("ST")) {
			// Search for procedure arguments among input variables
			if (inputs != null && text != null) {
				for (int i = 0; i < inputs.length; i++) {
					if ( text.contains(inputs[i].getName()) )
						arguments.add(inputs[i]);
				}
			}
		
			// Search for procedure arguments among output variables
			if (outputs != null && text != null) {
				for (int i = 0; i < outputs.length; i++) {
					if ( text.contains(outputs[i].getName()) )
						arguments.add(outputs[i]);
				}
			}
		
			// Search for procedure arguments among internal variables
			if (internals != null && text != null) {
				for (int i = 0; i < internals.length; i++) {
					if ( text.contains(internals[i].getName()) )
						arguments.add(internals[i]);
				}
			}
		}
		
		if (prototype != null) {
			int start = prototype.indexOf('(');
			int end = prototype.lastIndexOf(')');
			if (start > -1 && end > -1) {
				start++;
				String argList = prototype.substring(start, end);
				while (true) {
					end = argList.indexOf(',');
					String arg;
					if (end > -1)
						arg = argList.substring(0, end);
					else
						arg = argList;
					arg = arg.trim();
					if (arg.equals("") || arg.equals("void")) {
						paramTypes.add(new ParamType("", false));
						break;
					}

					String[] tokens = arg.split("\\s");
					boolean writable = true;
					if (language.equals("C")) {
						if (!arg.contains("*"))
							writable = false;
						else {
							int index = tokens[0].lastIndexOf('*');
							if (index > -1)
								tokens[0] = tokens[0].substring(0, index);
						}
					}
						
					paramTypes.add(new ParamType(tokens[0], writable));

					if (end > -1) 
						argList = argList.substring(end+1);
					else
						break;
				}
			}
			else
				paramTypes.add(new ParamType("", false));
		}
	}
	
	// Returns an array consisting of all arguments that are output data
	/*
	public VarType[] getOutputArgs() {
		int size = arguments.size();
		int outSize = 0;
		
		for (int i = 0; i < size; i++) {
			if ( arguments.get(i).matchCategory(Category.IFACE_OUT) )
				outSize++;
		}
		
		if (outSize > 0) {
			VarType[] vars = new VarType[outSize];
			for (int i = 0; i < size; i++) {
				if ( arguments.get(i).matchCategory(Category.IFACE_OUT) )
					vars[i] = arguments.get(i);
			}
			return vars;
		}
		return null;
	}
	*/
		
	// Returns an array containing the argument types for this procedure
	String[] getParamTypes() {
		String[] argTypes = null;
		
		if (!paramTypes.isEmpty()) {
			int size = paramTypes.size();
			argTypes = new String[size];
			for (int i = 0; i < size; i++)
				argTypes[i] = paramTypes.get(i).type;
		}
		else {
			argTypes = new String[1];
			argTypes[0] = "";
		}
		
		return argTypes;
	}
	
	public String getFullName() {
		return fullName;
	}
	
	public ArrayList<VarType> getArgumentList() {
		return arguments;
	}
	
	public ArrayList<ParamType> getParamTypeList() {
		return paramTypes;
	}
}
