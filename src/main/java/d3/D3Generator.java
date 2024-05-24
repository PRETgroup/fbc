/**
 * Class for generating D3-related code.
 * @author lihsien
 */
package d3;

import ir.*;
import ccode.*;
import fb.*;
import fbtostrl.*;

import java.io.IOException;
import java.util.*;

/**
 * Helper class to store the TTP message sent/received by a D3 device.
 * @author lihsien
 */
class D3Messages {
	private Device device;
	ArrayList<ComMessage> sendMsgs;
	ArrayList<ComMessage> recvMsgs;
	private String fileName;
	
	D3Messages(Device dev, ArrayList<ComMessage> s, ArrayList<ComMessage> r) {
		device = dev;
		sendMsgs = s;
		recvMsgs = r;
	}
	
	final void setFileName(String file) {
		fileName = file;
	}
	
	final String getFileName() {
		return fileName;
	}
	
	final String getFilePath() {
		return CodeGenerator.getParentName(fileName);
	}
	
	final String getProgramFilePrefix() {
		return CProgGenerator.getProgramFilePrefix(device.name, device.type);
	}
	
	final Device getDevice() {
		return device;
	}
	
	final String getDeviceName() {
		return device.name;
	}
	
	final HashSet<String> getSetOfAllMessages() {
		HashSet<String> messages = new HashSet<String>();
		for (ComMessage cm : sendMsgs)
			messages.add(cm.getMessage());
		for (ComMessage cm :  recvMsgs)
			messages.add(cm.getMessage());
		return messages;
	}
	
	final boolean isSendMsg(String msg) {
		for (ComMessage cm : sendMsgs) {
			if (cm.getMessage().equals(msg))
				return true;
		}
		return false;
	}
	
	final boolean isRecvMsg(String msg) {
		for (ComMessage cm : recvMsgs) {
			if (cm.getMessage().equals(msg))
				return true;
		}
		return false;
	}
}

//Helper class for storing TTP send messages 
class SendMessage {
	String msgID;				// original message name (from ID parameter)
	VarDeclaration[] messages;	// variables derived from the message
	String devName;				// device name
	
	SendMessage(String id, VarDeclaration[] m, String d) {
		msgID = id;
		messages = m;
		devName = d;
	}
}

public class D3Generator extends CProgGenerator {
	public D3Generator(Device dev, HashSet<Block> comBlocks, OutputPath o) {
		super(dev, comBlocks, o);
	}

	/**
	 * Generates code for all deferred devices for the D3 platform.
	 * @param programs list of program information needed for deferred code generation 
	 * @throws IOException
	 */
	public static void generateAllCode(LinkedHashMap<String, ProgramInfo> programs) 
			throws IOException {
		String[] nodes = getListOfDevIDs(programs);
		D3Messages[] messages = getTTPMessages(programs);
		assert (nodes.length == messages.length): 
				"Number of devices computed is inconsistent: " + nodes.length + 
				" <> " + messages.length + ".\n";
		
		Set<String> fileNames = programs.keySet();
		Iterator<String> fn = fileNames.iterator();
		String fileName = null;
		for (int i = 0; i < messages.length; i++) {
			make_d3_setup_h(messages[i].getFilePath() +  "d3_setup.h", nodes.length);
			fileName = fn.next();
			ProgramInfo progInfo = programs.get(fileName);
			HashMap<String, GlobalVar> globalVars = progInfo.getGlobalVars();
			makeD3_setup_h(messages[i], nodes, globalVars, i+1);
			makeD3_setup_c(messages[i], nodes, globalVars, 
					progInfo.getSrcDependencies());
			makeD3_task_c(messages[i], globalVars, progInfo.getSrcDependencies());
			Device dev = messages[i].getDevice();
			makeD3main(dev, messages[i], globalVars, nodes);
			progInfo.handleLastRites(FBtoStrl.opts, dev, dev.name);
		}
		
		makeD3_NodeInfo(programs, messages, fileName);
	}
	
	/**
	 * Creates a list containing IDs for each device.
	 * @param generators
	 * @return An array containing IDs for each device.
	 */
	private static String[] getListOfDevIDs(LinkedHashMap<String, ProgramInfo> programs) {
		Set<String> fileNames = programs.keySet();
		String[] nodes = new String[fileNames.size()];
		int i = 0;
		for (String fileName : fileNames) {
			nodes[i] = "NODEID_" + programs.get(fileName).getDeviceName();
			i++;
		}
		return nodes;
	}
	
	/**
	 * Returns all the TTP messages that are sent/received by the device "dev."
	 * @param dev the TTP device of interest
	 * @return All TTP messages that are sent/received by "dev".
	 */
	private static D3Messages getTTPMessageForDev(Device dev) {
		ArrayList<ComMessage> sendMsgs = new ArrayList<ComMessage>();
		ArrayList<ComMessage> recvMsgs = new ArrayList<ComMessage>();
		for (Resource res : dev.resources)
			retrieveMessages(res.resType, sendMsgs, recvMsgs, "SENDD3_", "RECVD3_");
		
		return new D3Messages(dev, sendMsgs, recvMsgs);
	}
	
	/**
	 * Retrieves the list of TTP messages sent/received by all devices in the system.
	 * This method also does some error checking to ensure that all received
	 * messages have corresponding senders and vice versa.
	 * @param generators list of deferred program generators
	 * @return List of TTP messages sent/received by all devices in the system.
	 */
	private static D3Messages[] getTTPMessages(
			LinkedHashMap<String, ProgramInfo> programs) {
		Set<String> fileNames = programs.keySet();
		int size = fileNames.size();	// this is the number of devices in the TTP cluster
		D3Messages[] messages = new D3Messages[size];
		String[] devNames = new String[size];
		
		int i = 0;
		for (String fileName : fileNames) {
			Device dev = programs.get(fileName).getDevice();
			messages[i] = getTTPMessageForDev(dev);
			messages[i].setFileName(fileName);
			devNames[i] = dev.name;
			i++;
		}
		
		// Check that every received message has a sender
		for (i = 0; i < messages.length; i++) {
			for (ComMessage rMsg : messages[i].recvMsgs) {
				for (int j = 0; j < messages.length; j++) {
					if (j == i)
						continue;
					for (ComMessage sMsg : messages[j].sendMsgs) {
						if (sMsg.msgEquals(rMsg)) {
							rMsg.addNodeID(new Integer(j));
							sMsg.addNodeID(new Integer(i));
						}
					}
				}
				if (rMsg.noPartnerNode()) {
					OutputManager.printError(devNames[i],  
							"No corresponding sender for received message `" + 
							rMsg + "\'.", OutputLevel.FATAL);
					System.exit(-1);
				}
			}
		}
		
		// Check that every sent message has a receiver
		for (i = 0; i < messages.length; i++) {
			for (ComMessage sMsg : messages[i].sendMsgs) {
				if (!sMsg.noPartnerNode())
					continue;	// skip if partner nodes have already been found
				for (int j = 0; j < messages.length; j++) {
					if (j == i)
						continue;
					for (ComMessage rMsg : messages[j].recvMsgs) {
						if (rMsg.msgEquals(sMsg)) {
							sMsg.addNodeID(new Integer(j));
						}
					}
				}
				if (sMsg.noPartnerNode()) {
					OutputManager.printError(devNames[i],  
							"No corresponding receiver for sent message `" + 
							sMsg + "\'.", OutputLevel.FATAL);
					System.exit(-1);
				}
			}
		}
		
		return messages;
	}
	
	/**
	 * Prints the common d3_setup.h file (for the D3 platform).
	 * @param filePath output path for d3_setup.h
	 * @param numOfNodes number of nodes in the TTP network
	 * @throws IOException
	 */
	private static void make_d3_setup_h(String filePath, int numOfNodes) throws IOException {
		CodePrinter printer = new CodePrinter(filePath);
		printer.print("// This file is generated by FBC.", 1);
		printer.smartPrint("#ifndef D3_SETUP_H_");
		printer.smartPrint("#define D3_SETUP_H_", 1);
		printer.smartPrint("enum {TOTAL_NODE = " + numOfNodes + "};", 1);
		printer.smartPrint("extern int NODE_NO;", 1);
		printer.smartPrint("#endif // D3_SETUP_H_");
		printer.close();
	}
	
	/**
	 * Prints the node-specific d3_setup.h file (for the D3 platform).
	 * @param message messages sent/received for a given device (D3 node)
	 * @param nodes IDs for each device (D3 node)
	 * @param globalVars variable definitions for the messages
	 * @param nodeID ID for this particular device
	 * @throws IOException
	 */
	private static void makeD3_setup_h(D3Messages message, String[] nodes, 
			HashMap<String, GlobalVar> globalVars, int nodeID) throws IOException {
		CodePrinter printer = new CodePrinter(message.getFilePath() + 
				message.getProgramFilePrefix() + "d3_setup.h");
		
		printer.print("// This file is generated by FBC.", 1);
		printer.smartPrint("#ifdef _D3PT_");
		printer.smartPrint("    #define STORAGE");
		printer.smartPrint("#else");
		printer.smartPrint("    #define STORAGE extern");
		printer.smartPrint("#endif");
		printer.smartPrint("#include \"fbtypes.h\"", 1);
		
		printer.smartPrint("enum {");
		for (int i = 0; i < nodes.length; i++)
			printer.smartPrint(nodes[i] + " = " + Integer.toString(i+1) + ",");
		printer.smartPrint("NODE_NUM = " + nodeID);
		printer.smartPrint("};", 1);
		
		printer.smartPrint("#include \"d3_setup.h\"");
		printer.smartPrint("#include \"d3pt.h\"", 1);
		
		printer.smartPrint("STORAGE void D3SetUp(void);", 1);
		
		// Print out all received messages
		for (ComMessage cm : message.recvMsgs) {
			GlobalVar globalVar = globalVars.get(cm.getMessage());
			String array;
			VarDeclaration[] vars = globalVar.getMembers(message.getDeviceName());
			for (VarDeclaration var : vars) {
				String arraySize = var.getArraySize();
				if (arraySize.isEmpty()) {
					array = "[(sizeof(" + CGenerator.getPortableCType(var.getType()) + 
							")" + "+sizeof(ubyte4)-1) / sizeof(ubyte4) + 1];";
				}
				else {
					if (CodeGenerator.validateArraySize(arraySize) <= 0) {
						OutputManager.printError("RECVD3", "Message `" + cm.getMessage() + 
								"' has an invalid array size.", OutputLevel.FATAL);
						System.exit(0);
					}
					array = "[(sizeof(" + CGenerator.getPortableCType(var.getType()) + 
					")*" + arraySize + "+sizeof(ubyte4)-1) / sizeof(ubyte4) + 1];";
				}
				printer.smartPrint("// STORAGE ubyte1 __" + var.getName() + "_copy_m_rstat;");
				printer.smartPrint("STORAGE int Valid_" + var.getName() + ";");
				printer.smartPrint("STORAGE ubyte4 __" + var.getName() + "_copy_m" + 
						array);
				printer.smartPrint("STORAGE ubyte4 " + var.getName() + "_data" + 
						array, 1);
			}
		}
		
		// Print out all sent messages
		for (ComMessage cm : message.sendMsgs) {
			GlobalVar globalVar = globalVars.get(cm.getMessage());
			String array;
			VarDeclaration[] vars = globalVar.getMembers(message.getDeviceName());
			for (VarDeclaration var : vars) {
				String arraySize = var.getArraySize();
				if (arraySize.isEmpty()) {
					array = "[(sizeof(" + CGenerator.getPortableCType(var.getType()) + 
							")" + "+sizeof(ubyte4)-1) / sizeof(ubyte4) + 1];";
				}
				else {
					if (CodeGenerator.validateArraySize(arraySize) <= 0) {
						OutputManager.printError("SENDD3", "Message `" + cm.getMessage() + 
								"' has an invalid array size.", OutputLevel.FATAL);
						System.exit(0);
					}
					array = "[(sizeof(" + CGenerator.getPortableCType(var.getType()) + 
					")*" + arraySize + "+sizeof(ubyte4)-1) / sizeof(ubyte4) + 1];";
				}
				printer.smartPrint("STORAGE int Valid_" + var.getName() + ";");
				printer.smartPrint("STORAGE ubyte4 __" + var.getName() + "_copy_m" + 
						array);
			}
		}
		printer.println();
		
		printer.print("#undef STORAGE");
		
		printer.close();
	}
	
	/**
	 * Prints the d3_setup.c file (for the D3 platform).
	 * @param message messages sent/received for a given device (D3 node)
	 * @param nodes IDs for each device (D3 node)
	 * @param globalVars variable definitions for the messages
	 * @param srcDependencies set of additional source dependencies that need to 
	 *        be considered for makefile generation
	 * @throws IOException
	 */
	private static void makeD3_setup_c(D3Messages message, String[] nodes, 
			HashMap<String, GlobalVar> globalVars, HashSet<String> srcDependencies) 
	throws IOException {
		String filePrefix = message.getProgramFilePrefix();
		String file = filePrefix + "d3_setup.c";
		srcDependencies.add(file);
		
		CodePrinter printer = new CodePrinter(message.getFilePath() + file);		
		printer.print("// This file is generated by FBC.", 1);
		printer.smartPrint("#undef _D3SETUP_");
		printer.smartPrint("#define _D3SETUP_", 1);
		
		printer.smartPrint("#include \"d3_ttpc.h\"");
		printer.smartPrint("#include \"d3_ttpc_msg.h\"");
		printer.smartPrint("#include \"" + filePrefix + "d3_setup.h\"", 1);
		
		printer.print("/* Starts the D3 TTP/C protocol initialization */");
		printer.smartPrint("void D3SetUp(void)");
		printer.smartPrint("{");
		printer.smartPrint("D3Init();", 1);
		
		// Initialize all send message buffers
		for (ComMessage cm : message.sendMsgs) {
			GlobalVar globalVar = globalVars.get(cm.getMessage());
			VarDeclaration[] vars = globalVar.getMembers(message.getDeviceName());
			int[] nodeIDs = cm.getNodeIDs();
			for (int id : nodeIDs) {
				for (VarDeclaration var : vars) {
					String varName = "__" + var.getName() + "_copy_m";
					printer.print("D3SetMessage(" + nodes[id] + 
							", SEND, sizeof(" + varName + "), &Valid_" + 
							var.getName() + ", " + varName + ");");
				}
			}
		}
		
		// Initialize all receive message buffers
		for (ComMessage cm : message.recvMsgs) {
			GlobalVar globalVar = globalVars.get(cm.getMessage());
			VarDeclaration[] vars = globalVar.getMembers(message.getDeviceName());
			int[] nodeIDs = cm.getNodeIDs();
			for (int id : nodeIDs) {
				for (VarDeclaration var : vars) {
					String varName = var.getName() + "_data";
					printer.print("D3SetMessage(" + nodes[id] + 
							", RECEIVE, sizeof(" + varName + "), &Valid_" + 
							var.getName() + ", " + varName + ");");
				}
			}
		}
		
		printer.println();
		printer.smartPrint("ttpc_startup_process();");
		printer.smartPrint("}");
		
		printer.close();
	}
	
	/**
	 * Prints the d3_task.c file (for the D3 platform).
	 * @param message messages sent/received for a given device (D3 node)
	 * @param globalVars variable definitions for the messages
	 * @param srcDependencies set of additional source dependencies that need to 
	 *        be considered for makefile generation
	 * @throws IOException
	 */
	private static void makeD3_task_c(D3Messages message,  
			HashMap<String, GlobalVar> globalVars, HashSet<String> srcDependencies) 
	throws IOException {
		String filePrefix = message.getProgramFilePrefix();
		String file = filePrefix + "d3_TASK_task.c";
		srcDependencies.add(file);
		
		CodePrinter printer = new CodePrinter(message.getFilePath() + file);
		printer.print("// This file is generated by FBC.", 1);
		
		printer.smartPrint("#include \"d3_ttpc.h\"");
		printer.smartPrint("#include \"d3_ttpc_msg.h\"");
		printer.print("#include \"" + filePrefix + "d3_setup.h\"", 1);
		
		printer.smartPrint("static void Receive_data(ubyte1 rstat, ubyte4* sdata, int* valid, ubyte4* ddata, int array);", 1);
		
		printer.smartPrint("static void Receive_data(ubyte1 rstat, ubyte4* sdata, int* valid, ubyte4* ddata, int array)");
		printer.smartPrint("{");
		printer.smartPrint("if (rstat) {");
		printer.smartPrint("int i;");
		printer.smartPrint("for (i = 0; i < array; i++)");
		printer.indent();
		printer.print("ddata[i] = sdata[i];");
		printer.unindent();
		printer.smartPrint("(*valid)++;");
		printer.smartPrint("}");
		printer.smartPrint("}", 1);
		
		printer.smartPrint("tt_task(taskRecieve)");
		printer.smartPrint("{");
		printer.smartPrint("if (d3ctl.start) {");
		StringBuilder varName = new StringBuilder();
		for (ComMessage cm : message.recvMsgs) {
			GlobalVar globalVar = globalVars.get(cm.getMessage());
			VarDeclaration[] vars = globalVar.getMembers(message.getDeviceName());
			for (VarDeclaration var : vars) {
				varName.replace(0, varName.length(), var.getName() + "_data");
				printer.print("Receive_data(__" + var.getName() + "_copy_m_rstat, " + 
						"__" + var.getName() + "_copy_m, " + 
						"&Valid_" + var.getName() + ", " + varName.toString() + 
						", sizeof(" + varName.toString() + ")/sizeof(ubyte4));");
			}
		}
		printer.smartPrint("}");
		printer.smartPrint("}", 1);
		
		printer.smartPrint("tt_task(taskSend)");
		printer.smartPrint("{");
		printer.smartPrint("if (d3ctl.start) {");
		printer.smartPrint("int i;");
		printer.smartPrint("for ( i=0 ; i < TOTAL_NODE ; i++ ) {");
		printer.smartPrint("if ( d3ctl.node[i].sr[SEND].valid != NULL ) {");
		printer.smartPrint("if ( (*d3ctl.node[i].sr[SEND].valid) > 0 ) {");
		printer.smartPrint("(*d3ctl.node[i].sr[SEND].valid) = 0 ;");
		printer.smartPrint("}");
		printer.smartPrint("else {");
		printer.smartPrint("if ( d3ctl.node[i].sr[SEND].pdata != NULL ) {");
		printer.smartPrint("int j;");
		printer.smartPrint("for ( j=0; j < (d3ctl.node[i].sr[SEND].len/sizeof(ubyte4)); j++ ) {");
		printer.smartPrint("d3ctl.node[i].sr[SEND].pdata[j] = 0;");
		printer.smartPrint("}");	   
		printer.smartPrint("}");
		printer.smartPrint("}");
		printer.smartPrint("}");
		printer.smartPrint("}");
		printer.smartPrint("}");
		printer.smartPrint("}", 1);
		
		Resource[] resources = message.getDevice().getResources();
		for (Resource res : resources) {
			printer.smartPrint("tt_task(" + res.name + ") {");
			printer.smartPrint("taskRecieve();");
			if (!message.sendMsgs.isEmpty())
				printer.smartPrint("taskSend();");
			printer.smartPrint("if (d3ctl.start) {");
			printer.smartPrint("d3ctl.d3_round++;");
			printer.smartPrint("d3ctl.d3_sync_cnt++;");
			printer.smartPrint("}");
			printer.smartPrint("}");
		}
		
		printer.close();
	}
	
	/**
	 * Prints the main file if generating code for the D3 platform
	 * @param device device (D3 node) for which the main file is being generated
	 * @param message messages sent/received for a given device
	 * @param globalVars variable definitions for the messages
	 * @param nodes IDs for each device
	 * @throws IOException
	 */
	private static void makeD3main(Device device, D3Messages message, 
			HashMap<String, GlobalVar> globalVars, String[] nodes) 
	throws IOException {
		CodePrinter printer = new CodePrinter(message.getFilePath() + 
				makeDevFileName(device.name, device.type) + ".c");
		printer.print("// This file is generated by FBC.", 1);
		if (device.comment != null)
			printer.print("// " + device.comment, 1);
		
		for (int i = 0; i < device.resources.length; i++) {
			printer.print("#include \"" + device.resources[i].resType.getTargetType() + ".h\"");
		}
		printer.println();
		
		String filePrefix = message.getProgramFilePrefix(); 
		printer.print("#define _D3PT_", 1);
		printer.print("#include \"d3_ttpc.h\"");
		printer.print("#include \"d3_ttpc_msg.h\"");
		printer.print("#include \"" + filePrefix + "d3_setup.h\"");
		printer.print("#include \"d3_com_init.h\"", 1);
		
		printer.print("/* Debug and test */");
		printer.print("#define DEBUG");
		printer.print("#include \"debug_packet.h\"", 1);
		
		printer.print("static void start_sync(void);", 1);
		printer.print("// Define and initialize NODE_NO");
		printer.print("int NODE_NO = NODE_NUM;", 1);
		
		int txMessages = 0;
		if (!message.sendMsgs.isEmpty()) {
			printer.print("// Messages sent by this node");
			for (ComMessage cm : message.sendMsgs) {
				GlobalVar globalVar = globalVars.get(cm.getMessage());
				int size = 0;
				VarDeclaration[] vars = globalVar.getMembers(message.getDeviceName());
				for (VarDeclaration var : vars) {
					String array = CodeGenerator.getArrayIndex(var.getArraySize(), "SENDD3", 
							cm.getMessage()) + ";";
					printer.smartPrint(CGenerator.getPortableCType(var.getType()) + 
							" _" + var.getName() + array);
					size++;
				}
				txMessages += size * cm.getNumOfMsgPartners();
			}			
			if (txMessages > 1)
				printer.print("int txStatus[" + txMessages + "];", 1);
			else
				printer.print("int txStatus;", 1);
		}
		
		int rxMessages = 0;
		if (!message.recvMsgs.isEmpty()) {
			printer.print("// Messages received by this node");
			for (ComMessage cm : message.recvMsgs) {
				GlobalVar globalVar = globalVars.get(cm.getMessage());
				int size = 0;
				VarDeclaration[] vars = globalVar.getMembers(message.getDeviceName());
				for (VarDeclaration var : vars) {
					String array = CodeGenerator.getArrayIndex(var.getArraySize(), "RECVD3", 
							cm.getMessage()) + ";";
					printer.smartPrint(CGenerator.getPortableCType(var.getType()) + 
							" _" + var.getName() + array);
					size++;
				}
				rxMessages += size * cm.getNumOfMsgPartners();
			}
			if (rxMessages > 1)
				printer.print("int rxStatus[" + rxMessages + "];", 1);
			else
				printer.print("int rxStatus;", 1);
		}
		
		printer.print("// Application data structures");
		for (int i = 0; i < device.resources.length; i++)
			printer.print(device.resources[i].resType.getTargetType() + " " + device.resources[i].name + ";");
		printer.println();
		
		printer.print("/* Synchronized startup */");
		printer.print("static void start_sync(void)");
		printer.smartPrint("{");
		printer.smartPrint("for (;;) {");
		printer.smartPrint("int rc = D3TTPwatch();");
		printer.smartPrint("if (rc < 0) {");
		printer.smartPrint("do {");
		printer.smartPrint("rc = D3Restart();");
		printer.smartPrint("} while (rc);");
		printer.smartPrint("continue;");
		printer.smartPrint("}");
		printer.smartPrint("else {");
		
		// Generate liveness test for all partner nodes that sends to or  
		// receives from current node
		HashSet<Integer> nodeIDs = new HashSet<Integer>();
		for (ComMessage cm : message.sendMsgs)
			nodeIDs.addAll(cm.getArrayListOfIDs());
		for (ComMessage cm : message.recvMsgs)
			nodeIDs.addAll(cm.getArrayListOfIDs());
		if (!nodeIDs.isEmpty()) {
			Iterator<Integer> i = nodeIDs.iterator();
			StringBuilder str = new StringBuilder("if ( ttpc_is_node_alive(" + 
					nodes[i.next().intValue()] + ")");
			while (i.hasNext()) {
				str.append(" &&");
				printer.smartPrint(str.toString());
				str.replace(0, str.length(), "     ttpc_is_node_alive(" + 
						nodes[i.next().intValue()] + ")");
			}
			str.append(" ) {");
			printer.smartPrint(str.toString());
		}
		
		printer.smartPrint("unsigned int cycle = d3ctl.ttp_cycle;");
		printer.smartPrint("for (;;) {");
		printer.smartPrint("rc = D3TTPwatch();");
		printer.smartPrint("if (rc < 0) {");
		printer.smartPrint("do {");
		printer.smartPrint("rc = D3Restart();");
		printer.smartPrint("} while (rc);");
		printer.smartPrint("}");
		printer.smartPrint("if ( (cycle + TTPC_WAIT_CYCLE) == D3TTPCycleCnt() )");
		printer.indent();
		printer.smartPrint("return;");
		printer.unindent();
		printer.smartPrint("}");
		printer.smartPrint("}");
		printer.smartPrint("}");
		printer.smartPrint("}");
		printer.smartPrint("}", 1);
		
		printer.print("int main(void)");
		printer.smartPrint("{");		
		printer.smartPrint("#ifdef DEBUG");
		printer.print("xil_printf(\"Start\\n\");");
		printer.smartPrint("#endif", 1);
		
		for (int i = 0; i < device.resources.length; i++)
			printer.print(device.resources[i].resType.getTargetType() + "init(&" + device.resources[i].name + ");");
		printer.println();
		
		printer.print("/* TTP message initialization comes here, if any */", 1);
		
		printer.print("D3ComInit();");
		printer.print("LED_OFF();");
		printer.print("D3SetUp();");
		printer.print("start_sync();", 1);
		
		printer.smartPrint("for (;;) {");
		printer.smartPrint("int rc_sync = D3Sync();");
		printer.smartPrint("if (rc_sync < 0) {");
		printer.smartPrint("#ifdef DEBUG");
		printer.smartPrint("xil_printf(\"Error: D3Sync(%d).\\n\", rc_sync);");
		printer.smartPrint("#endif");		
		printer.smartPrint("start_sync();");
		printer.smartPrint("continue;");
		printer.smartPrint("}");
		printer.smartPrint("else {");
		if (rxMessages > 0) {
			printer.print("// Read from TTP communication buffers");
			int status = 0;
			for (ComMessage cm : message.recvMsgs) {
				GlobalVar globalVar = globalVars.get(cm.getMessage());
				VarDeclaration[] vars = globalVar.getMembers(message.getDeviceName());
				int[] ids = cm.getNodeIDs();
				for (int id : ids) {
					for (VarDeclaration var : vars) {
						String cast;
						if (var.getArraySize().isEmpty())
							cast = ", (unsigned char*)&_";
						else
							cast = ", (unsigned char*)_";
						if (rxMessages > 1) {
							printer.print("rxStatus[" + status + "] = D3RxUChar(" + 
									nodes[id] + cast + var.getName() + ", sizeof(_" + 
									var.getName() + "));");
							status++;
						}
						else {
							printer.print("rxStatus = D3RxUChar(" + nodes[id] + 
									cast + var.getName() + ", sizeof(_" + 
									var.getName() + "));");
						}
					}
				}
			}
			printer.println();
			printer.print("/* Error checking code for reading from communication buffers comes here, if any */", 1);
		}
		
		printer.print("// User application code");
		for (int i = 0; i < device.resources.length; i++) {
			printer.print(device.resources[i].resType.getTargetType() + "run();");
		}
		
		if (txMessages > 0) {
			printer.println();
			printer.print("// Write to TTP communication buffers");
			int status = 0;
			for (ComMessage cm : message.sendMsgs) {
				GlobalVar globalVar = globalVars.get(cm.getMessage());
				VarDeclaration[] vars = globalVar.getMembers(message.getDeviceName());
				int[] ids = cm.getNodeIDs();
				for (int id : ids) {
					for (VarDeclaration var : vars) {
						String cast;
						if (var.getArraySize().isEmpty())
							cast = ", (unsigned char*)&_";
						else
							cast = ", (unsigned char*)_";
						if (txMessages > 1) {
							printer.print("txStatus[" + status + "] = D3TxUChar(" + 
									nodes[id] + cast + var.getName() + ", sizeof(_" + 
									var.getName() + "));");
							status++;
						}
						else {
							printer.print("txStatus = D3TxUChar(" + nodes[id] + 
									cast + var.getName() + ", sizeof(_" + 
									var.getName() + "));");
						}
					}
				}
			}
			printer.println();
			printer.print("/* Error checking code for writing to communication buffers comes here, if any */");
		}
		
		printer.smartPrint("}");
		printer.smartPrint("}");
		printer.smartPrint("}", 1);
		  
		printer.print("#undef _D3PT_");
		printer.close();
	}
	
	/**
	 * Generate device and message information to enable integration with TTTech's tools. 
	 * @param programs list of program information needed for deferred code generation
	 * @param messages list of TTP messages sent/received by all devices in the system
	 * @param fileName arbitrary key to any one entry of "programs"
	 * @throws IOException
	 */
	private static void makeD3_NodeInfo(LinkedHashMap<String, ProgramInfo> programs, 
			D3Messages[] messages, String fileName) throws IOException {
		if (programs == null)
			return;
		if (programs.isEmpty())
			return;
		
		ProgramInfo progInfo = programs.get(fileName);
		CodePrinter printer = new CodePrinter(progInfo.sysOutputPath() + "Nodes.xml");
		printer.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		printer.print("<!-- This file is generated by FBC. -->");
		printer.print("<root>");
		printer.indent();
		
		ArrayList<SendMessage> sendMessages = new ArrayList<SendMessage>();
		
		// Print node information
		Iterator<String> fn = programs.keySet().iterator();
		for (D3Messages message : messages) {
			progInfo = programs.get(fn.next());
			HashMap<String, GlobalVar> globalVars = progInfo.getGlobalVars();
			HashSet<String> msgSet = message.getSetOfAllMessages();
			String devName = message.getDeviceName();
			printer.print("<node name=\"" + devName + "\">");
			printer.indent();
			Resource[] resources = message.getDevice().getResources();
			for (Resource res : resources) {
				printer.print("<task name=\"" + res.name + "\">");
				printer.indent();
				for (String msg : msgSet) {
					GlobalVar globalVar = globalVars.get(msg);
					VarDeclaration[] vars = globalVar.getMembers(devName);
					String recv = message.isRecvMsg(msg) ? "yes" : "no";
					String send;
					if (message.isSendMsg(msg)) {
						send = "yes";
						sendMessages.add(new SendMessage(msg, vars, devName));
					}
					else
						send = "no";
					for (VarDeclaration var : vars) {
						printer.print("<node_message receives=\"" + recv + "\" sends=\"" + 
								send + "\">" + var.getName() + "</node_message>");
					}
				}				
				printer.unindent();
				printer.print("</task>");
			}
			printer.unindent();
			printer.print("</node>");
		}
		
		int size = sendMessages.size();
		ArrayList<VarDeclaration[]> ubyte4Messages = new ArrayList<VarDeclaration[]>(size);
		String[] msgIDs = new String[size];
		String[] devNames = new String[size];
		
		// Convert all message types to equivalent UDINT arrays
		for (int i = 0; i < size; i++) {
			SendMessage sm = sendMessages.get(i);
			ubyte4Messages.add( mapToUDINTarray(sm.messages) );
			msgIDs[i] = sm.msgID;
			devNames[i] = sm.devName;
		}
		
		// Print message type information
		HashSet<String> msgTypes = new HashSet<String>();
		for (int i = 0; i < size; i++)
			printMsgTypes(msgTypes, printer, ubyte4Messages.get(i), msgIDs[i]);
		
		// Print message information
		for (int i = 0; i < size; i++)
			printMessages(printer, ubyte4Messages.get(i), msgIDs[i], devNames[i]);
		
		printer.unindent();
		printer.print("</root>");
		printer.close();		
	}
	
	/**
	 * Prints the TTP message types used by a device.
	 * @param msgTypes set of all printed message types
	 * @param printer code printer
	 * @param messages list of messages associated to a communication function block
	 * @param msgID message ID
	 * @throws IOException
	 */
	private static void printMsgTypes(HashSet<String> msgTypes, CodePrinter printer, 
			VarDeclaration[] messages, String msgID) throws IOException {
		for (VarDeclaration var : messages) {
			String type = TTPDataTypes.getTTPDataType(var.getType(), Options.strlen());
			if (msgTypes.contains(type))
				continue;
			msgTypes.add(type);
			MsgProperties msgProp = TTPDataTypes.getMsgProperties(type);
			if (msgProp != null) {
				// Create message_type_p here (extended primitive types)
				printer.print("<message_type_p name=\"" + type + "\">");
				printer.indent();
				printer.print("<length>" + msgProp.length + "</length>");
				printer.print("<type_cat>" + msgProp.typeCat + "</type_cat>");
				printer.print("<typedef>" + msgProp.typedef + "</typedef>");
				printer.print("<type_length>" + msgProp.typeLength + "</type_length>");
				printer.unindent();
				printer.print("</message_type_p>");
			}
			else if ( !TTPDataTypes.isPrimitive(type) ) {
				// Create message_type_s here (user-defined types)
				printer.print("<message_type_s name=\"" + type + "\">");
				printer.indent();
				VarDeclaration[] vars = IEC61131Types.getStructuredType(type);
				for (VarDeclaration elem : vars) {
					String name = elem.getName();
					String array = TTPDataTypes.getArrayIndex(elem.getArraySize(), type, 
							name);
					printer.print("<element element_type=\"" + elem.getType() + array +
							"\">" + name + "</element>");
				}
				printer.unindent();
				printer.print("</message_type_s>");
			}
			
			String array = TTPDataTypes.getArrayIndex(var.getArraySize(), msgID, 
					var.getName());
			if ( !array.isEmpty() ) {
				// Create message_type_a here (array types)
				String arrayType = type + array;
				if (msgTypes.contains(arrayType))
					continue;
				msgTypes.add(arrayType);
				printer.print("<message_type_a name=\"" + arrayType + "\">");
				printer.indent();
				printer.print("<element_type>" + type + "</element_type>");
				printer.print("<length>" + var.getArraySize() + "</length>");
				printer.unindent();
				printer.print("</message_type_a>");
			}
		}
	}
	
	/**
	 * Prints the TTP messages used by a device.
	 * @param printer code printer
	 * @param messages list of messages associated to a communication function block
	 * @param msgID message ID
	 * @param devName device name
	 * @throws IOException
	 */
	private static void printMessages(CodePrinter printer, VarDeclaration[] messages, 
			String msgID, String devName) throws IOException {
		for (VarDeclaration var : messages) {
			String name = var.getName();
			printer.print("<message name=\"" + name + "\">");
			printer.indent();
			printer.print("<d_period>Cluster.tr_period*2</d_period>");
			printer.print("<subsystem>" + devName + "</subsystem>");
			String array = TTPDataTypes.getArrayIndex(var.getArraySize(), msgID, name);
			String type = TTPDataTypes.getTTPDataType(var.getType(), Options.strlen());
			printer.print("<msg_type>" + type + array + "</msg_type>");
			printer.print("<init_value>" + 
					TTPDataTypes.getInitializer(!array.isEmpty(), var.getInitial(), type, name) + 
					"</init_value>");
			printer.unindent();
			printer.print("</message>");
		}
	}
	
	/**
	 * Converts any data type into an equivalent UDINT array. Data types that are not an
	 * exact multiple of sizeof(UDINT) will be converted to the smallest UDINT array 
	 * capable of fully containing it. An additional element will then be added to the
	 * resulting UDINT array to store D3 specific header information.  
	 * @param messages
	 * @return
	 */
	private static VarDeclaration[] mapToUDINTarray(VarDeclaration[] messages) {
		VarDeclaration[] vars = new VarDeclaration[messages.length];
		for (int i = 0; i < messages.length; i++) {
			int baseSize = IEC61131Types.getSizeof("UDINT", 1);
			int arraySize = CodeGenerator.validateArraySize(messages[i].getArraySize());
			if (arraySize <= 0)
				arraySize = 1;
			int sizeof = IEC61131Types.getSizeof(messages[i].getType(), arraySize);
			String array = Integer.toString((sizeof + baseSize - 1) / baseSize + 1);
			vars[i] = new VarDeclaration(messages[i].getName(), "UDINT", array);
		}
		return vars;
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
		hprint.print("#include \"d3_tttech.h\"", 1);
		
		for (String varName : varNames) {
			GlobalVar global = globalVars.get(varName);
			printDirectAccessGlobalVars(global, hprint, null, devName);
		}
		
		hprint.println();
		hprint.print("#endif // " + macroHeader);		
		hprint.close();
	}
	
	/**
	 * Prints global variables that are accessed directly without using any functions.
	 * @param global information about the global variables
	 * @param hprint code printer for the .h file
	 * @param cprint code printer for the .c file
	 * @param devName device name
	 * @throws IOException
	 */
	private static void printDirectAccessGlobalVars(GlobalVar global, CodePrinter hprint, 
			CodePrinter cprint, String devName) throws IOException {
		String prefix = "_";
		VarDeclaration[] vars = global.getMembers(devName);
		for (VarDeclaration var : vars) {
			String type = var.getType();
			String name = var.getName();
			String array = CodeGenerator.getArrayIndex(var.getArraySize(), "", name) + ";";
			
			if (type == null) {
				StringBuilder varName = new StringBuilder(name);
				int index = name.indexOf('_');
				if (index > 0)
					varName.setCharAt(index, '.');
				OutputManager.printError("", "`" + varName.toString() + 
						"\' has undefined port type.", OutputLevel.FATAL);
				System.exit(0);
			}
			if (hprint != null) {
				hprint.print("extern " + CGenerator.getPortableCType(var.getType()) + 
						" " + prefix + var.getName() + array);
			}
			if (cprint != null) {
				cprint.print(CGenerator.getPortableCType(var.getType()) + " " +
						prefix + var.getName() + array);
			}
		}
	}
}
