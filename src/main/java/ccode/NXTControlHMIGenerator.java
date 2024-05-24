package ccode;

import ir.*;

import java.io.*;
import java.util.LinkedList;

import org.jdom2.*;
import org.jdom2.input.SAXBuilder;

import fb.*;
import fbtostrl.*;

public class NXTControlHMIGenerator {
	public static boolean hmiUsed = false;
	public static String[] extraCFiles = {"NxtHMIServer.c","NxtHMIData.c","NxtCommunication.c","--Device--Specific--.c"}; // NxtHMIServerConfig changes to device specific version
	public static LinkedList<String> pendingHMIInits = new LinkedList<String>();
	public static LinkedList<String> pendingHMIInitDeclarations = new LinkedList<String>();
	
	static FileWriter hmiconfigh;
	static FileWriter hmiconfigc;
	private static StringBuilder handleSub = new StringBuilder();
	private static StringBuilder setData = new StringBuilder();
	private static StringBuilder processUpdates = new StringBuilder();
	
	private static String currentDeviceName = "";
	// Only required for NxtControl
	// Some maybe useful else where
	public static String ProjectName;
	public static String ProjectState = "Running";
	public static String ProjectGuid;
	
	/*
	 * Get project options from DB.xml file: eg:
	 * ..\nxtControl\<ProjectName>\IEC61499\Database\DB.xml
	 */
	public static void parseNxtControlProject(File sourcefolder)
	{
		File db = new File(sourcefolder.getAbsolutePath()+File.separator+"Database"+File.separator+"DB.xml");
		//TODO: Ask cheng which file I should use to get GUID etc");
		//TODO: "In fb source folder either: IEC61499.dfbproj or Database\\DB.xml");
		//TODO: Which is gauranteed to work? -- regardless of previous build history?");
		SAXBuilder sax = XMLParser.getSaxBuilder(false);
		try {
			Document dbxml = sax.build(db);
			Element IEC61499Database = dbxml.getRootElement();
			ProjectName = IEC61499Database.getAttributeValue("Name");
			ProjectGuid = IEC61499Database.getAttributeValue("Guid");
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	
	// Traverse a previously processed block type, but with new insName..
	// Add any _HMI stuff as required
	public static void processAnyHMIBlocks(FBType fbtype, String fullyQualifiedInsName) throws IOException {
		if( fbtype.getName().endsWith("_HMI") )
		{
			// To avoid 'Multiple Definitions' Error...
			// Find the FB :( 
			for(FunctionBlock fb : FunctionBlock.fbs)
			{
				if( fb.getCompiledType() == fbtype.getName())
				{
					printHMIStuff(fb, fullyQualifiedInsName, false); // false for 2nd instance
					break;
				}
			}
			
		}
		else if( fbtype.subBlocks != null )
		{
			
			for(int b = 0; b < fbtype.subBlocks.length; b++)
			{
				processAnyHMIBlocks(fbtype.subBlocks[b], fullyQualifiedInsName+"."+fbtype.composite.instances[b].name); // dot appended at the end cause thats what is done with an ancestor var in translate()
			}
		}
		
	}
	
	private static void prepareHMIConfigFiles() throws IOException
	{
		if( ProjectName == null )
		{
			parseNxtControlProject(NXTControlHelpers.sourceFolder);
		}
		if( hmiconfigh == null )
		{
			hmiconfigh = new FileWriter(FBtoStrl.opts.outputpath() + currentDeviceName + 
					"_HMIServerConfig.h", false);
			
			hmiconfigh.write("#ifndef _NXTHMISERVERCONFIG_H__"+CodePrinter.newLine);
			hmiconfigh.write("#define _NXTHMISERVERCONFIG_H__"+CodePrinter.newLine);
			hmiconfigh.write("#include \"NxtHMIServer.h\""+CodePrinter.newLine);
			//hmiconfigh.write("#include \"NxtHMIData.h\""+CodePrinter.newLine);
			
			
			hmiconfigh.write("#ifdef _MSC_VER"+CodePrinter.newLine);
			// TODO: used to be winsock.h but then I get linker probs
			hmiconfigh.write("#include <Winsock2.h>"+CodePrinter.newLine);
			hmiconfigh.write("#else"+CodePrinter.newLine);
			hmiconfigh.write("#include <sys/socket.h>"+CodePrinter.newLine);
			hmiconfigh.write("#endif"+CodePrinter.newLine);
			
			hmiconfigh.write("#include <stdio.h>"+CodePrinter.newLine);
			
			/*hmiconfigh.write("typedef struct"+CodePrinter.newLine);
			hmiconfigh.write("{"+CodePrinter.newLine);
			hmiconfigh.write("    unsigned int* linkids;"+CodePrinter.newLine);
			hmiconfigh.write("    unsigned int* cookies;"+CodePrinter.newLine);
			hmiconfigh.write("#ifdef _MSC_VER"+CodePrinter.newLine);
			hmiconfigh.write("    SOCKET* clientsockets;"+CodePrinter.newLine);
			hmiconfigh.write("#else"+CodePrinter.newLine);
			hmiconfigh.write("    int* clientsockets;"+CodePrinter.newLine);
			hmiconfigh.write("#endif"+CodePrinter.newLine);
			hmiconfigh.write("    unsigned int count;"+CodePrinter.newLine);
			hmiconfigh.write("}SubscriberList;"+CodePrinter.newLine);
			hmiconfigh.write(""+CodePrinter.newLine);
			hmiconfigh.write("typedef struct"+CodePrinter.newLine);
			hmiconfigh.write("{"+CodePrinter.newLine);
			hmiconfigh.write("#ifdef _MSC_VER"+CodePrinter.newLine);
			hmiconfigh.write("    SOCKET* remotesockets;"+CodePrinter.newLine);
			hmiconfigh.write("#else"+CodePrinter.newLine);
			hmiconfigh.write("    int* remotesockets;"+CodePrinter.newLine);
			hmiconfigh.write("#endif"+CodePrinter.newLine);
			hmiconfigh.write("    unsigned int count;"+CodePrinter.newLine);
			hmiconfigh.write("}PublisherList;"+CodePrinter.newLine);
			hmiconfigh.write(""+CodePrinter.newLine);*/
			hmiconfigh.write("#ifdef _MSC_VER"+CodePrinter.newLine);
			hmiconfigh.write("SubscriberList* addSubscriber(SubscriberList* subscriberNode, SOCKET* socket, unsigned int linkid, unsigned int cookie);"+CodePrinter.newLine);
			hmiconfigh.write("void subscribeSocket(NxtHMISubscribeMsg* req, SOCKET* socket);"+CodePrinter.newLine);
			hmiconfigh.write("#else"+CodePrinter.newLine);
			hmiconfigh.write("SubscriberList* addSubscriber(SubscriberList* subscriberNode, int* socket, unsigned int linkid, unsigned int cookie);"+CodePrinter.newLine);
			hmiconfigh.write("void subscribeSocket(NxtHMISubscribeMsg* req, int* socket);"+CodePrinter.newLine);
			hmiconfigh.write("#endif"+CodePrinter.newLine);
			
			hmiconfigh.write("void processUpdates();"+CodePrinter.newLine);
			hmiconfigh.write("int setData(NxtHMISetDataPathMsg* req);"+CodePrinter.newLine);
			
			hmiconfigh.write("// Project.Name"+CodePrinter.newLine);
			hmiconfigh.write("char* ProjectName;// = \""+ProjectName+"\";"+CodePrinter.newLine);
			hmiconfigh.write("char* ProjectState;// = \"Running\";"+CodePrinter.newLine);
			hmiconfigh.write("// GUID FROM <Project>\\IEC61499\\DatabaseDB.xml"+CodePrinter.newLine);
			hmiconfigh.write("char* ProjectGuid;// = \""+ProjectGuid+"\";"+CodePrinter.newLine);
			hmiconfigh.write("unsigned short HMIServerPort;// Default = 61498"+CodePrinter.newLine);
			
			
			// TODO: change to CodeGenerator.copyFile("NxtHMIServer.h")
			// and add all needed files in NxtHMIServer.h (@copyFile)
			// Copy the server files
			String[] filenames = {"NxtHMIServer.c","NxtHMIServer.h", "NxtHMIData.c", "NxtHMIData.h", "NxtCommunication.c", "NxtCommunication.h"};
			//
			for(String filename : filenames)
			{
				File infile = new File("resources"+File.separator+"sifb"+File.separator+"c"+File.separator+"nxtControl"+File.separator+filename);
				File outfile = new File(FBtoStrl.opts.outputpath() + filename);
				
				BufferedReader in = new BufferedReader(new FileReader(infile));
				      
				BufferedWriter out = new BufferedWriter(new FileWriter(outfile));
				CodeGenerator.plainCopyFile(out, in);			
			}
			BufferedReader in = new BufferedReader(new FileReader("resources"+File.separator+"sifb"+File.separator+"c"+File.separator+"PUBSUB"+File.separator+"pubsub.c"));
			BufferedWriter out = new BufferedWriter(new FileWriter(FBtoStrl.opts.outputpath() + "pubsub.c"));
			CodeGenerator.plainCopyFile(out, in);	
			in = new BufferedReader(new FileReader("resources"+File.separator+"sifb"+File.separator+"c"+File.separator+"PUBSUB"+File.separator+"pubsub.h"));
			out = new BufferedWriter(new FileWriter(FBtoStrl.opts.outputpath() + "pubsub.h"));
			CodeGenerator.plainCopyFile(out, in);	
			
			
		}
		if( hmiconfigc == null )
		{
			// Start .c
			hmiconfigc = new FileWriter(FBtoStrl.opts.outputpath() + currentDeviceName + 
					"_HMIServerConfig.c",false);
			
			hmiconfigc.write("#include \""+currentDeviceName+"_HMIServerConfig.h\"\n");
			hmiconfigc.write("\n");
			
			
			hmiconfigc.write("#ifdef _MSC_VER"+CodePrinter.newLine);
			hmiconfigc.write("    SubscriberList* addSubscriber(SubscriberList* subscriberNode, SOCKET* socket, unsigned int linkid, unsigned int cookie)\n");
			hmiconfigc.write("#else"+CodePrinter.newLine);
			hmiconfigc.write("    SubscriberList* addSubscriber(SubscriberList* subscriberNode, int* socket, unsigned int linkid, unsigned int cookie)\n");
			hmiconfigc.write("#endif"+CodePrinter.newLine);
			
			hmiconfigc.write("{\n");
			
			hmiconfigc.write("    SubscriberList* node = subscriberNode;\n");
			hmiconfigc.write("    // Traverse LinkedList to find empty str\n");
			hmiconfigc.write("    while(1)\n");
			hmiconfigc.write("    {\n");
			hmiconfigc.write("        if( node->linkid == 0 )\n");
			hmiconfigc.write("        {\n");
			hmiconfigc.write("            node->remotesocket = socket;\n");
			hmiconfigc.write("            node->linkid = linkid;\n");
			hmiconfigc.write("            node->cookie = cookie;\n");
			hmiconfigc.write("            break;\n");
			hmiconfigc.write("        }\n");
			hmiconfigc.write("        else if (node->next == NULL)\n");
			hmiconfigc.write("        {\n");
			hmiconfigc.write("            node->next = malloc(sizeof(SubscriberList));\n");
			hmiconfigc.write("            node->next->remotesocket = NULL;\n");
			hmiconfigc.write("            node->next->linkid = 0;\n");
			hmiconfigc.write("            node->next->cookie = 0;\n");
			hmiconfigc.write("            node->next->next = NULL;\n");
			hmiconfigc.write("        }\n");
			hmiconfigc.write("        //else\n");
			hmiconfigc.write("        node = node->next;\n");
			hmiconfigc.write("    }\n");
			hmiconfigc.write("    return node;\n");
			hmiconfigc.write("}\n");
			hmiconfigc.write("\n");
			
			
			// Start handleSub
			handleSub.append("#ifdef _MSC_VER"+CodePrinter.newLine);
			handleSub.append("void subscribeSocket(NxtHMISubscribeMsg* req, SOCKET* socket)\n");
			
			handleSub.append("#else"+CodePrinter.newLine);
			handleSub.append("void subscribeSocket(NxtHMISubscribeMsg* req, int* socket)\n");
			
			handleSub.append("#endif"+CodePrinter.newLine);
			handleSub.append("{\n");
			
			// Added for Sending value on sub
			handleSub.append("    SubscriberList* subscriber;\n");
			handleSub.append("    unsigned int offset = 0;\n");
			handleSub.append("    unsigned char output[256];\n");
		    
			handleSub.append("    unsigned char eventNumber = req->eventNumber & 0x7F; // clear bit 7\n");
			handleSub.append("    bool outputEvent = (req->eventNumber & 0x80); // get bit 7\n");
			handleSub.append("    req->eventNumber &= 0x1F; // get lower 5 bits\n");
			//handleSub.append("    printf(\"\tSubscribed to %s.Event[%d]\\n\",req->accessorPath, eventNumber);\n");
			handleSub.append("    if( outputEvent ){\n");
			handleSub.append("        printf(\"Dunno what to do for outputEvent.\\n\");");
			handleSub.append("        return;");
			handleSub.append("    }\n");


			
			
			// Start setData
			setData.append("int setData(NxtHMISetDataPathMsg* req)\n");
			setData.append("{\n");
			setData.append("    unsigned int var = 0;\n");
			setData.append("    unsigned char eventNumber = req->eventNumber & 0x7F; // clear bit 7 (which is '1' for inputevent)\n");
			//setData.append("    printf(\"setData for %s event: %d\\n\",req->accessorPath, eventNumber);\n");
			
			
			// Start processUpdates
			processUpdates.append("void processUpdates()\n");
			processUpdates.append("{\n");
			processUpdates.append("    SubscriberList* subscriber;\n");
			processUpdates.append("    unsigned int offset = 0;\n");
			processUpdates.append("    unsigned char output[256];\n");
		}
	}
	
	private static void addHMIInstance(FunctionBlock fb, String fullyQualifiedInsName) throws IOException
	{
		hmiUsed = true;
		
		// Add global vars
		String varName = fullyQualifiedInsName.replaceAll("\\.", "");
		String devQualifiedInsName = fullyQualifiedInsName.replaceAll(currentDeviceName+"\\.", "");

		for(Event e : fb.inputEvents)
		{
			hmiconfigh.write("HMIInputEvent "+varName+e.getName()+";\n");
			//hmiconfigh.write("IECValue "+fullyQualifiedInsName+e.getName()+"Pubdata["+e.withVar.length+"];\n");
		}
		
		for(Event e : fb.outputEvents)
		{
			hmiconfigh.write("HMIOutputEvent "+varName+e.getName()+";\n");
			//hmiconfigh.write("IECValue "+fullyQualifiedInsName+e.getName()+"Subdata["+e.withVar.length+"];\n");
			
		}
		
		// add to addSubscriber()
		handleSub.append("    if( strcmp(req->accessorPath, \""+devQualifiedInsName+"\") == 0 ){\n");
		handleSub.append("        switch(eventNumber)\n");
		handleSub.append("        {\n");
		for(int eNum = 1; eNum < fb.inputEvents.length; eNum++)
		{
			handleSub.append("            case "+(eNum-1)+":\n");
			handleSub.append("                subscriber = addSubscriber(&"+varName+fb.inputEvents[eNum].getName()+".subscribers, socket, getNewLinkID(), req->cookie);\n");
			handleSub.append("                offset = sendNotification(output, 0, subscriber->linkid, subscriber->cookie, "+varName+fb.inputEvents[eNum].getName()+".dataCount, "+varName+fb.inputEvents[eNum].getName()+".datain);\n");
			handleSub.append("                send(*subscriber->remotesocket,output,offset,0);\n");
			handleSub.append("                return;\n");
		}
		// Handle missing:
			handleSub.append("            default:\n");
			handleSub.append("                printf(\"[addSubscriber] Error: %s event %d not handled.\\n\", req->accessorPath, req->eventNumber);\n");
			handleSub.append("                break;\n");
		handleSub.append("        }\n");
		handleSub.append("        return;\n");
		handleSub.append("    }\n");
		
		// add to setData()
		setData.append("    if( strcmp(req->accessorPath, \""+devQualifiedInsName+"\") == 0 ){\n");
		setData.append("        switch(eventNumber)\n");
		setData.append("        {\n");
		for(int eNum = 1; eNum < fb.outputEvents.length; eNum++) // 1 to skip INIT & INITO
		{
			setData.append("            case "+(eNum-1)+":\n");
			setData.append("                (*"+varName+fb.outputEvents[eNum].getName()+".events) |= (1<<"+varName+fb.outputEvents[eNum].getName()+".eventNum);\n");
			setData.append("                for(var = 0; var < "+varName+fb.outputEvents[eNum].getName()+".dataCount; var++)\n");
			setData.append("                {\n");
			
            
			setData.append("                    "+varName+fb.outputEvents[eNum].getName()+".dataout[var].dataType = req->data[var].dataType;\n");
			setData.append("                    if( req->data[var].len == 0 )\n");
			setData.append("                        req->data[var].len = 1; // likely a bool\n");
			setData.append("                   	"+varName+fb.outputEvents[eNum].getName()+".dataout[var].len = req->data[var].len;\n");
			setData.append("                    memcpy("+varName+fb.outputEvents[eNum].getName()+".dataout[var].data, req->data[var].data,req->data[var].len);\n");
			setData.append("                }\n");
			setData.append("                return "+varName+fb.outputEvents[eNum].getName()+".dataCount;\n");
			
		}
		// Handle missing:
			setData.append("            default:\n");
			setData.append("                printf(\"[setData] Error: %s event %d not handled.\\n\", req->accessorPath, req->eventNumber);\n");
			setData.append("                break;\n");
		setData.append("        }\n");
		setData.append("        return 0;\n");
		setData.append("    }\n");
		
		
		// add to processUpdates()
		
		
		for(int eNum = 1; eNum < fb.inputEvents.length; eNum++)
		{
			processUpdates.append("    if( (*"+varName+fb.inputEvents[eNum].getName()+".events) & (1<<"+varName+fb.inputEvents[eNum].getName()+".eventNum) )\n");
			processUpdates.append("    {\n");
			processUpdates.append("        subscriber = &"+varName+fb.inputEvents[eNum].getName()+".subscribers;\n");
			processUpdates.append("        while(1)\n");
			processUpdates.append("        {\n");
			processUpdates.append("            if(subscriber->linkid == 0)\n");
			processUpdates.append("            {\n");
			processUpdates.append("                break;\n");
			processUpdates.append("            }\n");
			processUpdates.append("            else\n");
			processUpdates.append("            {\n");
			processUpdates.append("                offset = sendNotification(output, 0, subscriber->linkid, subscriber->cookie, "+varName+fb.inputEvents[eNum].getName()+".dataCount, "+varName+fb.inputEvents[eNum].getName()+".datain);\n");
			processUpdates.append("                send(*subscriber->remotesocket,output,offset,0);\n");
			
			processUpdates.append("                if(subscriber->next == 0 || subscriber->next == NULL )\n");
			processUpdates.append("                    break;\n");
			processUpdates.append("                subscriber = subscriber->next;\n");
			processUpdates.append("            }\n");
			processUpdates.append("        }\n");
			processUpdates.append("        (*"+varName+fb.inputEvents[eNum].getName()+".events) &= ~(1<<"+varName+fb.inputEvents[eNum].getName()+".eventNum);\n");
			processUpdates.append("    }\n");
		}
		
	}
	
	// Print global vars for use in NxtHMIServer
	public static void printHMIStuff(FunctionBlock fb, String fullyQualifiedInsName, boolean first) throws IOException
	{
		hmiUsed = true;
		
		if( hmiconfigc == null )
			startNewConfigFiles(FBtoStrl.currentDeviceName);
		
		// Add information to currentDeviceName+"_HMIServerConfig.c"
		prepareHMIConfigFiles();
		
		printHMIInterface(fb);
		
		addHMIInstance(fb, fullyQualifiedInsName);

		String initName = fullyQualifiedInsName.replaceAll(currentDeviceName+"\\.", ""); // The name of the instance relative to the top level device
		NXTControlHMIGenerator.pendingHMIInits.add(fullyQualifiedInsName.replaceAll("\\.", "")+"init(&"+initName+");");
		
		fullyQualifiedInsName = fullyQualifiedInsName.replaceAll("\\.", "");
		NXTControlHMIGenerator.pendingHMIInitDeclarations.add("void "+fullyQualifiedInsName+"init("+fb.getCompiledType()+"* me);");
		
		String fbName = fb.getCompiledType();
		
		//File hfile = new File(FBtoStrl.opts.outputPath+File.separator+fbName+".h");
		//FileWriter hprinter = new FileWriter(hfile, true);
		// Just append the declaration
		hmiconfigh.write("#include \""+fb.getCompiledType()+".h\"\n");
		hmiconfigh.write("void "+fullyQualifiedInsName+"init("+fb.getCompiledType()+"* me); // TODO: This overwrites the last declaration\n");
		//hprinter.close();
		
		
		if( first )
		{
			File cfile = new File(FBtoStrl.opts.outputpath() + fbName + ".c");
			FileWriter cprinter = new FileWriter(cfile, false);
			
			cprinter.write("#include \""+ fb.getCompiledType() +".h\"\n");
			cprinter.write("\n");
			cprinter.write("void "+fb.getCompiledType()+"run("+fb.getCompiledType()+"* me)\n");
			cprinter.write("{\n");
			cprinter.write("    me->_output.event.INITO = me->_input.event.INIT;\n");
			for(Event e: fb.outputEvents)
			{
			cprinter.write("    if( me->_preoutput.event."+e.getName()+" )\n");
			cprinter.write("        me->_output.event."+e.getName()+" = 0;\n");
			}
			
			cprinter.write("    me->_preoutput.events =  me->_output.events;\n");
			cprinter.write("}\n");
			cprinter.write("\n");
			cprinter.close();
		}
		
		hmiconfigc.write("void "+fullyQualifiedInsName+"init("+fb.getCompiledType()+"* me)\n");
		hmiconfigc.write("{\n");
		
		//hmiconfigc.write("    "+"memset(me, 0, sizeof("+fb.getCompiledType()+"));\n");
		// TODO: I Don;t like that I have to set INITO here...
		// eventindex appears to be after INIT & after INITO :(
		hmiconfigc.write("    "+"me->_output.event.INITO = 1; // Set the block's INITO flag to true... cleared on run\n");
		for(int eNum = 0; eNum < fb.inputEvents.length; eNum++)
		{
			Event e = fb.inputEvents[eNum];
			int dataCount = 0;
			if( e.withVar != null ) 
				dataCount = e.withVar.length;
			hmiconfigc.write("    "+"memset(&"+fullyQualifiedInsName+e.getName()+", 0, sizeof(HMIInputEvent));\n");
			hmiconfigc.write("\n");
			hmiconfigc.write("    "+fullyQualifiedInsName+e.getName()+".events = &me->_input.events;\n");
			hmiconfigc.write("    "+fullyQualifiedInsName+e.getName()+".eventNum = " + eNum + ";\n");
			hmiconfigc.write("    "+fullyQualifiedInsName+e.getName()+".dataCount = " + dataCount + ";\n");
			hmiconfigc.write("    "+fullyQualifiedInsName+e.getName()+".datain = malloc(sizeof(IECValuePtr) * " + dataCount + ");\n");
			hmiconfigc.write("\n");
			for(int v = 0; v < dataCount; v++)
			{
				hmiconfigc.write("    "+fullyQualifiedInsName+e.getName()+".datain["+v+"].dataType = IEC_"+ e.withVar[v].getType()+";\n");
				hmiconfigc.write("    "+fullyQualifiedInsName+e.getName()+".datain["+v+"].data = &me->_"+e.withVar[v].getName()+";\n");
			}
			hmiconfigc.write("\n");

			
		}
		
		for(int eNum = 0; eNum < fb.outputEvents.length; eNum++)
		{
			Event e = fb.outputEvents[eNum];
			hmiconfigc.write("    "+"memset(&"+fullyQualifiedInsName+e.getName()+", 0, sizeof(HMIOutputEvent));\n");
			hmiconfigc.write("\n");
			hmiconfigc.write("    "+fullyQualifiedInsName+e.getName()+".events = &me->_output.events;\n");
			hmiconfigc.write("    "+fullyQualifiedInsName+e.getName()+".eventNum = " + eNum + ";\n");
			hmiconfigc.write("    "+fullyQualifiedInsName+e.getName()+".dataCount = " + e.withVar.length + ";\n");
			hmiconfigc.write("    "+fullyQualifiedInsName+e.getName()+".dataout = malloc(sizeof(IECValuePtr) * " + e.withVar.length + ");\n");
			hmiconfigc.write("\n");
			
			for(int v = 0; v < e.withVar.length; v++)
			{
				hmiconfigc.write("    "+fullyQualifiedInsName+e.getName()+".dataout["+v+"].dataType = IEC_"+ e.withVar[v].getType()+";\n");
				hmiconfigc.write("    "+fullyQualifiedInsName+e.getName()+".dataout["+v+"].data = &me->_"+e.withVar[v].getName()+";\n");
			}
			hmiconfigc.write("\n");

			
		}
		
		hmiconfigc.write("}\n");
		hmiconfigc.write("\n");
		
	}

	static void printHMIInterface(FunctionBlock fb) throws IOException
	{
		String fbName = fb.getCompiledType();
		CodePrinter printer = new CodePrinter(FBtoStrl.opts.outputpath() + fbName + ".h");
		//InterfaceList iface = fb.getInterfaceList();
		
		printer.print("#ifndef "+fbName.toUpperCase()+"_H_");
		printer.print("#define "+fbName.toUpperCase()+"_H_");
		printer.print("");
		printer.print("#include \"fbtypes.h\""); // data types
		
		if (fb.inputEvents != null) {
			printer.print("typedef union {");
			printer.indent();
			printer.print("UDINT events;");
			printer.print("struct {");
			printer.indent();
			
			// Print out all input event declarations
			for (int i = 0; i < fb.inputEvents.length; i++) {
				String buf = "UDINT " + fb.inputEvents[i].getName() + " : 1;";
				if (fb.inputEvents[i].getComment() != null)
					buf = buf + " // " + fb.inputEvents[i].getComment();
				printer.print(buf);
			}
			
			printer.unindent();
			printer.print("} event;");
			printer.unindent();
			printer.print("} " + fbName + "IEvents;", 1);
		}

		// Print out all output event declarations
		if (fb.outputEvents != null) {
			printer.print("typedef union {");
			printer.indent();
			printer.print("UDINT events;");
			printer.print("struct {");
			printer.indent();
			for (int i = 0; i < fb.outputEvents.length; i++) {
				String buf = "UDINT " + fb.outputEvents[i].getName() + " : 1;";
				if (fb.outputEvents[i].getComment() != null)
					buf = buf + " // " + fb.outputEvents[i].getComment();
				printer.print(buf);
			}
			printer.unindent();
			printer.print("} event;");
			printer.unindent();
			printer.print("} " + fbName + "OEvents;", 1);
		}
		
		String comment = fb.getComment();
		if (comment != null) {
			if (!comment.isEmpty())
				printer.print("/* " + comment + "*/");
		}
		printer.print("typedef struct {");
		printer.indent();
		
		printer.print(fbName + "OEvents _output;");
		printer.print(fbName + "OEvents _preoutput;");
		
		printer.print(fbName + "IEvents _input;");
		
		
		// Print out all input data declarations and their internal registers
		if (fb.inputData != null) {
			for (int i = 0; i < fb.inputData.length; i++) {
				String[] str = CGenerator.makeDeclaration(fb.inputData[i], fb.getFQInstName());
				for (int j = 0; j < str.length; j++) {
					assert (str[j] != null): "Cannot make input data declaration.\n";
					printer.print(str[j] + ";");
				}
			}
		}
		
		// Print out all output data declarations and their internal registers
		if (fb.outputData != null) {
			for (int i = 0; i < fb.outputData.length; i++) {
				String[] str = CGenerator.makeDeclaration(fb.outputData[i], fb.getFQInstName());
				for (int j = 0; j < str.length; j++) {
					assert (str[j] != null): "Cannot make output data declaration.\n";
					printer.print(str[j] + ";");
				}
			}
		}
		
		printer.unindent();
		printer.print("} " + fbName + ";", 1);
		
		printer.print("void "+fbName + "run("+fbName+"* me);", 2);
		
		printer.print("#endif");
		
		printer.close();
	}
	
	public static void printHMIInits(CodePrinter printer) throws IOException
	{
		for(String init : pendingHMIInits)
		{
			printer.print(init);
		}
		pendingHMIInits.clear();
		
	}
	
	public static void printHMIInitDeclarations(CodePrinter printer) throws IOException
	{
		for(String declaration : pendingHMIInitDeclarations)
		{
			printer.print("extern "+declaration);
		}
		pendingHMIInitDeclarations.clear();
		
	}
	
	public static void startNewConfigFiles(String devName) throws IOException
	{
		if( hmiconfigc != null )
			closeDevHMIFile();
		
		currentDeviceName = devName;
		extraCFiles[3] = currentDeviceName+"_HMIServerConfig.c";
		
		prepareHMIConfigFiles();
	}
	
	private static void closeDevHMIFile() throws IOException
	{
		if( hmiconfigc != null )
		{
			handleSub.append("}\n\n");
			setData.append("\tprintf(\"[setData] WARNING: %s Unhandled\\n\",req->accessorPath);\n");
			setData.append("\treturn 0;\n\n");
			setData.append("}\n\n");
			processUpdates.append("}\n\n");
			
			hmiconfigc.write(handleSub.toString());
			hmiconfigc.write(setData.toString());
			hmiconfigc.write(processUpdates.toString());
			
			handleSub.setLength(0);
			setData.setLength(0);
			processUpdates.setLength(0);
			
			hmiconfigc.close();
			hmiconfigc = null;
		}
	}
	
	public static void closeConfigFiles() throws IOException
	{
		if( hmiconfigh != null )
		{
			hmiconfigh.write("#endif\n");
			hmiconfigh.close();
			hmiconfigh = null;
		}
		
		closeDevHMIFile();
	}
}
