package ccode;

import fbtostrl.FBtoStrl;
import ir.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class NXTControlInterResourceCommGenerator {
	public static boolean interRESUsed = false;
	//public static String[] extraCFiles = {"NxtCommunication.c"};
	public static LinkedList<String> extraCFiles = new LinkedList<String>(); // TODO: This is temp so we can add uniqueblock names to makefile
	public static LinkedList<String> extraMainInits = new LinkedList<String>();
	
	private static HashMap<String,String> dataWireIDs = new HashMap<String,String>();
	private static LinkedList<String> eventWireIDs = new LinkedList<String>();
	
	//public static HashMap<String, String> linkPubCodes = new HashMap<String,String>();
	public static HashMap<String, Instance> linkFBs = new HashMap<String,Instance>();
	
	// Set LinkPub COMMAND
	private static String udprecvFileName;
	
	public static void addCFile(String cFile)
	{
		extraCFiles.add(cFile);
	}

	public static String getBlockName(String fileName) 
	{
		if( fileName.contains("LINKPUBLISHER") )
			return "LINKPUBLISHER";
		if( fileName.contains("LINKDATARECEIVER") )
			return "LINKDATARECEIVER";
		if( fileName.contains("LINKEVENTRECEIVER") )
			return "LINKEVENTRECEIVER";
		return null;
	}
	
	public static String getUniqueBlockName(String tN) 
	{
		String typeName = tN;
		/*if( typeName.contains("LINKDATARECEIVER") )
		{
			typeName += "_"+SIPath.getUniqueCount();
		}
		else if( typeName.contains("LINKEVENTRECEIVER") )
		{
			typeName += "_"+SIPath.getUniqueCount();
		}
		else if( typeName.contains("LINKPUBLISHER") )
		{
			typeName += "_"+SIPath.getUniqueCount();
		}
		*/
		return typeName;
	}
	
	public static void clearExtraCFiles()
	{
		extraCFiles.clear();
		extraCFiles.add("NxtCommunication.c");
	}
	
	public static void addChannel(String resName, String sourceName, String channelInstance)
	{
		
		if( extraMainInits.size() == 0 )
		{
			interRESUsed = true;
			clearExtraCFiles();
			extraCFiles.add("COMMCHANNELUDPRECV_"+sourceName+".c");
			extraMainInits.clear();
			udprecvFileName = FBtoStrl.opts.outputpath() + "COMMCHANNELUDPRECV_" + 
					sourceName + ".c";
			extraMainInits.add("// Init global var thing");
			extraMainInits.add("memset(&commChannels,0, sizeof(COMMCHANNELLinkedList));");
			extraMainInits.add("strcpy(sourceName, \""+sourceName+"\");");
		}
		extraMainInits.add("addChannel(&"+resName+"."+channelInstance+");"); // addChannel(&me->DEV1_RES0); for COMMCHANNELs
	}
	
	public static void printCommInits(CodePrinter printer) throws IOException
	{
		for(String init : extraMainInits)
		{
			printer.print(init);
		}
		extraMainInits.clear();
		
	}
	
	public static void addDataWireID(String wireid, String type)
	{
		/* // 	add with type
		String arrayString = sig.getSignal().substring(1, sig.getSignal().length()-1);
		String[] ids = arrayString.split(",");
		for(int i = 0; i < ids.length; i++)
		{
			if( !dataWireIDs.containsKey(ids[i]) )
				dataWireIDs.put(ids[i], types[i]);
		}
		*/
		dataWireIDs.put(wireid, type);
	}
	
	public static void addEventWireIDs(String param)
	{
		// split string
		String arrayString = param.substring(1, param.length()-1);
		String[] ids = arrayString.split(",");
		for(String id : ids)
		{
			if( !eventWireIDs.contains(id) )
				eventWireIDs.add(id);
		}
	}
	
	public static void addLinkPub(String name, Instance fb, boolean isAdapter)
	{
		//linkPubCodes.put(name, isAdapter ? "0x87" : "0x82");
		SIPath.addToVarList(fb.getFQName(), "CommandType", isAdapter ? "0x87" : "0x82", "");
		linkFBs.put(name, fb);
	}
	
	public static void printDynamicCommUdpReceive() throws IOException
	{
		
		BufferedWriter commudpprinter = new BufferedWriter(new FileWriter(udprecvFileName));
		commudpprinter.write("#include \"NxtCommunication.h\"\n");
		
		commudpprinter.write("\n");
		commudpprinter.write("/**********************************************************************************************************\n");
		commudpprinter.write("VARIABLES:\n");
		commudpprinter.write("**********************************************************************************************************/\n");
		commudpprinter.write("\n");
		
			for(String id : dataWireIDs.keySet())
			{
				commudpprinter.write("void* DATA_"+id+";\n");
			}
			commudpprinter.write("\n");
			for(String id : eventWireIDs)
			{
				commudpprinter.write("OutputEvent EVENT_"+id+";\n");
			}
		commudpprinter.write("\n");
		commudpprinter.write("/**********************************************************************************************************\n");
		commudpprinter.write("Dynamically Generated Functions:\n");
		commudpprinter.write("**********************************************************************************************************/\n");
		commudpprinter.write("int CommChannelHandleFBData(unsigned long long chanid, char* source, unsigned char *inbuf, int len, int o)\n");
		commudpprinter.write("{\n");
			commudpprinter.write("int offset = o;\n");
			commudpprinter.write("int tempoffset = o;\n");
			commudpprinter.write("IECValue eventidV, tempV;\n");
			commudpprinter.write("unsigned long long eventid = 0, dataid = 0;\n");
			commudpprinter.write("// EventID\n");
			commudpprinter.write("offset = decodeNxtPacketValue(inbuf, offset, &eventidV);\n");
			commudpprinter.write("if( offset == -1 )\n");
				commudpprinter.write("return -1;\n");
			commudpprinter.write("memcpy(&eventid,&eventidV.data,8);\n");
			commudpprinter.write("\n");			
			commudpprinter.write("// Reserved\n");
			commudpprinter.write("offset = decodeNxtPacketValue(inbuf, offset, &tempV);\n");
			commudpprinter.write("if( offset == -1 )\n");
				commudpprinter.write("return -1;\n");
			commudpprinter.write("\n");			
			
			// Loop getting data
			commudpprinter.write("while(true)\n");	
			commudpprinter.write("{\n");	
			// Get DataWireID (but check it)
			commudpprinter.write("	if( offset+1 >= len ) // Ensure data left\n");
			commudpprinter.write("	    break;\n");
			commudpprinter.write("	// DataID\n");	
			commudpprinter.write("	tempoffset = decodeNxtPacketValue(inbuf, offset, &tempV);\n");	
			commudpprinter.write("	if( tempoffset == -1 ) // error caused by end of packet?\n");	
			commudpprinter.write("		break;\n");	
			commudpprinter.write("		\n");	
			commudpprinter.write("	if( tempV.dataType != IEC_ULINT ) // If it isn't a DataID...\n");	 
			commudpprinter.write("		break; // fin.. go set the events\n");	
			commudpprinter.write("		\n");	
			commudpprinter.write("	offset = tempoffset;\n");	
			commudpprinter.write("	memcpy(&dataid,&tempV.data,8);\n");	
			commudpprinter.write("	\n");	
			// Get the value
			commudpprinter.write("	// Get Value\n");	
			commudpprinter.write("	offset = decodeNxtPacketValue(inbuf, offset, &tempV);\n");	
			commudpprinter.write("	if( offset == -1 )\n");	
			commudpprinter.write("		return -1;\n");
			
			// Assign the data-value
			for(String id : dataWireIDs.keySet())
			{
			commudpprinter.write("    if(dataid == "+id+")\n");
			commudpprinter.write("    {\n");
			commudpprinter.write("        memcpy(DATA_" + id + ",&tempV.data,sizeof(" + dataWireIDs.get(id) + "));\n");
			commudpprinter.write("    }\n");
			}
			commudpprinter.write("}\n");
			commudpprinter.write("\n");
			
			commudpprinter.write("// Set Event\n");
			for(String id : eventWireIDs)
			{
			commudpprinter.write("    if(eventid == "+id+")\n");
			commudpprinter.write("    {\n");
				commudpprinter.write("        (*EVENT_"+id+".events) |= (1 << EVENT_"+id+".eventNum);\n");
			commudpprinter.write("    }\n");
			}
			commudpprinter.write("\n");
			commudpprinter.write("    return offset;\n");
		commudpprinter.write("}\n");
		commudpprinter.write("\n");
		commudpprinter.write("int CommChannelHandleAdapterData(unsigned long long chanid, char* source, unsigned char *inbuf, int o)\n");
		commudpprinter.write("{//TODO:\n");
			commudpprinter.write("int offset = o;\n");
			commudpprinter.write("\n");
			commudpprinter.write("return offset;\n");
		commudpprinter.write("}\n");
		commudpprinter.write("\n");
		
		commudpprinter.write("void CommChannelClearEvents()\n");
		commudpprinter.write("{\n");
		if( eventWireIDs.size() > 0 )
		{
			commudpprinter.write("    if( EVENT_"+eventWireIDs.get(0)+".events == NULL ) return;\n");
			for(String id : eventWireIDs)
			{
				commudpprinter.write("    (*EVENT_"+id+".events) &= ~(1 << EVENT_"+id+".eventNum);\n");
			}
		}
		commudpprinter.write("}\n");
		
		/*
		// Each LinkPub:
		for(String lPubName : linkFBs.keySet())
		{
			Instance lPub = linkFBs.get(lPubName);
			
			
			commudpprinter.write("int "+lPubName+"packdata("+lPubName+"* me)\n");
			commudpprinter.write("{\n");
			commudpprinter.write("    int offset = 0;\n");
			commudpprinter.write("    unsigned int tempV = 0;\n");
			commudpprinter.write("    int di = 0;\n");
			//commudpprinter.write("    // For now inputs are also embedded in ANY :|\n");
			commudpprinter.write("// Version\n");
			commudpprinter.write("tempV = 2;\n");
			commudpprinter.write("\n");
			commudpprinter.write("offset = appendNxtPacketValue(me->buffer, offset, IEC_USINT, (void*)&tempV);\n");
			commudpprinter.write("if( offset <= 0 )\n");
			commudpprinter.write("	return -1;\n");
			commudpprinter.write("	\n");
			commudpprinter.write("//Source\n");
			commudpprinter.write("offset = appendNxtPacketValue(me->buffer, offset, IEC_STRING, (void*)sourceName);\n");
			commudpprinter.write("if( offset <= 0 )\n");
			commudpprinter.write("	return -1;\n");
			commudpprinter.write("	\n");
			commudpprinter.write("//Command Type\n");
			commudpprinter.write("tempV = "+linkPubCodes.get(lPubName)+";\n");
			commudpprinter.write("offset = appendNxtPacketValue(me->buffer, offset, IEC_USINT, (void*)&tempV);\n");
			commudpprinter.write("if( offset <= 0 )\n");
			commudpprinter.write("	return -1;\n");
			commudpprinter.write("\n");
			commudpprinter.write("offset = appendNxtPacketValue(me->buffer, offset, IEC_ULINT, (void*)&me->_CHANID);\n");
			commudpprinter.write("if( offset <= 0 )\n");
			commudpprinter.write("	return -1;\n");
			commudpprinter.write("\n");
			commudpprinter.write("\n");
			commudpprinter.write("    offset = appendNxtPacketValue(me->buffer, offset, IEC_ULINT, (void*)&me->_EVENTWIREID);\n");
			commudpprinter.write("    if( offset <= 0 )\n");
			commudpprinter.write("        return -1;\n");
			commudpprinter.write("\n");
			commudpprinter.write("    // Reserved\n");
			commudpprinter.write("    offset = appendNxtPacketValue(me->buffer, offset, IEC_UDINT, (void*)&tempV);\n");
			commudpprinter.write("    if( offset <= 0 )\n");
			commudpprinter.write("        return -1;\n");
			commudpprinter.write("\n");
			commudpprinter.write("    // VARIABLE\n");
			
			LinkedList<String> inputTypes = new LinkedList<String>();
			for(Signal sig : lPub.getSignalSet())
			{
				if( sig.getPortType() == PortType.INPUT && sig.getSignal().startsWith("SD_"))
				{
					inputTypes.add(sig.getSigType());
				}
			}
			for(int di = 0; di < inputTypes.size(); di++)
			{
				commudpprinter.write("        // Data wire id\n");
				commudpprinter.write("        offset = appendNxtPacketValue(me->buffer, offset, IEC_ULINT, (void*)&me->_DATAWIREIDS["+di+"]);\n");
				commudpprinter.write("        if( offset <= 0 )\n");
				commudpprinter.write("            return -1;\n");
				commudpprinter.write("\n");
				commudpprinter.write("        // Value\n");
				commudpprinter.write("        offset = appendNxtPacketValue(me->buffer, offset, IEC_"+inputTypes.get(di)+", (void*)&me->_SD_"+(di+1)+");\n");
				commudpprinter.write("        if( offset <= 0 )\n");
				commudpprinter.write("            return -1;\n");
				
			}
			commudpprinter.write("    // END VAR\n");
			commudpprinter.write("    return offset;\n");
			commudpprinter.write("}\n");
			commudpprinter.write("\n");
		}
		//*/
		commudpprinter.close();
		commudpprinter = null;
		
		eventWireIDs.clear();
		dataWireIDs.clear();
		
	}

}
