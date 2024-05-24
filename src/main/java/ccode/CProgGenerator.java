/* CProgGenerator.java
 * This class generates a top-level C file containing the main function to glue
 * separate C files obtained from multiple function blocks to form a single
 * executable for a given device.
 *
 * Written by: Li Hsien Yoong
 */

package ccode;

import fb.*;
import fbtostrl.*;
import ir.*;

import java.io.*;
import java.util.*;

import org.jdom2.Element;

public class CProgGenerator {
	protected Device device;
	private HashMap<String, Variables> legacyVars;
	private OutputPath outputPath;
	
	public CProgGenerator(Device dev, HashSet<Block> comBlocks, OutputPath o) {
		device = dev;
		legacyVars = createGlobalVars(comBlocks);
		outputPath = o;
	}
	
	final Device getDevice() {
		return device;
	}
	
	final String outputpath() {
		return outputPath.getPath(); 
	}
	
	final String sysOutputPath() {
		return outputPath.getPathOnly();
	}
	
	public static String getProgramFilePrefix(String insName, String devType) {
		return (insName.isEmpty() ? devType : insName) + "_";
	}
	
	public static String makeDevFileName(String insName, String devType) {
		return insName.isEmpty() ? devType : insName + "_" + devType;
	}
	
	/* DESCRIPTION: Creates a map containing all global variables from the set 
	 *              of local communication blocks
	 * PARAMETERS: comBlocks - set of local communication block
	 * RETURNS: Map containing all global variables
	 */
	public static HashMap<String, Variables> createGlobalVars(HashSet<Block> comBlocks) {
		HashMap<String, Variables> globals = new HashMap<String, Variables>();
		
		Iterator<Block> i = comBlocks.iterator();
		while (i.hasNext()) {
			Block block = i.next();
			String varName = block.getIDname();
			String sifbName = block.getSIFBname();
			if (globals.containsKey(varName)) {
				// Perform consistency checks
				Variables var = globals.get(varName);
				DataType[] data;
				if (sifbName.equals("SEND")) {
					data = block.getPredDataTypes();
					String type = var.getSendType(); 
					if (type.isEmpty()) {
						if (!validateSendRecvPair(sifbName, var.getRecvType())) {
							OutputManager.printError(sifbName + "."+varName, 
									varName + ": attempting to connect type `" + 
									sifbName + "\' with `" + var.getRecvType() + "\'", 
									OutputLevel.FATAL);
							System.exit(0);
						}
						var.setSendType(sifbName);
						var.setSendEvents(block.getInputEvents(), block.getOutputEvents());
					}
					else if (type.equals(sifbName)) {
						if (type.equals("SEND")) {
							OutputManager.printError(sifbName+"."+varName, 
									varName + " of type `" + type + "\' already declared", 
									OutputLevel.FATAL);
							System.exit(0);
						}
					}
					else {
						OutputManager.printError(sifbName+"."+varName, varName + 
								": first defined as type `" + type + "\' but redefined as " + 
								sifbName, OutputLevel.FATAL);
						System.exit(0);
					}
				}
				else {
					data = block.getSuccDataTypes();
					String type = var.getRecvType(); 
					if (type.isEmpty()) {
						if (!validateSendRecvPair(var.getSendType(), sifbName)) {
							OutputManager.printError(sifbName+"."+varName, 
									varName + ": attempting to connect type `" + 
									sifbName + "\' with `" + var.getSendType() + "\'",
									OutputLevel.FATAL);
							System.exit(0);
						}
						var.setRecvType(sifbName);
						var.setRecvEvents(block.getInputEvents(), block.getOutputEvents());
					}
					else if (type.equals(sifbName)) {
						if (type.equals("RECV")) {
							OutputManager.printError(sifbName+"."+varName, 
									varName + " of type `" + type + "\' already declared",
									OutputLevel.FATAL);
							System.exit(0);
						}
					}
					else {
						OutputManager.printError(sifbName+"."+varName, varName + 
								": first defined as type `" + type + "\' but redefined as " + 
								sifbName, OutputLevel.FATAL);
						System.exit(0);
					}
				}
				
				// Ensure that the number of data elements for the send/receive pair match 
				DataType[] foundData = var.getData();
				if (data.length != foundData.length) {
					OutputManager.printError(sifbName+"."+varName, sifbName + 
							": first defined with " + Integer.toString(foundData.length) + 
							" data ports but redefined with " + 
							Integer.toString(data.length), OutputLevel.FATAL);
					System.exit(0);
				}
				
				// Ensure that the types of data elements for the send/receive pair match
				for (int j = 0; j < data.length; j++) {
					if (!data[j].equals(foundData[j])) {
						OutputManager.printError(sifbName  +"." + varName, 
								varName + ": type mismatch for data element " + 
								Integer.toString(j+1) + " - declared as " +  data[j].getType() +
								" and " + foundData[j].getType(), OutputLevel.FATAL);
						System.exit(0);
					}
				}
			}
			else {
				// Add local communication blocks to global variables map
				if (sifbName.equals("SEND")) {
					makeGlobalComVar(globals, block, varName, false, sifbName);
				}
				else if (sifbName.equals("RECV")) {
					makeGlobalComVar(globals, block, varName, true, sifbName);
				}
			}
		}
		
		// Ensure that point-to-point communication blocks are paired
		Set<String> varNames = globals.keySet();
		Iterator<String> j = varNames.iterator();
		while (j.hasNext()) {
			String varName = j.next();
			Variables var = globals.get(varName);
			String recvType = var.getRecvType();
			String sendType = var.getSendType();
			if (recvType.equals("RECV") && sendType.isEmpty()) {
				OutputManager.printError(recvType + "." + varName, 
						"No corresponding SEND for `" + varName + "\' of type RECV", 
						OutputLevel.FATAL);
				System.exit(0);
			}
			else if (sendType.equals("SEND") && recvType.isEmpty()) {
				OutputManager.printError(sendType+"."+varName, "No corresponding RECV for `" + 
						varName + "\' of type SEND", OutputLevel.FATAL);
				System.exit(0);
			}
		}
		
		return globals;
	}
	
	/* DESCRIPTION: Creates a global variable for a local communication block
	 * PARAMETERS: globals - map of all global variables
	 *             block - local communication block
	 *             varName - name of the global variable to be created
	 *             type - type of communication block (false for sending, true for receiving)
	 *             name - type name of the communication block
	 */
	private static void makeGlobalComVar(HashMap<String, Variables> globals, 
			Block block, String varName, boolean type, String name) {
		if (!type) {
			// Send type
			DataType[] data = block.getPredDataTypes();
			if (data != null) {
				Variables var = new Variables(data, block.getSIFBQlen());
				var.setSendType(name);
				var.setSendEvents(block.getInputEvents(), block.getOutputEvents());
				globals.put(varName, var);
			}					
		}
		else {
			// Receive type
			DataType[] data = block.getSuccDataTypes();
			if (data != null) {
				Variables var = new Variables(data, block.getSIFBQlen());
				var.setRecvType(name);
				var.setRecvEvents(block.getInputEvents(), block.getOutputEvents());
				globals.put(varName, var);
			}					
		}
	}
	
	/* DESCRIPTION: Ensures that only compatible local communication blocks are
	 *              ever connected together 
	 * PARAMETERS: sendType - type of the sending local communication block
	 *             recvType - type of the receiving local communication block
	 * RETURNS: TRUE if the pair of communication blocks are compatible, FALSE otherwise
	 */
	private static boolean validateSendRecvPair(String sendType, String recvType) {
		if (sendType == null || recvType == null)
			return true;
		
		if ( recvType.equals("RECV") && sendType.equals("SEND") ) {
			return true;	
		}
		
		return false;
	}
	
	/* DESCRIPTION: Prints out global variables for local communication blocks
	 * PARAMETERS: globals - map containing global variables
	 *             prn - code printer for implementation file
	 *             hprn - code printer for definition file
	 */
	public static void printGlobalVars(HashMap<String, Variables> globals, CodePrinter prn, 
			CodePrinter hprn) throws IOException {
		Set<String> varNames = globals.keySet();
		Iterator<String> j = varNames.iterator();
		while (j.hasNext()) {
			String varName = j.next();
			Variables var = globals.get(varName);
			prn.print("struct {");
			hprn.print("extern struct {");
			prn.indent();
			hprn.indent();
			prn.print("union {");
			hprn.print("union {");
			prn.indent();
			hprn.indent();
			prn.print("UDINT events;");
			hprn.print("UDINT events;");
			prn.print("struct {");
			hprn.print("struct {");
			prn.indent();
			hprn.indent();
			Event[] events = var.getSendIEvents();
			if (events != null) {
				for (int i = 0; i < events.length; i++) {
					String tmp = "UDINT " + var.getSendType() + "_" + 
					             events[i].getName() + " : 1;";
					prn.print(tmp);
					hprn.print(tmp);
				}
			}
			if ((events = var.getRecvIEvents()) != null) {
				for (int i = 0; i < events.length; i++) {
					String tmp = "UDINT " + var.getRecvType() + "_" + 
				                 events[i].getName() + " : 1;";
					prn.print(tmp);
					hprn.print(tmp);
				}
			}
			prn.unindent();
			hprn.unindent();
			prn.print("} event;");
			hprn.print("} event;");
			prn.unindent();
			hprn.unindent();
			prn.print("} _input;");
			hprn.print("} _input;");
			
			prn.print("union {");
			hprn.print("union {");
			prn.indent();
			hprn.indent();
			prn.print("UDINT events;");
			hprn.print("UDINT events;");
			prn.print("struct {");
			hprn.print("struct {");
			prn.indent();
			hprn.indent();
			if ((events = var.getSendOEvents()) != null) {
				for (int i = 0; i < events.length; i++) {
					String tmp = "UDINT " + var.getSendType() + "_" + 
				                 events[i].getName() + " : 1;";
					prn.print(tmp);
					hprn.print(tmp);
				}
			}
			if ((events = var.getRecvOEvents()) != null) {
				for (int i = 0; i < events.length; i++) {
					String tmp = "UDINT " + var.getRecvType() + "_" + 
				                 events[i].getName() + " : 1;";
					prn.print(tmp);
					hprn.print(tmp);
				}
			}
			prn.unindent();
			hprn.unindent();
			prn.print("} event;");
			hprn.print("} event;");
			prn.unindent();
			hprn.unindent();
			prn.print("} _output;");
			hprn.print("} _output;");
			
			StringBuilder line = new StringBuilder();
			String qlen;
			if (var.getQlen() > 0)
				qlen = "[" + Integer.toString(var.getQlen()) + "];";
			else
				qlen = ";";
			DataType[] data = var.getData();
			for (int i = 0; i < data.length; i++) {
				line.delete(0, line.length());
				String datatype = data[i].getType();
				line.append(datatype + " d" + Integer.toString(i+1));
				String size = data[i].getArraySize();
				if (!size.isEmpty())
					line.append("[" + size + "]");
				prn.print(line.toString() + qlen);
				hprn.print(line.toString() + qlen);
				line.insert(datatype.length() + 1, "_");
				prn.print(line.toString() + ";");
				hprn.print(line.toString() + ";");
			}
			if (var.getSendType().equals("SEND")) {
				prn.print("int _wrptr;");
				hprn.print("int _wrptr;");
				prn.print("int _rdptr;");
				hprn.print("int _rdptr;");
				prn.print("int _count;");
				hprn.print("int _count;");
			}
			prn.unindent();
			hprn.unindent();
			String tmp = "} _" + varName + ";";
			prn.print(tmp, 1);
			hprn.print(tmp, 1);
		}
	}
	
	/* DESCRIPTION: Generates code to initialize global variables for local 
	 *              communication blocks
	 * PARAMETERS: globals - map containing global variables
	 *             prn - code printer for implementation file
	 */
	public static void initGlobalVars(HashMap<String, Variables> globals, CodePrinter prn) 
			throws IOException {
		Set<String> varNames = globals.keySet();
		Iterator<String> j = varNames.iterator();
		while (j.hasNext()) {
			String varName = "_" + j.next();
			prn.print(varName + "._input.events = 0;");
			prn.print(varName + "._output.events = 0;");
			if (globals.get(varName.substring(1)).getSendType().equals("SEND")) {
				prn.print(varName + "._wrptr = 0;");
				prn.print(varName + "._rdptr = 0;");
				prn.print(varName + "._count = 0;");
			}
		}
		prn.print("");
	}
	
	/**
	 * Set the specific variable to the given parameter value.
	 * @param paramVars list of all possible input variables
	 * @param param	name-value pair of a parameter
	 * @return A new variable initialized according to the given parameter; null if no 
	 * input variable corresponding to the parameter can be found.
	 */
	protected static VarType setParamValue(VarDeclaration[] paramVars, Parameter param) {
		for (VarDeclaration paramVar : paramVars) {
			if (paramVar.getName().equals(param.name)) {
				VarType var = new VarType(Category.IFACE_IN, paramVar);
				var.setInitial(param.value);
				return var;
			}
		}
		return null;
	}
	
	public void copyPTARMFiles(String fileName) {
		// deadline.h
		String hname = CodeGenerator.getParentName(fileName) + "deadline.h";
		String file = "ptarm/deadline.h";
		BufferedWriter out = null;
		BufferedReader in = null;
		try {
			InputStream fileInputStream = CProgGenerator.class.getResourceAsStream("/"+file);
			if(fileInputStream == null) {
				OutputManager.printError("", file + 
						": Could not be opened.\n", OutputLevel.FATAL);
				System.exit(-1);
			}
			out = new BufferedWriter(new FileWriter(hname));
			//fileNames.add(hname);
			in = new BufferedReader(new InputStreamReader(fileInputStream));
			CodeGenerator.plainCopyFile(out, in);
		} catch (IOException e) {
			OutputManager.printError("", hname + 
					": Could not be opened.\n", OutputLevel.FATAL);
			System.exit(-1);
		}
		
		
		// ptthread.h
		hname = CodeGenerator.getParentName(fileName) + "ptthread.h";
		file = "ptarm/ptthread.h";
		try {
			InputStream fileInputStream = CProgGenerator.class.getResourceAsStream("/"+file);
			if(fileInputStream == null) {
				OutputManager.printError("", file + 
						": Could not be opened.\n", OutputLevel.FATAL);
				System.exit(-1);
			}
			out = new BufferedWriter(new FileWriter(hname));
			//fileNames.add(hname);
			in = new BufferedReader(new InputStreamReader(fileInputStream));
			CodeGenerator.plainCopyFile(out, in);
		} catch (IOException e) {
			OutputManager.printError("", hname + 
					": Could not be opened.\n", OutputLevel.FATAL);
			System.exit(-1);
		}
		
		// ptio.h
		hname = CodeGenerator.getParentName(fileName) + "ptio.h";
		file = "ptarm/ptio.h";
		try {
			InputStream fileInputStream = CProgGenerator.class.getResourceAsStream("/"+file);
			if(fileInputStream == null) {
				OutputManager.printError("", file + 
						": Could not be opened.\n", OutputLevel.FATAL);
				System.exit(-1);
			}
			out = new BufferedWriter(new FileWriter(hname));
			//fileNames.add(hname);
			in = new BufferedReader(new InputStreamReader(fileInputStream));
			CodeGenerator.plainCopyFile(out, in);
		} catch (IOException e) {
			OutputManager.printError("", hname + 
					": Could not be opened.\n", OutputLevel.FATAL);
			System.exit(-1);
		}
	
	}
	
	/* DESCRIPTION: Generates code for a given device
	 * PARAMETERS: fileName - base name of the output file(s) produced
	 */
	public void generateCode(String fileName) throws IOException {
		CodePrinter printer = new CodePrinter(fileName + ".c");
		printer.print("// This file is generated by FBC.", 1);
		
		if (device.type.equals(Device.TYPE_PTARM)) {
			copyPTARMFiles(fileName);
			printer.print("//ptarm files");
			printer.print("#include \"deadline.h\"");
			printer.print("#include \"ptio.h\"");
			printer.print("#include \"ptthread.h\"");
			printer.print("//ptarm global variables");
			printer.print("int __PTARM_INITIALISED = 0;");
			printer.print("//---");
		}
		
		if (device.comment != null)
			printer.print("// " + device.comment, 1);
		
		for (int i = 0; i < device.resources.length; i++) {
			printer.print("#include \"" + device.resources[i].resType.getTargetType() + ".h\"");
		}
		if(NXTControlHMIGenerator.hmiUsed ) {
			printer.print("#include \""+FBtoStrl.currentDeviceName+"_HMIServerConfig.h\"");
		}
		if( NXTControlInterResourceCommGenerator.interRESUsed  ) {
			printer.print("#include \"COMMCHANNELUDPRECV.h\"");
		}
		printer.print("");
		
		// Print a .h file if necessary
		if (!legacyVars.isEmpty()) {
			String def = fileName + ".h";
			CodePrinter hprinter = new CodePrinter(def);
			hprinter.print("// This file is generated by FBC.", 1);
			def = CodeGenerator.makeHeaderMacro(CodeGenerator.getFileName(def));
			hprinter.print("#ifndef " + def);
			hprinter.print("#define " + def, 1);
			if ( FBtoStrl.opts.sleepValue > 0 ) {
				hprinter.print("#include <stdlib.h>");
				hprinter.print("#include <stdio.h>",1);
			}
			printGlobalVars(legacyVars, printer, hprinter);
			hprinter.print("#endif // " + def);
			hprinter.close();
		}
		
		// Extra functions for windows
		/*printer.print("#ifdef _MSC_VER");
		printer.print("#include <windows.h>");
		printer.print("#include <time.h>");
		printer.print("int gettimeofday(struct timeval* tp, void* tzp)"); 
		printer.print("{ ");
		printer.print("    DWORD t;"); 
		printer.print("    t = GetTickCount();"); 
		printer.print("    tp->tv_sec = t / 1000;"); 
		printer.print("    tp->tv_usec = (t % 1000) * 1000; ");
		printer.print("    return 0; // 0 indicates that the call succeeded."); 
		printer.print("}", 1);
		printer.print("void timersub( const struct timeval * tvp, const struct timeval * uvp, struct timeval* vvp) ");
		printer.print("{ ");
		printer.print("    vvp->tv_sec = tvp->tv_sec - uvp->tv_sec;"); 
		printer.print("    vvp->tv_usec = tvp->tv_usec - uvp->tv_usec;"); 
		printer.print("    if( vvp->tv_usec < 0 )"); 
		printer.print("    { ");
		printer.print("       --vvp->tv_sec;"); 
		printer.print("       vvp->tv_usec += 1000000;"); 
		printer.print("    }"); 
		printer.print("}");
		printer.print("#endif // _MSC_VER", 1);*/
		
		if ( FBtoStrl.opts.isNxtControl() && NXTControlHMIGenerator.hmiUsed ) {
			NXTControlHMIGenerator.printHMIInitDeclarations(printer);
		}
		if (device.type.equals(Device.TYPE_PTARM)) {
			for (int i = 0; i < device.resources.length; i++) {
				String resType = device.resources[i].resType.getTargetType();
				String res = device.resources[i].name;
				printer.print(resType + " " + res + ";");
			}
		}
		
		if (FBtoStrl.startUpFile.length() > 0) {
			try {
				File CfileFILE = new File(FBtoStrl.startUpFile);
				InputStream inputStream = new FileInputStream(CfileFILE);
				BufferedReader Cfile = new BufferedReader(new InputStreamReader(inputStream));
				String line = Cfile.readLine();
				for(;;line = Cfile.readLine()) {
					if (line == null) {
						break;
					}
					printer.print(line);
				}
				Cfile.close();
			}catch(IOException e){
				e.printStackTrace();
				System.out.println("Startup file open error:" + e);
			}
		}
		
		printer.print("int main(void)");
		printer.print("{");
		printer.indent();
		
		if (FBtoStrl.startUpFile.length() > 0) {
			printer.print("startup();\n");
		}
		
		int PTARM_HARDWARE_THREADS = 4;
		int currentHWThread = 0;
		
		if (device.type.equals(Device.TYPE_PTARM)) {
			if( device.parameters != null ) {
				for(int p = 0; p < device.parameters.length; p++) {
					if (device.parameters[p].name.equals("HWThreadsNo")) {
						PTARM_HARDWARE_THREADS = Integer.parseInt(device.parameters[p].value);
					}
				}
			}
			printer.print("int threadID;");
			printer.print("GET_THREAD_ID(threadID);");
			printer.print("if (threadID==0) {");
			printer.indent();
		}
		
		initGlobalVars(legacyVars, printer);
		

		for (int i = 0; i < device.resources.length; i++) {
	
			String resType = device.resources[i].resType.getTargetType();
			String res = device.resources[i].name;
			if (device.type.equals(Device.TYPE_PTARM)==false) {
				printer.print(resType + " " + res + ";");
			}
			printer.print(resType + "init(&" + res + ");");
			
			Parameter[] params = device.resources[i].params; 
			if (params != null) {
				VarDeclaration[] paramVars = device.resources[i].resType.
						getInterfaceList().inputVars;
				for (int j = 0; j < params.length; j++) {
					VarType paramVar = setParamValue(paramVars, params[j]);
					if (paramVar == null)
						continue;
					String[] inits = CGenerator.makeInitialization(paramVar, res + ".", 
							(byte)0x02, res);
					for (String init : inits)
						printer.print(init);
				}
			}
		}
		
		if (device.type.equals(Device.TYPE_PTARM)) {
			printer.print("__PTARM_INITIALISED = 1;");
			printer.unindent();
			printer.print("}");
			printer.print("while(__PTARM_INITIALISED==0){}");		
		}
		printer.print("");
		

		
		if ( FBtoStrl.opts.isNxtControl() ) {
			if( NXTControlHMIGenerator.hmiUsed ) {
				// init globals
				printer.print("ProjectName = \""+NXTControlHMIGenerator.ProjectName+"\";");
				printer.print("ProjectState = \"Running\";");
				printer.print("ProjectGuid = \""+NXTControlHMIGenerator.ProjectGuid+"\";");
				String HMIServerPort = "61498";
				if( device.parameters != null ) {
					for(int p = 0; p < device.parameters.length; p++) {
						if( device.parameters[p].name.equals("HMI_ID") ) {
							if( device.parameters[p].value.contains(":") )
								HMIServerPort = device.parameters[p].value.split(":")[1];
							else
								HMIServerPort = device.parameters[p].value;
							break;
						}
					}
				}	
				printer.print("HMIServerPort = "+HMIServerPort+";");				

				NXTControlHMIGenerator.printHMIInits(printer);
				// Init the HMIServer
				printer.print("NxtHMIServerinit();");
				printer.print("");
			}
			if ( NXTControlInterResourceCommGenerator.interRESUsed ) {
				NXTControlInterResourceCommGenerator.printCommInits(printer);
			}
			printer.print("");
		}
		
		if(FBtoStrl.opts.timeAnnotate) {
			printer.print("asm(\"#@PRET_Parse start\");");
			printer.print("asm(\"#@PRET_Thread start PRET_Node_main_ID_0\");");
		}	
		printer.print("for (;;) {");
		printer.indent();
		if (device.type.equals(Device.TYPE_PTARM)) {
			printer.print("unsigned high, low;");
			printer.print("GET_TIME(high, low);");
		}
		for (int i = 0; i < device.resources.length; i++) {
			
			if (device.type.equals(Device.TYPE_PTARM)) {
				printer.print("if (threadID == "+currentHWThread+") {");
				printer.indent();
			}
			
			printer.print(device.resources[i].resType.getTargetType() + "run(&" + 
					device.resources[i].name + ");");
			
			if (device.type.equals(Device.TYPE_PTARM)) {
				if (currentHWThread < PTARM_HARDWARE_THREADS-1) {
					currentHWThread++;
				}
				printer.print("}");
				printer.unindent();
			}
			
		}
			
		// If a HMI block was used
		if (NXTControlHMIGenerator.hmiUsed) {
			// Run the HMIServer
			printer.print("NxtHMIServerrun();");
		}
		if ( FBtoStrl.opts.sleepValue > 0 ) {
			/*printer.print("#ifdef _MSC_VER");
			printer.print("Sleep("+FBtoStrl.opts.sleepValue+");");
			printer.print("#else ");*/
			printer.print("sleep("+FBtoStrl.opts.sleepValue+");");
			//printer.print("#endif");
		}
		if (FBtoStrl.opts.timeAnnotate) {
			printer.print("asm(\"#@PRET_EOT start\");");
			printer.print("asm(\"#@PRET_EOT end\");");
		}
		if (device.type.equals(Device.TYPE_PTARM)) {
			printer.print("DELAY_UNTIL_OFFSET_NS(1000000000,high,low);");
			
		}
		printer.unindent();
		printer.print("}", 1);
		
		printer.print("return 0;");
		printer.unindent();
		printer.print("}");
		printer.close();
	}
	
	/**
	 * Handles the last rites for the code generation of a stand-alone resource or 
	 * function block. This method creates the global variable files and the Makefile for
	 * the given compilation unit.
	 * @param opts options for the code generation
	 * @param topFB path to the given compilation unit
	 * @param target instance name of the compilation unit
	 * @param srcDependencies set of additional source dependencies that need to be
	 *        considered for Makefile generation
	 * @param objDependencies object dependencies to be included in the Makefile
	 * @param includePaths include paths to be included in the Makefile
	 * @param globalVars list of all global variables to be generated
	 * @throws IOException
	 */
	public void handleLastRites(Options opts, String topFB, String target,
			HashSet<String> srcDependencies, HashSet<String> objDependencies,
			HashSet<String> includePaths, HashMap<String, GlobalVar> globalVars) 
			throws IOException {
		createGlobalVarsFile(opts.outputpath(), srcDependencies, globalVars, 
				FBtoStrl.currentDeviceName);
		if ( !opts.isNoMake() )
			createMakefile(opts, topFB, target, srcDependencies, objDependencies, includePaths);
	}

	/**
	 * Handles the last rites for the code generation of a device. This method creates the 
	 * global variable files for a device and the Makefile for that device.
	 * @param outputPath path to generate code to
	 * @param opts options for the code generation
	 * @param devType reference to the device for code generation
	 * @param insName instance name of the device
	 * @param srcDependencies set of additional source dependencies that need to be
	 *        considered for Makefile generation
	 * @param objDependencies object dependencies to be included in the Makefile
	 * @param includePaths include paths to be included in the Makefile
	 * @param globalVars list of all global variables to be generated
	 * @throws IOException
	 */
	public void handleLastRites(String outputPath, Options opts, Device devType, 
			String insName, HashSet<String> srcDependencies, HashSet<String> objDependencies,
			HashSet<String> includePaths, HashMap<String, GlobalVar> globalVars)
			throws IOException {
		createGlobalVarsFile(outputPath, srcDependencies, globalVars, insName);
		if ( !opts.isNoMake() )
			createMakefile(outputPath, opts, devType, insName, srcDependencies, 
					objDependencies, includePaths);
	}
	
	public static String getGlobalVarsFileName(String deviceName) {
		return deviceName + "globalvars."; 
	}
	
	/**
	 * Generates .h and .c files containing all the global variables used in a program.
	 * @param outputPath path to place the generated file
	 * @param srcDependencies set of additional source dependencies that need to 
	 *        be considered for makefile generation
	 * @param globalVars list of all global variables to be generated
	 * @param devName device name
	 * @throws IOException
	 */
	protected void createGlobalVarsFile(String outputPath, HashSet<String> srcDependencies, 
			HashMap<String, GlobalVar> globalVars, String devName) throws IOException {
		Set<String> varNames = globalVars.keySet();
		if (varNames.isEmpty())
			return;
		
		String file = getGlobalVarsFileName(devName);
		String path = outputPath + file;
		CodePrinter hprint = new CodePrinter(path + "h");
		hprint.print("// This file is generated by FBC.", 1);
		String macroHeader = CodeGenerator.makeHeaderMacro(file + "h");
		hprint.print("#ifndef " + macroHeader);
		hprint.print("#define " + macroHeader, 1);
		hprint.print("#include \"fbtypes.h\"");
		
		CodePrinter cprint = new CodePrinter(path + "c");
		srcDependencies.add(file + "c");
		cprint.print("// This file is generated by FBC.", 1);
		cprint.print("#include \"" + file + "h\"");
		cprint.print("#include \"fbtypes.h\"", 1);
		
		for (String varName : varNames) {
			GlobalVar global = globalVars.get(varName);
			if (!global.isDirectAccess()) {
				hprint.print("enum {_PRESENT = 1, _NEW = 2, _ABSENTNEW = 2, _PRESENTNEW = 3};", 1);
				break;
			}
		}
		
		HashSet<String> printedTypes = new HashSet<String>();
		for (String varName : varNames) {
			GlobalVar global = globalVars.get(varName);
			printPointerAccessGlobalVars(global, hprint, cprint, varName, printedTypes);				
		}
		
		hprint.println();
		hprint.print("#endif // " + macroHeader);		
		hprint.close();
		cprint.close();
	}
	
	/**
	 * Prints global variables that are accessed using pointers.
	 * @param global information about the global variables
	 * @param hprint code printer for the .h file
	 * @param cprint code printer for the .c file
	 * @param varName name of the global variable
	 * @throws IOException
	 */
	private static void printPointerAccessGlobalVars(GlobalVar global, 
		CodePrinter hprint, CodePrinter cprint, String varName, 
		HashSet<String> printedTypes) throws IOException {
		VarDeclaration status = global.getStatus();
		String array = CodeGenerator.getArrayIndex(status.getArraySize(), "", 
				status.getName()) + ";";
		
		String globalType = global.getTypeName();
		if (hprint != null) {
			if ( !printedTypes.contains(globalType) ) {
				printedTypes.add(globalType);
				hprint.print("typedef struct {");
				hprint.indent();
				hprint.print(status.getType() + " " + status.getName() + array);
				ArrayList<ArrayList<VarDeclaration>> members = global.makeMemberSets();
				if (members.size() > 0) {
					hprint.print("struct B" + varName + " {");
					hprint.indent();
					for (ArrayList<VarDeclaration> vars : members) {
						if (vars.size() == 1) {
							VarDeclaration var = vars.get(0);
							array = CodeGenerator.getArrayIndex(var.getArraySize(), "", 
									var.getName()) + ";";
							hprint.print(var.getType() + " " + var.getName() + array);
						}
						else {
							String dataName = vars.get(0).getName();
							hprint.smartPrint("union U" + dataName + " {");
							int size = vars.size();
							for (int i = 0; i < size; i++) {
								VarDeclaration var = vars.get(i);
								array = CodeGenerator.getArrayIndex(var.getArraySize(), "", 
										var.getName()) + ";";
								hprint.print(var.getType() + " d" + i + array);
							}
							hprint.smartPrint("} " + dataName + ";");
						}
					}
					hprint.unindent();
					hprint.print("} buffer;");
				}
				hprint.unindent();
				hprint.print("} " + globalType + ";");
			}
			hprint.print("extern " + globalType + " " + varName + ";", 1);
		}
	
		if (cprint != null) {
			cprint.print(globalType + " " + varName + ";");
		}
	}

	/**
	* Generates the top-level makefile for a system. Calls makefile of individual devices.
	* 
	* @param opts
	*            compiler options
	* @param devices
	*            list of devices whose makefiles are to be invoked from the
	*            top-level make file
	*/
	public static void createMakefile(Options opts, List<?> devices) throws IOException {
		File makefile = new File(opts.sysOutputPath() + "Makefile");
		BufferedWriter outBuf = null;
		try {
			outBuf = new BufferedWriter(new FileWriter(makefile));
		} catch (IOException e) {
			OutputManager.printError("", makefile.getPath() + ": Could not be opened.\n",
					OutputLevel.FATAL);
			System.exit(-1);
		}
		
		outBuf.write("CC = gcc\n");
		if (opts.altSim == true) {
			outBuf.write("CFLAGS = -Wall -O2 -D___ALTSIM___\n\n");
		}else{
			outBuf.write("CFLAGS = -Wall -O2\n\n");
		}
		outBuf.write(".PHONY: all clean cleanall\n\n");
		
		outBuf.write("all:\n");
		// make sub-makes
		Iterator<?> i = devices.iterator();
		while (i.hasNext()) {
			Element dev = (Element) i.next();
			outBuf.write("\t$(MAKE) -C " + dev.getAttributeValue("Name") + "\n");
		}
		outBuf.write("\n");
		
		outBuf.write("clean:\n");
		// clean sub-makes
		i = devices.iterator();
		while (i.hasNext()) {
			Element dev = (Element) i.next();
			outBuf.write("\t$(MAKE) -C " + dev.getAttributeValue("Name") + " clean\n");
		}
		outBuf.write("\n");
		
		outBuf.write("cleanall:\n");
		// cleanall sub-makes
		i = devices.iterator();
		while (i.hasNext()) {
			Element dev = (Element) i.next();
			outBuf.write("\t$(MAKE) -C " + dev.getAttributeValue("Name") + " cleanall\n");
		}
		outBuf.close();
		
		/* Suppress Makefile generation for nmake
		if (!FBtoStrl.pretCUsed) // opts.isNxtControl() )
		{
			// Make an nmake file too
			File nmakefile = new File(opts.outputPath + File.separatorChar
					+ "Makefile.nmake");
			BufferedWriter noutBuf = new BufferedWriter(new FileWriter(
					nmakefile));
		
			noutBuf.write("all:\n");
			// make sub-makes
			i = devices.iterator();
			while (i.hasNext()) {
				Element dev = (Element) i.next();
				noutBuf.write("\tnmake -f Makefile.nmake." + dev.getAttributeValue("Name") + 
						"\n");
			}
			noutBuf.write("\n");
		
			noutBuf.write("clean:\n");
			// clean sub-makes
			i = devices.iterator();
			while (i.hasNext()) {
				Element dev = (Element) i.next();
				noutBuf.write("\tnmake -f Makefile.nmake." + dev.getAttributeValue("Name") + 
						" clean\n");
			}
			noutBuf.write("\n");
			
			noutBuf.write("cleanall:\n");
			// cleanall sub-makes
			i = devices.iterator();
			while (i.hasNext()) {
				Element dev = (Element) i.next();
				noutBuf.write("\tnmake -f Makefile.nmake." + dev.getAttributeValue("Name") + 
						" cleanall\n");
			}
			
			noutBuf.close();
		}
		*/
	}

	/*
	private static void copyBuildScript() throws IOException {
		File infile;
		//if (opts.isSimul()) 
		//	infile = new File("resources" + File.separator + "vsbuild.bat");
		//else
			infile = new File("resources" + File.separator + "gccbuild.bat");
		File outfile = new File(FBtoStrl.opts.outputPath + File.separator
				+ "build.bat");
	
		InputStream in = new FileInputStream(infile);
	
		OutputStream out = new FileOutputStream(outfile);
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}
	*/

	/**
	 * Generates the makefile for the given device.
	 * @param outputPath path to generate code to
	 * @param opts compiler options
	 * @param devType type of the device
	 * @param insName instance name of the device
	 * @param srcDependencies source dependencies to be included in the makefile
	 * @param objDependencies object dependencies to be included in the makefile
	 * @param includePaths include paths to be included in the makefile
	 */
	private static void createMakefile(String outputPath, Options opts, Device devType, 
			String insName, HashSet<String> srcDependencies, HashSet<String> objDependencies,
			HashSet<String> includePaths) throws IOException {
		String target, prefix;
		if (insName.isEmpty()) {
			target = devType.type;
			prefix = "";
		}
		else {
			target = insName;
			prefix = insName + ".";
		}
		String source = makeDevFileName(insName, devType.type) + ".c";

		File makefile = new File(outputPath + "Makefile");
		BufferedWriter outBuf = null;
		try {
			outBuf = new BufferedWriter(new FileWriter(makefile));
		} catch (IOException e) {
			OutputManager.printError("", makefile.getPath() + ": Could not be opened.\n", 
					OutputLevel.FATAL);
			System.exit(-1);
		}
		
		String targetDependencies, objectDependencies, libDependencies;
		if (opts.isGNUuCOS()) {
			outBuf.write("## BEGIN uCOS SPECIFIC VARIABLES - THESE MAY REQUIRE CUSTOMIZATIONS. \n\n");
			outBuf.write("## The following definition must be set for uCOS >= V2.8x. ##########\n");
			outBuf.write("## It must not be set for older versions. ###########################\n");
			outBuf.write("#UCOS_FLAGS = -DNO_TYPEDEF_OS_FLAGS\n\n");
			outBuf.write("## Flag for uCOS-II WIN32 ###########################################\n");
			outBuf.write("UCOS_PORT_FLAG = -D__WIN32__\n\n");
			outBuf.write("## Path for uCOS-II core source files ###############################\n");
			outBuf.write("UCOS_SRC = ../../../../../../source\n\n");
			outBuf.write("## Path for uCOS-II WIN32 port source files #########################\n");
			outBuf.write("UCOS_PORT_SRC = ../../src\n\n");
			outBuf.write("## Path for uCOS-II WIN32 example source files ######################\n");
			outBuf.write("UCOS_PORT_EX = ./\n\n");
			outBuf.write("## Path for uCOS-II WIN32 port library file #########################\n");
			outBuf.write("UCOS_PORT_LIB = $(UCOS_PORT_EX)\n\n");
			outBuf.write("## END uCOS SPECIFIC VARIABLES ######################################\n\n");
			targetDependencies = "os_cfg.h includes.h $(UCOS_PORT_LIB)/ucos_ii.a";
			objectDependencies = "$(UCOS_PORT_FLAG) $(UCOS_FLAGS) -I$(UCOS_SRC) -I$(UCOS_PORT_SRC) -I$(UCOS_PORT_EX) ";
			libDependencies = "$(UCOS_PORT_LIB)/ucos_ii.a /usr/lib/w32api/libwinmm.a /usr/lib/mingw/libcoldname.a";
		}
		else {
			targetDependencies = "";
			objectDependencies = "";
			libDependencies = "";
		}
		
		outBuf.write("CC = gcc\n");
		
		String CustomCFlags = "", CustomIncludes = "", CustomLDFlags = "", CustomLibs = "", CustomLibsSrc = "";
		if (FBtoStrl.makeConfig.get("CFLAGS") != null) {
			CustomCFlags = (String) FBtoStrl.makeConfig.get("CFLAGS");
			
		}
		if (FBtoStrl.makeConfig.get("INCLUDES") != null) {
			CustomIncludes = (String) FBtoStrl.makeConfig.get("INCLUDES");
			
		}
		if (FBtoStrl.makeConfig.get("LDFLAGS") != null) {
			CustomLDFlags = (String) FBtoStrl.makeConfig.get("LDFLAGS");
			
		}
		if (FBtoStrl.makeConfig.get("LIBS") != null) {
			CustomLibs = (String) FBtoStrl.makeConfig.get("LIBS");
			
		}
		if (FBtoStrl.makeConfig.get("LIBRARY_SOURCES") != null) {
			CustomLibsSrc = (String) FBtoStrl.makeConfig.get("LIBRARY_SOURCES");
			
		}
		
		if (opts.altSim == true) {
			outBuf.write("CFLAGS = -Wall -O2 -D___ALTSIM___ "+CustomCFlags+"\n\n");
		}else{
			outBuf.write("CFLAGS = -Wall -O2 "+CustomCFlags+"\n\n");
		}
		outBuf.write("INCLUDES = ");
		for (String inc : includePaths) {
			outBuf.write("-I" + CodeGenerator.escapeFilePaths(inc) + " ");
		}
		outBuf.write(" "+CustomIncludes+" \n");
		
		outBuf.write("LDFLAGS = "+CustomLDFlags+" \n");
		outBuf.write("LIBS = ");
		
		if( CGenerator.STFunctionsUsed )
			outBuf.write("-lm");
		outBuf.write(" "+CustomLibs+" \n");
		
		outBuf.write("LIBRARY_SOURCES =");
		for (int i = 0; i < FBtoStrl.libraryFiles.size(); i++) {
			outBuf.write(" "+FBtoStrl.libraryFiles.get(i));
		}
		outBuf.write(" "+CustomLibsSrc+" \n\n");
		
		outBuf.write("TARGET = " + target + "\n\n");
		outBuf.write("C_SOURCES = "+source);
		if (CGenerator.STFunctionsUsed)
			outBuf.write(" \\\n            STFunctions.c");
		for (String s : srcDependencies) {
			if (s.lastIndexOf('.') <= 0)
				continue;
			outBuf.write(" \\\n            " + CodeGenerator.escapeFilePaths(s));
		}
		if (NXTControlInterResourceCommGenerator.interRESUsed
				&& !srcDependencies.contains("pubsub.c"))
			outBuf.write(" \\\n            pubsub.c");

		LinkedList<String> cSources = new LinkedList<String>();
		for (Resource res : devType.resources) {
			outBuf.write(" \\\n            " + res.resType.getTargetType() + ".c");
			printCFileNames(res.resType, prefix + res.name + ".", outBuf, cSources);
		}

		if (opts.isNxtControl()) {
			// If an HMI block was used
			if (NXTControlHMIGenerator.hmiUsed) {
				for (String cFile : NXTControlHMIGenerator.extraCFiles)
					outBuf.write(" \\\n            " + cFile);
			}
			if (NXTControlInterResourceCommGenerator.interRESUsed) {
				for (String cFile : NXTControlInterResourceCommGenerator.extraCFiles)
					outBuf.write(" \\\n            " + cFile);
			}
		}

		if (opts.isSimul()) {
			for (String cFile : SimHelpers.extraCFiles)
				outBuf.write(" \\\n            " + cFile);
		}
		outBuf.write("\n");
		
		StringBuilder objects = new StringBuilder();
		for (String s : objDependencies)
			objects.append(s + " ");
		outBuf.write("OBJS = $(C_SOURCES:.c=.o)\n");
		outBuf.write("RM = rm -f\n\n");
		outBuf.write(".PHONY: all clean cleanall\n\n");
		outBuf.write("all: $(TARGET)\n\n");
		outBuf.write("$(TARGET): $(OBJS) Makefile " + targetDependencies + "\n");
		outBuf.write("\t$(CC) $(OBJS) " + objects.toString() + "$(LDFLAGS) $(LIBS) " + libDependencies + "-o $(TARGET)\n\n");
		outBuf.write("%.o: %.c Makefile\n");
		outBuf.write("\t$(CC) $(CFLAGS) $(INCLUDES) $" + objectDependencies + "-c $< -o $@\n\n");
		if (opts.isGNUuCOS()) {
			outBuf.write("$(UCOS_PORT_LIB)/ucos_ii.a: os_cfg.h $(UCOS_PORT_SRC)/os_cpu_c.c $(UCOS_PORT_SRC)/os_cpu.h $(UCOS_PORT_SRC)/pc.c $(UCOS_PORT_SRC)/pc.h Makefile\n");
			outBuf.write("\t$(CC) $(CFLAGS) $(LIBRARY_SOURCES)" + objectDependencies + "-c $(UCOS_SRC)/ucos_ii.c $(UCOS_PORT_SRC)/pc.c $(UCOS_PORT_SRC)/os_cpu_c.c\n");
			outBuf.write("\tar -r $(UCOS_PORT_LIB)/ucos_ii.a ucos_ii.o os_cpu_c.o pc.o\n\n");
		}
		outBuf.write("clean:\n");
		outBuf.write("\t$(RM) *.o\n\n");
		outBuf.write("cleanall:\n");
		outBuf.write("\t$(RM) *.o\n");
		outBuf.write("\t$(RM) *.a\n");
		outBuf.write("\t$(RM) $(TARGET)\n");
		outBuf.write("\t$(RM) $(TARGET).exe\n");

		outBuf.close();

		/* Suppress Makefile generation for nmake
		if (!opts.isGNUuCOS()) // opts.isNxtControl() )
		{
			// Make an nmake file too
			File nmakefile = new File(opts.outputPath + File.separatorChar
					+ "Makefile.nmake" + ext);
			BufferedWriter noutBuf = new BufferedWriter(new FileWriter(
					nmakefile));
			noutBuf.write("TARGET = " + target + ".exe\n");
			//noutBuf.write("CFLAGS = /Zi /nologo /W3 /WX- /O2 /D \"WIN32\" /D \"_CONSOLE\" -D \"_CRT_SECURE_NO_WARNINGS\"\n");
			noutBuf.write("CFLAGS = /Zi /nologo /W3 /WX- /Od /D \"WIN32\" /D \"_CONSOLE\" -D \"_CRT_SECURE_NO_WARNINGS\" /D \"_DEBUG\" /Gm /RTC1 /MDd \n");
			noutBuf.write("INCLUDES = \n");
			//noutBuf.write("LDFLAGS = /Out:\"$(TARGET)\" /INCREMENTAL /NOLOGO  /DEBUG  /SUBSYSTEM:CONSOLE /TLBID:1 /DYNAMICBASE /NXCOMPAT /MACHINE:X86\n");
			noutBuf.write("LDFLAGS = /Out:\"$(TARGET)\" /INCREMENTAL /NOLOGO  /DEBUG  /SUBSYSTEM:CONSOLE /TLBID:1 /DYNAMICBASE /NXCOMPAT /MACHINE:X86 /ALLOWISOLATION\n");

			noutBuf.write("\n");
			cSources = new LinkedList<String>();
			String devCfile = source;
			cSources.add(devCfile);
			noutBuf.write("C_SOURCES = " + devCfile);	// + "$(wildcard *.c)");
			if (CGenerator.STFunctionsUsed) {
				noutBuf.write(" \\\n            STFunctions.c");
				cSources.add("STFunctions.c");
			}

			// Don't add pub & sub if not used...
			for (String s : srcDependencies) {
				if (s.lastIndexOf('.') <= 0)
					continue;
				String formatted = formatMakefileSrcPath(s);
				noutBuf.write(" \\\n            " + formatted);
				cSources.add(formatted);
			}
			if (NXTControlInterResourceCommGenerator.interRESUsed
					&& !srcDependencies.contains("pubsub.c")) {
				noutBuf.write(" \\\n            pubsub.c");
				cSources.add("pubsub.c");
			}

			for (Resource res : devType.resources) {
				String resCfile = res.resType.getTargetType() + ".c";
				cSources.add(resCfile);
				noutBuf.write(" \\\n            " + resCfile);
				printCFileNames(res.resType, prefix + res.name + ".", noutBuf, cSources);
			}
			if (opts.isNxtControl()) { // if an HMI block was used
				if (NXTControlHMIGenerator.hmiUsed) {
					for (String cFile : NXTControlHMIGenerator.extraCFiles) {
						noutBuf.write(" \\\n            " + cFile);
						cSources.add(cFile);
					}
				}
				if (NXTControlInterResourceCommGenerator.interRESUsed) {
					for (String cFile : NXTControlInterResourceCommGenerator.extraCFiles) {
						noutBuf.write(" \\\n            " + cFile);
						cSources.add(cFile);
					}
				}
			}
			if (opts.isSimul()) {
				for (String cFile : SimHelpers.extraCFiles) {
					noutBuf.write(" \\\n            " + cFile);
					cSources.add(cFile);
				}
			}
			noutBuf.write("\n");

			noutBuf.write("OBJS = $(C_SOURCES:.c=.obj)\n");
			noutBuf.write("LIBS = \"WSock32.Lib\" \"WinMM.Lib\" \"Ws2_32.lib\" \"Kernel32.lib\"\n");
			noutBuf.write("RM = del /F /Q\n\n");
			noutBuf.write("all: $(TARGET)\n\n");
			noutBuf.write("$(TARGET): $(OBJS) Makefile.nmake" + ext + " " + targetDependencies + "\n");
			noutBuf.write("\tlink $(LIBS) $(OBJS) " + objects.toString() + "$(LDFLAGS)\n\n");

			// damn nmake:
			for (String cfile : cSources) {
				int index = cfile.lastIndexOf('.');
				if (index <= 0)
					continue;
				noutBuf.write(cfile.substring(0, index) + ".obj: " + cfile + "\n");
				noutBuf.write("\t$(CC) $(CFLAGS) $(INCLUDES) /c " + cfile + "\n\n");
			}

			noutBuf.write("clean:\n");
			noutBuf.write("\t$(RM) $(OBJS)\n\n");
			noutBuf.write("cleanall:\n");
			noutBuf.write("\t$(RM) $(OBJS)\n");
			noutBuf.write("\t$(RM) $(TARGET)\n");
			
			noutBuf.close();
		}
		*/

		if (NXTControlInterResourceCommGenerator.interRESUsed)
			NXTControlInterResourceCommGenerator.clearExtraCFiles();
	}

	/**
	 * Generates a makefile when no device is present. Used typically for
	 * testing/simulating a standalone resource or function block.
	 * 
	 * @param opts compiler options
	 * @param topFB top level function block or resource
	 * @param target name of the executable to generate
	 * @param srcDependencies source dependencies to be included in the makefile
	 * @param objDependencies object dependencies to be included in the makefile
	 * @param includePaths include paths to be included in the makefile
	 */
	private static void createMakefile(Options opts, String topFB, String target,
			HashSet<String> srcDependencies, HashSet<String> objDependencies, 
			HashSet<String> includePaths) throws IOException {
		File makefile = new File(opts.outputpath() + "Makefile");
		BufferedWriter outBuf = new BufferedWriter(new FileWriter(makefile));
		if (opts.pretc) {
			outBuf.write("PRETCC = $(PRETC)\\PRETCC.bat\n");
			outBuf.write("PRETCFLAGS = -skipVC ");
			if (opts.sleepValue > 0) {
				outBuf.write("-tickDelay "+opts.sleepValue);
			}
			outBuf.write("\n");		
		}
		outBuf.write("CC = gcc\n");
		String CustomCFlags = "", CustomIncludes = "", CustomLDFlags = "", CustomLibs = "", CustomLibsSrc = "";
		if (FBtoStrl.makeConfig.get("CFLAGS") != null) {
			CustomCFlags = (String) FBtoStrl.makeConfig.get("CFLAGS");
			
		}
		if (FBtoStrl.makeConfig.get("INCLUDES") != null) {
			CustomIncludes = (String) FBtoStrl.makeConfig.get("INCLUDES");
			
		}
		if (FBtoStrl.makeConfig.get("LDFLAGS") != null) {
			CustomLDFlags = (String) FBtoStrl.makeConfig.get("LDFLAGS");
			
		}
		if (FBtoStrl.makeConfig.get("LIBS") != null) {
			CustomLibs = (String) FBtoStrl.makeConfig.get("LIBS");
			
		}
		if (FBtoStrl.makeConfig.get("LIBRARY_SOURCES") != null) {
			CustomLibsSrc = (String) FBtoStrl.makeConfig.get("LIBRARY_SOURCES");
			
		}
		
		if (opts.pretc) {
			outBuf.write("CFLAGS =  -O0 "+CustomCFlags+"\n"); //pretc optimizations off
		}else{
			if (opts.altSim == true) {
				outBuf.write("CFLAGS = -Wall -O2 -D___ALTSIM___ "+CustomCFlags+"\n\n");
			}else{
				outBuf.write("CFLAGS = -Wall -O2 "+CustomCFlags+"\n\n");
			}
		}
		
		outBuf.write("INCLUDES = ");
		for (String inc : includePaths) {
			outBuf.write("-I" + CodeGenerator.escapeFilePaths(inc) + " ");
		}
		outBuf.write(" "+CustomIncludes+" \n");
		
		outBuf.write("LDFLAGS = "+CustomLDFlags+" \n");
		outBuf.write("LIBS = ");
		if( CGenerator.STFunctionsUsed )
			outBuf.write("-lm");
		outBuf.write(" "+CustomLibs+" \n");
		outBuf.write("LIBRARY_SOURCES = ");
		for (int i = 0; i < FBtoStrl.libraryFiles.size(); i++) {
			outBuf.write(" "+FBtoStrl.libraryFiles.get(i));
		}
		outBuf.write(" "+CustomLibsSrc+" \n\n");

		outBuf.write("TARGET = " + target + "\n");
		if (opts.pretc) {
			outBuf.write("PRETC_SOURCES = "+FBtoStrl.inputFileName+".pretc.c\n");
			outBuf.write("C_SOURCES = ");
			outBuf.write(" \\\n            " + "$(TARGET).main.c");
		}else{
			outBuf.write("C_SOURCES = ");
			if (opts.isRunnable())
				outBuf.write(" \\\n            " + topFB + "run.c ");
			else if (opts.isSimul())
				outBuf.write(" \\\n            " + topFB + "sim.c ");
		}


		if (CGenerator.STFunctionsUsed)
			outBuf.write(" \\\n            STFunctions.c ");
		for (String s : srcDependencies) {
			if (s.lastIndexOf('.') <= 0)
				continue;
			outBuf.write(" \\\n            " + CodeGenerator.escapeFilePaths(s));
		}
		if (NXTControlInterResourceCommGenerator.interRESUsed
				&& !srcDependencies.contains("pubsub.c"))
			outBuf.write(" \\\n            pubsub.c");
		if (opts.pretc == false) {
			for (FunctionBlock fb : FunctionBlock.fbs)
				outBuf.write(" \\\n            " + fb.getCompiledType() + ".c ");
		}
		// If an HMI block was used
		if (NXTControlHMIGenerator.hmiUsed) {
			for (String cFile : NXTControlHMIGenerator.extraCFiles)
				outBuf.write(" \\\n            " + cFile);
		}
		if (opts.isSimul()) {
			for (String cFile : SimHelpers.extraCFiles)
				outBuf.write(" \\\n            " + cFile);
		}
		outBuf.write("\n");
		
		
		StringBuilder objects = new StringBuilder();
		for (String s : objDependencies)
			objects.append(s + " ");
		// outBuf.write("OBJS\t\t\t= $(C_SOURCES:.c=.o)\n");
		outBuf.write("RM = rm -f\n\n");
		outBuf.write(".PHONY: all clean cleanall\n\n");
		outBuf.write("all: $(TARGET)\n\n");
		if (opts.pretc) {
			outBuf.write("$(TARGET): $(PRETC_SOURCES) Makefile\n");
			outBuf.write("\t$(PRETCC) $(PRETCFLAGS) -l $(PRETC_SOURCES) -p $(TARGET)\n");
		}else{
			outBuf.write("$(TARGET): $(C_SOURCES) Makefile\n");
		}
		outBuf.write("\t$(CC) $(CFLAGS) $(INCLUDES) $(C_SOURCES) $(LIBRARY_SOURCES) $(LDFLAGS) " + objects.toString());
		if (opts.isSimul()) {
			for (String libFile : SimHelpers.extraCLibs)
				outBuf.write(libFile + " ");
		}
		outBuf.write("$(LIBS) -o $(TARGET)\n\n");
		outBuf.write("clean:\n");
		outBuf.write("\t$(RM) *.o\n\n");
		outBuf.write("cleanall:\n");
		outBuf.write("\t$(RM) *.o\n");
		outBuf.write("\t$(RM) $(TARGET)\n");
		outBuf.write("\t$(RM) $(TARGET).exe\n");

		outBuf.close();

		/* Suppress Makefile generation for nmake
		if (true) // opts.isNxtControl() )
		{
			// Make an nmake file too
			File nmakefile = new File(opts.outputPath + File.separatorChar
					+ "Makefile.nmake");
			BufferedWriter noutBuf = new BufferedWriter(new FileWriter(
					nmakefile));
			noutBuf.write("TARGET = " + target + ".exe\n");
			//noutBuf.write("CFLAGS = /Zi /nologo /W3 /WX- /O2 /D \"WIN32\" /D \"_CONSOLE\" -D \"_CRT_SECURE_NO_WARNINGS\"\n");
			noutBuf.write("CFLAGS = /Zi /nologo /W3 /WX- /Od /D \"WIN32\" /D \"_CONSOLE\" -D \"_CRT_SECURE_NO_WARNINGS\" /D \"_DEBUG\" /Gm /RTC1 /MDd \n");
			noutBuf.write("INCLUDES = \n");
			//noutBuf.write("LDFLAGS = /Out:\"$(TARGET)\" /INCREMENTAL /NOLOGO  /DEBUG  /SUBSYSTEM:CONSOLE /TLBID:1 /DYNAMICBASE /NXCOMPAT /MACHINE:X86\n");
			noutBuf.write("LDFLAGS = /Out:\"$(TARGET)\" /INCREMENTAL /NOLOGO  /DEBUG  /SUBSYSTEM:CONSOLE /TLBID:1 /DYNAMICBASE /NXCOMPAT /MACHINE:X86 /ALLOWISOLATION\n");


			LinkedList<String> cSources = new LinkedList<String>();

			noutBuf.write("C_SOURCES = ");
			if (opts.isRunnable()) {
				noutBuf.write(topFB + "run.c");
				cSources.add(topFB + "run.c");
			}
			else if (opts.isSimul()) {
				noutBuf.write(topFB + "sim.c");
				cSources.add(topFB + "sim.c");
			}

			if (CGenerator.STFunctionsUsed) {
				noutBuf.write(" \\\n            STFunctions.c");
				cSources.add("STFunctions.c");
			}

			// Don't add pub & sub if not used...
			for (String s : srcDependencies) {
				if (s.lastIndexOf('.') <= 0)
					continue;
				String formatted = formatMakefileSrcPath(s);
				noutBuf.write(" \\\n            " + formatted);
				cSources.add(formatted);
			}
			
			if (NXTControlInterResourceCommGenerator.interRESUsed
					&& !srcDependencies.contains("pubsub.c")) {
				noutBuf.write(" \\\n            pubsub.c");
				cSources.add("pubsub.c");
			}

			for (FunctionBlock fb : FunctionBlock.fbs) {
				if (cSources.contains(fb.getCompiledType() + ".c"))
					continue;
				noutBuf.write(" \\\n            " + fb.getCompiledType() + ".c ");
				cSources.add(fb.getCompiledType() + ".c");
			}

			// If an HMI block was used
			if (opts.isNxtControl() && NXTControlHMIGenerator.hmiUsed) {
				for (String cFile : NXTControlHMIGenerator.extraCFiles) {
					noutBuf.write(" \\\n            " + cFile);
					cSources.add(cFile);
				}
			}
			if (opts.isSimul()) {
				for (String cFile : SimHelpers.extraCFiles) {
					noutBuf.write(" \\\n            " + cFile);
					cSources.add(cFile);
				}
			}
			noutBuf.write("\n");

			noutBuf.write("OBJS = $(C_SOURCES:.c=.obj)\n");
			noutBuf.write("LIBS =  \n");
			noutBuf.write("\n");
			noutBuf.write("RM = del /F /Q\n\n");
			noutBuf.write("all: $(TARGET)\n\n");
			noutBuf.write("$(TARGET): $(OBJS) Makefile.nmake\n");
			noutBuf.write("\t$(CC) $(CFLAGS) \"WSock32.Lib\" \"WinMM.Lib\" \"Ws2_32.lib\" ");
			if (opts.isSimul()) {
				for (String libFile : SimHelpers.extraCLibs)
					noutBuf.write(libFile + " ");
			}
			noutBuf.write("$(LIBS) $(OBJS) -o $(TARGET)\n\n");

			// damn nmake:
			for (String cfile : cSources) {
				int index = cfile.lastIndexOf('.');
				if (index <= 0)
					continue;
				noutBuf.write(cfile.substring(0, index) + ".obj: " + cfile + "\n");
				noutBuf.write("\t$(CC) $(CFLAGS) $(INCLUDES) /c " + cfile + "\n\n");
			}

			noutBuf.write("clean:\n");
			noutBuf.write("\t$(RM) $(OBJS)\n\n");
			noutBuf.write("cleanall:\n");
			noutBuf.write("\t$(RM) $(OBJS)\n");
			noutBuf.write("\t$(RM) $(TARGET)\n");

			noutBuf.close();
		}
		*/
	}

	/**
	 * Prints the names of C files that need to be included in a makefile.
	 * 
	 * @param fbt
	 *            function block type contained in a parent network
	 * @param fqParentName
	 *            instance name of the parent
	 * @param outBuf
	 *            output buffer to print to
	 * @param sources
	 *            list of C files that have already been printed
	 */
	private static void printCFileNames(FBType fbt, String fqParentName,
			BufferedWriter outBuf, LinkedList<String> sources) 	throws IOException {
		if (fbt.composite != null) {
			if (fbt.composite.instances == null )
				return;
			
			for (int block = 0; block < fbt.composite.instances.length; block++) {
				FBInstance f = fbt.composite.instances[block];
				// If its not 'unique' then the exact type will already be in the list (with .c appended)
				if( sources.contains(f.type+".c") ) 
					continue; // Skip as already done
				FunctionBlock fb = FunctionBlock.getFunctionBlock(f, fqParentName + f.name);
				if (fb == null) {
					OutputManager.printError(f.name,
							"Could not find FunctionBlock for type: " + f.type,
							OutputLevel.FATAL);
					System.exit(-1);
				}
				
				if ( CodeGenerator.isLocalComBlock(fb.getCompiledType()) )
					continue;
				String cFile = fb.getCompiledType() + ".c";
				if (sources.contains(cFile))
					continue;
				
				outBuf.write(" \\\n            " + cFile);
				sources.add(cFile);
				
				// Now to recurse
				FBType subfbt = fbt.subBlocks[block];
				if (subfbt.composite != null) {
					printCFileNames(subfbt, fqParentName + f.name + ".", outBuf,
							sources);
				}
			}
		}
	}
	
	/**
	 * Retrieve the message associated to each send/receive block.
	 * @param fbt function block type of a subblock in a given network
	 * @param sendMsgs list of messages that are to be sent by the network
	 * @param recvMsgs list of messages that are to be received by the network
	 * @param sendPrefix type prefix of the send block
	 * @param recvPrefix type prefix of the receive block
	 */
	protected static void retrieveMessages(FBType fbt, ArrayList<ComMessage> sendMsgs,
			ArrayList<ComMessage> recvMsgs, String sendPrefix, String recvPrefix) {		
		if (fbt.subBlocks != null) {
			for (FBType sub : fbt.subBlocks)
				retrieveMessages(sub, sendMsgs, recvMsgs, sendPrefix, recvPrefix);
		}
		if (fbt.name.startsWith(sendPrefix)) {
			String tail = fbt.name.substring(sendPrefix.length());
			if (!SIPath.isAllDigit(tail))
				return;
			if (fbt.params != null) {
				for (Parameter param : fbt.params){
					if (param.name.equals("ID")) {
						param.value = param.value.replace("\"", "");
						sendMsgs.add(new ComMessage(param.value));
						break;
					}
				}
			}
		}
		else if (fbt.name.startsWith(recvPrefix)) {
			String tail = fbt.name.substring(recvPrefix.length());
			if (!SIPath.isAllDigit(tail))
				return;
			if (fbt.params != null) {
				for (Parameter param : fbt.params){
					if (param.name.equals("ID")) {
						param.value = param.value.replace("\"", "");
						recvMsgs.add(new ComMessage(param.value));
						break;
					}
				}
			}
		}
	}
}
