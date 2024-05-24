// TODO: PretC.Compile(String fbName, String ReactiveInterface, String PretCCode, 
//       String outputPath, String instanceName, String runXMLCode)

/* FBtoStrl.java
 * Transforms function block description into an equivalent Esterel program.
 *
 * Written by: Li Hsien Yoong
 */

// TODO: [gareth] Note to self: I thought I had done adapters (mostly)... but I don't see any code to parse Adapter connections From composite blocks ?!?!?
package fbtostrl;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.jdom2.*;
import org.json.JSONObject;
import org.json.JSONTokener;

import pretc.PRETCGenerator;
//import org.jdom2.output.XMLOutputter;

import fb.*;
import fb.Socket;
import ir.*;
import ccode.*;
import d3.D3Generator;
import ttpos.TTPGenerator;
import ucos.UCOSGenerator;
import esterel.*;
import rmc.*;

public class FBtoStrl {
	public static Options opts = new Options();

	// List of all deferred program generators
	public static LinkedHashMap<String, ProgramInfo> progGenerators;
	// Set of FBTypes that have already been processed
	public static HashSet<FBType> processedFBTypes = new HashSet<FBType>();
	// Set of communication blocks that will result in global variables
	private static HashSet<Block> communicationBlocks = new HashSet<Block>();
	private static ArrayList<String> searchPath = new ArrayList<String>();
	private static LinkedList<String> outputFiles = new LinkedList<String>();
	public static ArrayList<String> headerFiles = new ArrayList<String>();
	public static ArrayList<String> libraryFiles = new ArrayList<String>();
	public static String startUpFile = ""; 
	public static String makeConfigFile = ""; 
	public static Map<String, Object> makeConfig = new HashMap<String, Object>();
	public static HashMap<String, String> anyTypeMap = new HashMap<String, String>(); // FQInstanceName.Port,
																						// Type
	public static HashMap<String, String> varSizeMap = new HashMap<String, String>(); // FQInstanceName.CountName,
																						// Value

	private static String parent;
	public static String topLevelFile;
	public static String inputFileName;
	public static String currentDeviceName = "";
	public static String currentDeviceType = "";
	public static String currentResourceName = "";


	// PretC
	public static boolean pretCUsed = false;

	public static FBType getBlock(String name) {
		for (FBType fbt : FBtoStrl.processedFBTypes) {
			if (fbt.name.equalsIgnoreCase(name))
				return fbt;
		}
		return null;
	}

	public void addToSearchPath(String path) {
		searchPath.add(path);
	}

	/*
	 * DESCRIPTION: Resolves the data type of a communication block's data port
	 * PARAMETER: instance - instance name of the local communication block
	 * blockType - type of local communication block index - port number
	 * RETURNS: Local communication block type
	 */
	public static DataType getAnyDataType(String instance, String blockType,
			int index) {
		Iterator<Block> i = communicationBlocks.iterator();
		while (i.hasNext()) {
			Block block = i.next();
			if (block.getInstanceName().equals(instance)
					&& block.getInstanceType().startsWith(blockType)) {
				if (blockType.equals("SEND_"))
					return block.getPredDataType("SD_"
							+ Integer.toString(index));
				else if (blockType.equals("RECV_"))
					return block.getSuccDataType("RD_"
							+ Integer.toString(index));
			}
		}
		return null;
	}

	/*
	 * DESCRIPTION: Checks if a given function block file exists PARAMETER: file
	 * - path of the function block file fileName - name of the function block
	 * file RETURNS: Path of the function block file if the file exists; null
	 * otherwise
	 */
	static File isFBFileExists(File file, String fileName) {
		if (!file.exists()) {
			ListIterator<String> i = searchPath.listIterator();
			while (i.hasNext()) {
				String path = i.next();
				file = new File(path + fileName);
				if (file.exists())
					return file;
			}
			return null;
		}
		return file;
	}

	/**
	 * Recursively visits all function blocks to translate them.
	 * 
	 * @param input
	 *            input file to be parsed
	 * @param insName
	 *            name of the instance being translated
	 * @param parentsName
	 *            name of the parent function block
	 * @param adapterType
	 *            type of adapter interface --Gareth added adapterType :| -
	 *            Change to something like 'ExpectedFBtype'??
	 * @param params
	 *            relevant parameters for the translation
	 * @param targetType
	 *            concrete host type to be translated to
	 * @return Function block type that has been translated
	 */
	public static FBType translate(String input, String insName,
			String parentsName, String adapterType, Parameter[] params,
			String targetType) throws IOException {
		// Create base name for output files
		String baseName = opts.outputpath()
				+ CodeGenerator.removeFileExt(CodeGenerator.getFileName(input));
		if (opts.isNxtControl() && baseName.endsWith("COMMCHANNEL")) {
			NXTControlInterResourceCommGenerator.addChannel(
					currentResourceName, currentDeviceName + "_"
							+ currentResourceName, insName);
		}

		FBType fbtype = null;

		File file = new File(input);
		String fileName = file.getName();
		file = isFBFileExists(file, fileName);
		if (file == null) {
			if (opts.isNxtControl()) {
				if (fileName.matches("[A-Z_]+_-?\\d+\\.fbt")) {
					// For nxtStudio library SIFBs
					// TODO: maybe I don't need to do this completely this way
					// parse the .gfbt file???
					fileName = fileName.substring(0, fileName.lastIndexOf('_'))
							+ ".fbt";
					baseName = baseName.substring(0, baseName.lastIndexOf('_')); // REMEMBER
																					// to
																					// Append
																					// Unique
																					// ID
				}
				// Note: nxtStudio 1.5+ has new folder structure for Systems
				// Turns out we can ignore it just by changing the String parent
				// to be the actual parent dir of all things
				/*
				 * else if( fileName.matches("[^\\.]+.sys") ) { // For nxtStudio
				 * library SIFBs // System files are in sub dir with the same
				 * name as system name }
				 */
				else {
					String nxtBlockName = NXTControlInterResourceCommGenerator
							.getBlockName(fileName);
					if (nxtBlockName != null) {
						fileName = fileName.substring(0,
								fileName.lastIndexOf(nxtBlockName)
										+ nxtBlockName.length())
								+ ".fbt";
						baseName = baseName.substring(0,
								baseName.lastIndexOf(nxtBlockName)
										+ nxtBlockName.length()); // REMEMBER to
																	// Append
																	// Unique ID
					}
				}
				file = new File(input);
				file = isFBFileExists(file, fileName);
			}
			if (file == null) {
				OutputManager.printError("", fileName
						+ ": File cannot be found.", OutputLevel.FATAL);
				System.exit(0);
			}
		}

		Document xml = XMLParser.parse(file.getPath(), true);
		if (xml == null) {
			OutputManager.printError("", file.getPath()
					+ ": Parsing failed to produce Document object.",
					OutputLevel.FATAL);
			System.exit(0);
		}
		if (opts.verbose) {
			OutputManager.printNotice(parentsName + insName, "Translating: "
					+ file.getAbsolutePath(), OutputLevel.INFO);
		}

		Element root = xml.getRootElement();
		String rootName = root.getName();
		if (rootName.equals("FBType")) {
			String name = root.getAttributeValue("Name");
			String subType = root.getAttributeValue("SubType"); // an optional
																// Attribute
																// added for
																// TimeMe
			Iterator<FBType> it = processedFBTypes.iterator();

			// If this FBType has been processed, don't need to do it again -
			// reuse
			// previous copy
			while (it.hasNext() && opts.pretc == false) { //dont resue if PRET-C
				fbtype = it.next();
				if (name.equals(fbtype.getName())) {
					if (opts.isNxtControl()) { // opts.ccode is implied
						NXTControlHMIGenerator.processAnyHMIBlocks(fbtype,
								parentsName + insName);
					}

					// If ANY type / @CNT / requires global variables... must
					// have unique
					// FunctionBlock! We can probably use the same FBT though.
					if (fbtype.needsUnique() || fbtype.isPretC()
							|| FBType.getGlobalVarType(name) != null) {
						fbtype = fbtype.clone();
						CodeGenerator cg = toCode(fbtype, baseName, insName,
								parentsName, null, params, null);
						if (cg != null)
							fbtype.addDeferredBlock(cg);
					} else {
						// Check if this fbtype has been generated in the
						// current output
						// folder.
						for (String output : outputFiles) {
							String noExtension = CodeGenerator
									.removeFileExt(output);
							if (baseName.equals(noExtension))
								return fbtype;
						}

						// Otherwise, copy the corresponding file from a
						// previous folder
						// to the current output folder.
						String[] newFiles = new String[2];
						int fileCount = 0;
						for (String output : outputFiles) {
							String noExtension = CodeGenerator
									.removeFileExt(CodeGenerator
											.getFileName(output));
							if (CodeGenerator.getFileName(baseName).equals(
									noExtension)) {
								String newFile = baseName
										+ CodeGenerator.getFileExt(output);
								BufferedWriter out = null;
								BufferedReader in = null;
								try {
									out = new BufferedWriter(new FileWriter(
											newFile));
									in = new BufferedReader(new FileReader(
											output));
								} catch (FileNotFoundException e) {
									OutputManager.printError(name, output
											+ ": Could not be opened.\n",
											OutputLevel.FATAL);
									System.exit(-1);
								} catch (IOException e) {
									OutputManager.printError(name, newFile
											+ ": Could not be opened.\n",
											OutputLevel.FATAL);
									System.exit(-1);
								}
								CodeGenerator.plainCopyFile(out, in);
								newFiles[fileCount] = newFile;
								fileCount++;
								if (fileCount >= 2)
									break;
							}
						}
						for (int i = 0; i < newFiles.length; i++)
							outputFiles.add(newFiles[i]);
					}
					return fbtype;
				}
			}

			String[] classdefs = getCompilerClassDefs(root
					.getChild("CompilerInfo"));
			if (classdefs != null) {
				for (String classdef : classdefs)
					if (classdef.length() > 0)
						CodeGenerator.srcDependencies.add(classdef);
			}

			String[] headers = getCompilerHeaders(root.getChild("CompilerInfo"));
			InterfaceList iface = getInterfaceList(root
					.getChild("InterfaceList"));
			Element fb = null;
			if ((fb = root.getChild("FBNetwork")) != null) {
				fbtype = makeFBNetwork(fb, name,
						root.getAttributeValue("Comment"), headers, iface,
						insName, parentsName, baseName, params, null);

			} else if ((fb = root.getChild("PretC")) != null) {
				PretC pretc = getPretC(fb, iface);
				fbtype = new FBType(name, root.getAttributeValue("Comment"),
						headers, iface, pretc);
				toCode(fbtype, baseName, insName, parentsName, null, params,
						null);
			} else if ((fb = root.getChild("Service")) != null
					|| (fb = root.getChild("BasicFB")) == null
					|| root.getChild("BasicFB").getChildren().size() == 0) {
				if (opts.isNxtControl())
					name = NXTControlInterResourceCommGenerator
							.getUniqueBlockName(name);

				fbtype = new FBType(name, root.getAttributeValue("Comment"),
						headers, iface);
				CodeGenerator cg = toCode(fbtype, baseName, insName,
						parentsName, null, params, null);
				if (cg != null)
					fbtype.addDeferredBlock(cg);
			} else {
				BasicFB basicFB = getBasicFB(fb, iface);
				fbtype = new FBType(name, root.getAttributeValue("Comment"),
						headers, iface, basicFB);

				CodeGenerator cg = toCode(fbtype, baseName, insName,
						parentsName, null, params, null);
				if (cg != null)
					fbtype.addDeferredBlock(cg);
			}
			fbtype.setSubType(subType);
			processedFBTypes.add(fbtype);
		} else if (rootName.equals("AdapterType")) {
			String name = root.getAttributeValue("Name");
			String comment = root.getAttributeValue("Comment");
			Iterator<FBType> it = processedFBTypes.iterator();

			String[] headers = getCompilerHeaders(root.getChild("CompilerInfo"));
			InterfaceList iface = getInterfaceList(root
					.getChild("InterfaceList"));

			if (adapterType.equalsIgnoreCase("Socket")) {
				// If this FBType has been processed, don't need to do it again
				// - reuse
				// previous copy
				while (it.hasNext()) {
					fbtype = it.next();
					if (fbtype.getName().equals(name + "Socket")) {
						return fbtype;
					}
				}
				// Socket is default
				AdapterType adp = new AdapterType(name + "Socket", comment,
						headers, iface);
				CodeGenerator cg = toCode(adp, baseName + "Socket", insName,
						parentsName, null, params, null);
				if (cg != null) {
					fbtype.addDeferredBlock(cg);
				}
				processedFBTypes.add(adp);
				return adp;
			} else if (adapterType.equalsIgnoreCase("Plug")) {
				// If this FBType has been processed, don't need to do it again
				// - reuse
				// previous copy
				while (it.hasNext()) {
					fbtype = it.next();
					if (fbtype.getName().equals(name + "Plug")) {
						return fbtype;
					}
				}

				// Plug has interface reversed
				Event[] tempE = iface.eventInputs;
				iface.eventInputs = iface.eventOutputs;
				iface.eventOutputs = tempE;

				VarDeclaration[] tempV = iface.inputVars;
				iface.inputVars = iface.outputVars;
				iface.outputVars = tempV;

				AdapterType adp = new AdapterType(name + "Plug", comment,
						headers, iface);
				CodeGenerator cg = toCode(adp, baseName + "Plug", insName,
						parentsName, null, params, null);
				if (cg != null) {
					fbtype.addDeferredBlock(cg);
				}

				processedFBTypes.add(adp);
				return adp;
			}
		} else if (rootName.equals("ResourceType")) {
			fbtype = makeResource(root, root.getAttributeValue("Name"),
					insName, baseName, params, targetType);
		} else if (rootName.equals("DeviceType")) {
			makeDevice(root, "", baseName, null);
		} else if (rootName.equals("System")) {
			if (opts.isD3())
				progGenerators = new LinkedHashMap<String, ProgramInfo>();
			List<?> devices = root.getChildren("Device");
			Iterator<?> i = devices.iterator();
			while (i.hasNext()) {
				Element dev = (Element) i.next();
				makeSysDevice(dev, dev.getAttributeValue("Name"), baseName);
				if (opts.isNxtControl())
					NXTControlHMIGenerator.closeConfigFiles();
			}
			if (opts.isCCode()||opts.pretc) {
				// Create top Makefile
				if (!opts.isNoMake())
					CProgGenerator.createMakefile(opts, devices);
			}
			if (opts.isD3()) {
				D3Generator.generateAllCode(progGenerators);
			}
		} else if (rootName.equals("DataType")) {
			// leave and return fbtype = null;
			Element dt = null;
			String name = root.getAttributeValue("Name");
			if ((dt = root.getChild("StructuredType")) != null) {
				makeStructuredType(name, dt);
			} else if ((dt = root.getChild("EnumeratedType")) != null) {
				makeEnumeratedType(name, dt);
			}
		} else {
			OutputManager.printError("", file.getPath() + ": Root element `"
					+ rootName + "\' is unrecognized.", OutputLevel.FATAL);
			System.exit(0);
		}

		return fbtype;
	}

	private static void makeStructuredType(String name, Element dt)
			throws IOException {
		VarDeclaration[] vars = getVars(dt);
		if (opts.ccode || opts.pretc) {
			CGenerator.generateDataType(name, vars);
		}
		IEC61131Types.addStructuredType(name, vars);
	}

	private static void makeEnumeratedType(String name, Element dt)
			throws IOException {
		EnumeratedValue[] values = getEnumValues(dt);
		if (opts.ccode || opts.pretc) {
			CGenerator.generateDataType(name, values);
		}
	}

	/**
	 * Creates a function block network.
	 * 
	 * @param net
	 *            root element describing the network
	 * @param name
	 *            name of the function block type
	 * @param comment
	 *            comment for the function block network
	 * @param headers
	 *            header files for the compiler to generate
	 * @param iface
	 *            function block interface list
	 * @param insName
	 *            name of the function block instance being translated
	 * @param parentsName
	 *            name of the parent function block
	 * @param baseName
	 *            path to generate code to
	 * @param params
	 *            relevant parameters for the translation
	 * @param targetType
	 *            concrete host type to be translated to
	 * @return the resource type created; null otherwise
	 */
	private static FBType makeFBNetwork(Element net, String name,
			String comment, String[] headers, InterfaceList iface,
			String insName, String parentsName, String baseName,
			Parameter[] params, String targetType) throws IOException {
		FBNetwork compositeFB = getFBNetwork(net);
		
		ArrayList<FBType> fbs = new ArrayList<FBType>();
		String ancestor = insName.isEmpty() ? "" : parentsName + insName + ".";

		if (compositeFB.instances != null) {
			// Check for error in connections
			if (compositeFB.dataConnections != null) {
				for (Connection dataC : compositeFB.dataConnections) {
					for (Connection dataCTest : compositeFB.dataConnections) {
						if (!dataC.source.equals(dataCTest.source)) {
							if (dataC.destination.equals(dataCTest.destination)) {
								OutputManager.printError(name,
										"Multiple Data Connections to port "
												+ dataCTest.destination + ".",
										OutputLevel.FATAL);
								System.exit(-1);
							}
						}
					}
				}
			}

			for (int i = 0; i < compositeFB.instances.length; i++) {
				FBInstance instance = compositeFB.instances[i];
				FBType fbt = translate(parent + instance.type + ".fbt",
						instance.name, ancestor, instance.type,
						instance.params, null);
				if (fbt != null) {
					if (fbt.isSIFB() && opts.isNxtControl())
						instance.type = fbt.name; // correct in the case of many
													// SIFBs :|
					fbs.add(fbt);
				}
			}
		}

		// For Adapters:
		if (iface != null) { // resources!!!!
			if (iface.sockets != null || iface.plugs != null) {
				// Update instances array
				Vector<FBInstance> instances = new Vector<FBInstance>();
				// Copy existing instances
				if (compositeFB.instances != null) {
					for (FBInstance inst : compositeFB.instances)
						instances.add(inst);
				} else
					compositeFB.instances = new FBInstance[1];

				if (iface.sockets != null) {
					for (Socket sock : iface.sockets) {
						AdapterType adp = (AdapterType) translate(parent
								+ sock.type + ".adp", sock.name, ancestor,
								"Socket", null, null);
						if (adp != null)
							fbs.add(adp);

						// Set 'sub'type to Socket
						sock.type += "Socket";
						instances.add(sock);
					}
				}
				if (iface.plugs != null) {
					for (Plug plug : iface.plugs) {
						AdapterType adp = (AdapterType) translate(parent
								+ plug.type + ".adp", plug.name, ancestor,
								"Plug", null, null);
						if (adp != null)
							fbs.add(adp);

						// Set 'sub'type to Plug
						plug.type += "Plug";
						instances.add(plug);
					}
				}
				compositeFB.instances = instances
						.toArray(compositeFB.instances);
			}
		}
		// End Adapters

		FBType fbtype = new FBType(name, comment, headers, iface, compositeFB,
				fbs);
		for (FBType fbt : fbs)
			fbtype.addDeferredBlock(ancestor, fbt.getDeferredBlocks());

		FBGraph graph = new FBGraph(insName, fbtype, false); // NOTE: Not
																// acyclic
																// anymore:
																// opts.acyc);
		Block[] sorted = opts.sorted ? graph.sortTopologically() : null;
		
		CodeGenerator cg = toCode(fbtype, baseName, insName, parentsName,
				sorted, params, targetType);

		if (cg != null)
			fbtype.addDeferredBlock(cg);

		return fbtype;
	}

	/**
	 * Creates a resource type.
	 * 
	 * @param root
	 *            root element describing the resource
	 * @param resType
	 *            name of the resource type
	 * @param insName
	 *            name of the instance being translated
	 * @param baseName
	 *            path to generate code to
	 * @param params
	 *            relevant parameters for the translation
	 * @param targetType
	 *            concrete host type to be translated to
	 * @return The resource type created; null otherwise.
	 */
	private static FBType makeResource(Element root, String resType,
			String insName, String baseName, Parameter[] params,
			String targetType) throws IOException {
		currentResourceName = insName;
		
		String[] headers = getCompilerHeaders(root.getChild("CompilerInfo"));
		if (opts.isGNUuCOS() || opts.isD3()) {
			if (headers != null) {
				String[] includes = new String[headers.length + 1];
				int i;
				for (i = 0; i < headers.length; i++)
					includes[i] = headers[i];
				includes[i] = "\""
						+ CProgGenerator
								.getGlobalVarsFileName(currentDeviceName)
						+ "h\"";
				headers = includes;
			} else {
				headers = new String[1];
				headers[0] = "\""
						+ CProgGenerator
								.getGlobalVarsFileName(currentDeviceName)
						+ "h\"";
			}
		}
		VarDeclaration[] vars = getVars(root);
		Element net = root.getChild("FBNetwork");

		if (opts.isNxtControl()) {
			// START block is implicit
			// add it to make it explicit
			Element startBlock = new Element("FB");
			startBlock.setAttribute("Name", "START");
			startBlock.setAttribute("Type", "E_RESTART");
			net.addContent(startBlock);
		}

		// Create new resource type for each instance of a resource
		String parentName = "";
		if (!insName.isEmpty()) {
			if (currentDeviceName.isEmpty())
				targetType = insName + "_" + resType;
			else {
				targetType = currentDeviceName + "_" + insName + "_" + resType;
				parentName = currentDeviceName + ".";
			}
		}

		FBType fbtype;
		if (net != null) {
			// Handle translation of resource types
			fbtype = makeFBNetwork(net, resType,
					root.getAttributeValue("Comment"), headers,
					new InterfaceList(vars), insName, parentName, baseName,
					params, targetType);
		} else {
			// Handle translation of resource instances
			fbtype = translate(parent + resType + ".res", insName, parentName,
					null, params, targetType);
		}

		/*
		 * if(opts.isNxtControl()) { // Add the 'start' block to the resource
		 * (if it isnt there?) -- assuming not there for now FBType startBlock =
		 * translate("E_RESTART.fbt", null, "START", ""); FBType[] newSubBlocks
		 * = new FBType[fbtype.subBlocks.length+1]; for(int sb = 0; sb <
		 * fbtype.subBlocks.length; sb++) { newSubBlocks[sb] =
		 * fbtype.subBlocks[sb]; } newSubBlocks[fbtype.subBlocks.length] =
		 * startBlock;
		 * 
		 * fbtype.subBlocks = newSubBlocks; }
		 */
		return fbtype;
	}

	/*
	 * DESCRIPTION: Adds content to a JDOM element PARAMETER: elem - parent
	 * element that will contain the new content content - list of new content
	 * to be added
	 */
	static void addJDOMcontent(Element elem, List<?> content) {
		ListIterator<?> i = content.listIterator();
		while (i.hasNext()) {
			Element e = (Element) ((Element) i.next()).clone();
			elem.addContent(i.previousIndex(), e);
		}
	}

	/**
	 * Clear static variables used on a per device basis.
	 */
	private static void clearPerDeviceStaticVars() {
		communicationBlocks.clear();
		CodeGenerator.srcDependencies.clear();
		CodeGenerator.objDependencies.clear();
		CodeGenerator.includePaths.clear();
		CodeGenerator.globalVars.clear();
		if (CodeGenerator.msgPeriodMap != null)
			CodeGenerator.msgPeriodMap.clear();
		SIPath.clearAnyVarsMap();
	}

	/*
	 * DESCRIPTION: Creates a device type PARAMETER: elem - element describing
	 * the device insName - name of the instance being translated baseName -
	 * path to generate code to baseDev - DOM information of resources from the
	 * base device (if any)
	 */
	private static void makeDevice(Element elem, String insName,
			String baseName, ResourceDOMInfo[] baseDev) throws IOException {
		clearPerDeviceStaticVars(); // clear device related static variables

		currentDeviceName = insName;
		
		// If hmi has been used in a previous device... start it
		// (First device HMI is started in NXTControlHMIGenerator.printHMIStuff)
		if (opts.isNxtControl() && NXTControlHMIGenerator.hmiUsed)
			NXTControlHMIGenerator.startNewConfigFiles(currentDeviceName);

		String type;
		if (insName.isEmpty())
			type = elem.getAttributeValue("Name");
		else
			type = elem.getAttributeValue("Type");

		currentDeviceType = type;
		
		ResourceDOMInfo[] resInfos = makeResourceDOMInfo(
				elem.getChildren("Resource"), baseName, baseDev, insName, type);
		Resource[] resources = new Resource[resInfos.length];
		for (int j = 0; j < resInfos.length; j++) {
			FBType resType = makeResource(resInfos[j].dom, resInfos[j].type,
					resInfos[j].name, resInfos[j].baseName, resInfos[j].params,
					null);
			resources[j] = new Resource(resInfos[j].name, resType,
					resInfos[j].params);
		}

		StringBuilder base = new StringBuilder(
				CodeGenerator.getParentName(baseName));
		base.append(CProgGenerator.makeDevFileName(insName, type));
		Parameter[] parameters = getParameters(elem);
		Device devType = new Device(insName, type,
				elem.getAttributeValue("Comment"), resources, parameters);

		String path = base.toString();
		topLevelFile = CodeGenerator.removeFileExt(CodeGenerator
				.getFileName(path));
		toCode(devType, base.toString());
	}

	/**
	 * Creates the DOM information for a list of resources.
	 * 
	 * @param resElems
	 *            list of resource elements in the DOM
	 * @param baseName
	 *            path to generate code to
	 * @param baseDev
	 *            DOM information of resources from the base device
	 * @param devName
	 *            device name
	 * @param devType
	 *            device type
	 * @return Parsed DOM information for the list of resources.
	 */
	private static ResourceDOMInfo[] makeResourceDOMInfo(List<?> resElems,
			String baseName, ResourceDOMInfo[] baseDev, String devName,
			String devType) {
		ArrayList<ResourceDOMInfo> resInfos = new ArrayList<ResourceDOMInfo>();
		if (baseDev != null) {
			ListIterator<?> j = resElems.listIterator();
			while (j.hasNext()) {
				// This is the <Resource> element in the .sys file.
				Element res = (Element) j.next();
				String name = res.getAttributeValue("Name");
				String type = res.getAttributeValue("Type");
				String base = CodeGenerator.getParentName(baseName) + type;
				Element net = res.getChild("FBNetwork");
				ResourceDOMInfo baseRes = ResourceDOMInfo.getBaseResource(
						baseDev, name);

				if (net == null) {
					// The resource has not been extended in the system
					if (baseRes == null) {
						// The device has been extended with this resource at
						// the system level
						resInfos.add(new ResourceDOMInfo(res, name, type, base,
								getResourceParams(res, name)));
					} else {
						// The resource has been extended in the device. Since
						// the
						// resource is not extended in the system, we simply
						// used the
						// resource as is.
						resInfos.add(baseRes);
					}
				} else {
					// The resource has been extended in the system
					if (baseRes == null) {
						// The device has been extended with this resource at
						// the system level.
						// However, since we didn't encounter this resource at
						// the device
						// level, we'll now need to check the .res file here.
						extendFromResFile(res, name, type, parent + type, net);
						resInfos.add(new ResourceDOMInfo(res, name, type, base,
								getResourceParams(res, name)));
					} else {
						// The resource has been extended in the device as well.
						// We do this double extension by extending the
						// <Resource> element
						// that had already been extended when parsing the .dev
						// file.
						extendResElement(baseRes.dom, res, net);
						baseRes.dom = res;
						resInfos.add(baseRes);
					}
				}
			}

			// Add all resources that appeared in the .dev file, but not in the
			// .sys file.
			// This seems to be a peculiarity of BlokIDE when resources are not
			// extended
			// at the system level. For FBDK, all resources always appear in the
			// .sys
			// file, irrespective of whether they have been extended or not.
			for (ResourceDOMInfo baseInfo : baseDev) {
				boolean found = false;
				for (ResourceDOMInfo resInfo : resInfos) {
					if (baseInfo.name.equals(resInfo.name)) {
						found = true;
						break;
					}
				}
				if (!found)
					resInfos.add(baseInfo);
			}
		} else {
			ListIterator<?> j = resElems.listIterator();
			while (j.hasNext()) {
				// This is the <Resource> element in the .dev file.
				Element res = (Element) j.next();
				String name = res.getAttributeValue("Name");
				String type = res.getAttributeValue("Type");
				String base = CodeGenerator.getParentName(baseName) + type;
				Element net = res.getChild("FBNetwork");
				if (!(opts.isNxtControl() && type.equalsIgnoreCase("EMB_RES"))) {
					// This means that this resource has been extended in the
					// device.
					// So, we look for the base resource and merge all the
					// contents in it
					// with the new extensions.
					Element modifiedNet = extendFromResFile(res, name, type,
							parent + type, net);
					if (net == null) {
						if (modifiedNet != null)
							res.addContent(modifiedNet.detach());
					}
				}

				resInfos.add(new ResourceDOMInfo(res, name, type, base,
						getResourceParams(res, name)));
			}
		}

		return resInfos.toArray(new ResourceDOMInfo[0]);
	}

	/**
	 * Extends a <Resource> element by opening the original .res file.
	 * 
	 * @param res
	 *            new <Resource> element containing the extensions
	 * @param name
	 *            name of the resource instance
	 * @param type
	 *            type of resource
	 * @param base
	 *            base file name and path information for the .res file
	 * @param net
	 *            new <FBNetwork> element containing the extensions
	 * @return The <FBNetwork> element containing the original contents and
	 *         extensions
	 * @throws IOException
	 */
	private static Element extendFromResFile(Element res, String name,
			String type, String base, Element net) {
		File file = new File(base + ".res");
		String fileName = type + ".res";
		file = isFBFileExists(file, fileName);
		if (file == null) {
			OutputManager.printError("", fileName + ": File cannot be found.",
					OutputLevel.FATAL);
			System.exit(0);
		}

		Document xml = XMLParser.parse(file.getPath(), true);
		if (xml == null) {
			OutputManager.printError("", file.getPath()
					+ ": Parsing failed to produce Document object.",
					OutputLevel.FATAL);
			System.exit(0);
		}
		if (opts.verbose) {
			OutputManager.printNotice(name,
					"Translating: " + file.getAbsolutePath(), OutputLevel.INFO);
		}

		// We are now at the <Resource> element in the .res file.
		return extendResElement(xml.getRootElement(), res, net);
	}

	/**
	 * Extends a <Resource> element with another <Resource> element that has
	 * been parsed.
	 * 
	 * @param root
	 *            existing <Resource> element that had already been parsed
	 * @param res
	 *            new <Resource> element containing the extensions
	 * @param net
	 *            new <FBNetwork> element containing the extensions
	 * @return The <FBNetwork> element containing the original contents and
	 *         extensions
	 */
	private static Element extendResElement(Element root, Element res,
			Element net) {
		addJDOMcontent(res, root.getChildren("VarDeclaration"));
		Element el = root.getChild("CompilerInfo");
		if (el != null) {
			// If CompilerInfo information exists, add it to the new <Resource>
			// element
			res.addContent(0, el.detach());
		}
		el = root.getChild("FBNetwork");
		if (net == null)
			return el;
		else if (el != null) {
			addJDOMcontent(net, el.getChildren("FB"));
			Element el2 = el.getChild("EventConnections");
			if (el2 != null) {
				Element net2 = net.getChild("EventConnections");
				if (net2 == null)
					net.addContent(el2.detach());
				else
					addJDOMcontent(net2, el2.getChildren("Connection"));
			}
			el2 = el.getChild("DataConnections");
			if (el2 != null) {
				Element net2 = net.getChild("DataConnections");
				if (net2 == null)
					net.addContent(el2.detach());
				else
					addJDOMcontent(net2, el2.getChildren("Connection"));
			}
		}
		return net;
	}

	/**
	 * Returns the parameters to a resource. For uC/OS, some parameters are
	 * mandatory and special handling is performed for this case.
	 * 
	 * @param res
	 *            resource element
	 * @param name
	 *            name of the resource
	 * @return Parameters for the resource
	 */
	private static Parameter[] getResourceParams(Element res, String name) {
		Parameter[] params = getParameters(res);
		if (opts.isGNUuCOS()) {
			if (params == null) {
				OutputManager.printError(name,
						"Expecting `RATE\' and `PRIO\' parameters.",
						OutputLevel.FATAL);
				System.exit(-1);
			}
			boolean foundRate = false;
			for (int i = 0; i < params.length; i++) {
				try {
					if (params[i].name.equals("RATE")) {
						if (Integer.parseInt(params[i].value) <= 0)
							throw new NumberFormatException();
						foundRate = true;
					} else if (params[i].name.equals("PRIO")) {
						if (!params[i].value.isEmpty()) {
							if (Integer.parseInt(params[i].value) <= 0)
								throw new NumberFormatException();
						}
					}
				} catch (NumberFormatException e) {
					OutputManager.printError(name, "`" + params[i]
							+ "\' parameter must be a positive integer.",
							OutputLevel.FATAL);
					System.exit(-1);
				}
			}
			if (!foundRate) {
				OutputManager.printError(name, "Expecting `RATE\' parameter.",
						OutputLevel.FATAL);
				System.exit(-1);
			}
		}

		return params;
	}

	/*
	 * DESCRIPTION: Creates a device from a System description PARAMETER: elem -
	 * element describing the device insName - name of the instance being
	 * translated baseName - path to generate code to
	 */
	private static void makeSysDevice(Element elem, String insName,
			String baseName) throws IOException {
		String devType = elem.getAttributeValue("Type");
		File file = new File(parent + devType + ".dev");
		String fileName = devType + ".dev";
		file = isFBFileExists(file, fileName);
		if (file == null) {
			OutputManager.printError("", fileName + ": File cannot be found.",
					OutputLevel.FATAL);
			System.exit(0);
		}

		Document xml = XMLParser.parse(file.getPath(), true);
		if (xml == null) {
			OutputManager.printError("", file.getPath()
					+ ": Parsing failed to produce Document object.",
					OutputLevel.FATAL);
			System.exit(0);
		}
		if (opts.verbose) {
			OutputManager.printNotice(insName,
					"Translating: " + file.getAbsolutePath(), OutputLevel.INFO);
		}

		opts.setOutputPath(insName);
		File outPath = new File(opts.outputpath());
		if (!outPath.mkdirs()) {
			if (!outPath.exists()) {
				OutputManager.printError("", "Output folder `" + outPath
						+ "' creation failed.", OutputLevel.FATAL);
				System.exit(1);
			}
		}
		baseName = opts.outputpath() + CodeGenerator.getFileName(baseName);

		Element devElem = xml.getRootElement();
		ResourceDOMInfo[] resInfos = makeResourceDOMInfo(
				devElem.getChildren("Resource"), baseName, null, insName,
				devType);
		makeDevice(elem, insName, baseName, resInfos);
	}

	/*
	 * DESCRIPTION: Replaces the default DTD with our own and writes to a new
	 * file PARAMETER: "input" - input file to be parsed RETURNS: base name of
	 * the new file created
	 */
	static String modifyDTD(String input) throws IOException {
		File inputFile = new File(input);
		if (!inputFile.exists())
			return null;

		String filename = null;
		try {
			// Derive the base filename
			String baseName = input;
			int extIndex = input.lastIndexOf('.');
			if (extIndex != -1) {
				// Get rid of the input filename's extension
				baseName = baseName.substring(0, extIndex);
			}

			filename = input; // to be use with the exeption handler
			BufferedReader bufInput = new BufferedReader(new FileReader(
					filename));
			filename = baseName + ".cdt"; // to be use with the exeption handler
			BufferedWriter bufOutput = new BufferedWriter(new FileWriter(
					filename));
			String line;
			while ((line = bufInput.readLine()) != null) {
				if (line.contains("LibraryElement.dtd\"")) {
					// URL dtdURL =
					// FBench.class.getResource("LibraryElement.dtd");
					URL dtdURL = FBtoStrl.class.getResource("/fbtostrl");
					File dtdPath = new File(dtdURL.toString());
					dtdPath = dtdPath.getParentFile().getParentFile();
					int index = line.indexOf('\"') + 1;
					// line = line.substring(0, index) + dtdPath.getPath() +
					// "\" >";
					line = line.substring(0, index) + dtdPath.getPath()
							+ File.separator + "dtd" + File.separator
							+ "LibraryElement.dtd\" >";
				}
				// Write out "input" to a new file
				bufOutput.write(line + System.getProperty("line.separator"));
			}
			bufInput.close();
			bufOutput.close();
			return baseName;
		} catch (FileNotFoundException e) {
			OutputManager.printError("", "Could not open `" + filename + "\'",
					OutputLevel.ERROR);
			throw e;
		}
	}

	/**
	 * Creates the intermediate format and invokes the selected code generator.
	 * 
	 * @param fbt
	 *            function block to be translated
	 * @param baseName
	 *            base name of the function block file
	 * @param name
	 *            name of the code generator (corresponds to the instance name)
	 * @param parentsName
	 *            name(s) of the parent function block
	 * @param sortedNodes
	 *            list of sorted function blocks
	 * @param params
	 *            relevant parameters for the translation
	 * @param targetType
	 *            concrete host type to be translated to
	 * @return Null if code was generated; otherwise, the code generator itself.
	 */
	static CodeGenerator toCode(FBType fbt, String baseName, String name,
			String parentsName, Block[] sortedNodes, Parameter[] params,
			String targetType) throws IOException {
		
		CodeGenerator cg = null;
		boolean top;
		if (opts.isSimul())
			top = parentsName.isEmpty(); // top block is 'root' in simul..
		else
			top = name.isEmpty();

		FunctionBlock fb = new FunctionBlock(fbt, opts, parentsName + name,
				params, targetType);
		if (opts.ccode) {
			CGenerator factory = new CGenerator(fb, opts, outputFiles, name,
					parentsName, fbt.getDeferredBlocks(), sortedNodes);
			HashSet<Block> comBlocks = null;
			if (top && !communicationBlocks.isEmpty())
				comBlocks = communicationBlocks;

			// NOTE: All SIFBs delayed until after the function block network is
			// printed
			// for any type / any dynamic content
			if (fbt.needsUnique()) {
				// Delay printing SIFBs until we have connections (for ANY
				// types)
				cg = factory;
				String cgFilename = baseName.substring(0,
						baseName.lastIndexOf(File.separator) + 1);
				cgFilename += fb.getCompiledType();
				cg.setFileName(cgFilename);
			} else if (fbt.isPretC()) {
				String uniqueBaseName = baseName.substring(0,
						baseName.lastIndexOf(File.separator) + 1);
				uniqueBaseName += fb.getCompiledType();
				cg = factory.generateCode(uniqueBaseName, top, comBlocks);
			} else {
				if (factory.isUCOSresource()) {
					cg = factory;
					cg.setFileName(baseName);
				} else
					cg = factory.generateCode(baseName, top, comBlocks);
			}

			if (opts.isQTSimul() && top) {
				ProcessBuilder procBuilder = new ProcessBuilder("qmake",
						"-project");
				procBuilder.directory(new File(parent));
				procBuilder.redirectErrorStream(true);

				try {
					// Run "qmake -project"
					execProcess(procBuilder.start());

					// Run "qmake"
					procBuilder.command("qmake");
					execProcess(procBuilder.start());

					// Run "make"
					procBuilder.command("make");
					execProcess(procBuilder.start());
				} catch (IOException e) {
					List<String> commands = procBuilder.command();
					Iterator<String> c = commands.iterator();
					System.err
							.println(c.next()
									+ " cannot be found. All source files required for QT simulation");
					System.err
							.println("have been generated, but generation of the executable has failed.");
				}
			}
		}else if (opts.pretc) {
			PRETCGenerator factory = new PRETCGenerator(fb, opts, outputFiles, name,
					parentsName, fbt.getDeferredBlocks(), sortedNodes);
			HashSet<Block> comBlocks = null;
			if (top && !communicationBlocks.isEmpty())
				comBlocks = communicationBlocks;

			// NOTE: All SIFBs delayed until after the function block network is
			// printed
			// for any type / any dynamic content
			if (fbt.needsUnique()) {
				// Delay printing SIFBs until we have connections (for ANY
				// types)
				cg = factory;
				String cgFilename = baseName.substring(0,
						baseName.lastIndexOf(File.separator) + 1);
				cgFilename += fb.getCompiledType();
				cg.setFileName(cgFilename);
			} else {
				cg = factory.generateCode(baseName, top, comBlocks);
			}
			
			System.out.println(outputFiles);
			String cname = CodeGenerator.getParentName((baseName))+FBtoStrl.inputFileName+".c";
			CodeReader r = new CodeReader(cname);
			
			for (String s : outputFiles) {
				if (s.endsWith(".h")) {
					CodeReader h = new CodeReader(s);
					r.data = r.data.replaceFirst("\\Q#include \""+h.filename+".h\"\\E", h.data);
					r.data = r.data.replaceAll("\\Q#include \""+h.filename+".h\"\\E", "");
					//System.out.println("#include \""+h.filename+"\"");
				}
			}
			CodePrinter p = new CodePrinter(CodeGenerator.getParentName((baseName))+FBtoStrl.inputFileName+".pretc.c");
			p.print(r.data);
			p.close();
		} else if (opts.strl) {
			StrlGenerator factory = new StrlGenerator(fb, opts, outputFiles,
					name, parentsName, fbt.getDeferredBlocks());
			cg = factory.generateCode(baseName, top, null);
		} else if (opts.sysj) {

		} else if (opts.rmc) {
			RMCGenerator factory = new RMCGenerator(fb, opts, outputFiles,
					name, parentsName, fbt.getDeferredBlocks(), parentsName);
			cg = factory.generateCode(baseName, top, null);
		}

		return cg;
	}

	/*
	 * DESCRIPTION: Generates the program for the given device PARAMETER: dev -
	 * device for which program is to be generated baseName - path to generate
	 * code to
	 */
	static void toCode(Device dev, String baseName) throws IOException {
		if (opts.ccode) { //MK Note
			if (opts.isD3()) {
				D3Generator factory = new D3Generator(dev, communicationBlocks,
						opts.getOutputPath());
				progGenerators
						.put(baseName,
								new ProgramInfo(factory,
										new HashMap<String, GlobalVar>(
												CodeGenerator.globalVars),
										new HashSet<String>(
												CodeGenerator.srcDependencies),
										new HashSet<String>(
												CodeGenerator.objDependencies),
										new HashSet<String>(
												CodeGenerator.includePaths)));
			} else {
				OutputPath outputPath = opts.getOutputPath();
				CProgGenerator factory;
				if (opts.isTTPOS())
					factory = new TTPGenerator(dev, communicationBlocks,
							outputPath);
				else if (opts.isGNUuCOS())
					factory = new UCOSGenerator(dev, communicationBlocks,
							outputPath);
				else
					factory = new CProgGenerator(dev, communicationBlocks,
							outputPath);
				factory.generateCode(baseName);
				factory.handleLastRites(opts.outputpath(), opts, dev, dev.name,
						CodeGenerator.srcDependencies,
						CodeGenerator.objDependencies,
						CodeGenerator.includePaths, CodeGenerator.globalVars);
			}
		}
	}

	/*
	 * DESCRIPTION: Executes an external process PARAMETER: proc - external
	 * process to execute
	 */
	protected static void execProcess(Process proc) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(
				proc.getInputStream()));
		String line;
		while ((line = in.readLine()) != null) {
			OutputManager.printNotice("execProcess", line, OutputLevel.INFO);
		}
		try {
			proc.waitFor();
		} catch (InterruptedException e) {
			// Ignore and do nothing
		}
	}

	static Event getEvent(Element eventElem) {
		String name = eventElem.getAttributeValue("Name");
		String comment = eventElem.getAttributeValue("Comment");
		String[] with = null;

		List<?> content = eventElem.getChildren("With");
		int size = content.size();
		if (size > 0) {
			with = new String[size];
			Iterator<?> i = content.iterator();
			int j = 0;
			while (i.hasNext()) {
				with[j] = ((Element) i.next()).getAttributeValue("Var");
				j++;
			}
		}
		return new Event(name, comment, with);
	}

	static Event[] getEvents(Element eventsElem) {
		List<?> content = eventsElem.getChildren("Event");
		int size = content.size();
		if (size > 0) {
			Event[] events = new Event[size];

			Iterator<?> i = content.iterator();
			int j = 0;
			while (i.hasNext()) {
				events[j] = getEvent((Element) i.next());
				j++;
			}
			return events;
		}
		return null;
	}

	static VarDeclaration getVar(Element varElem) {
		String name = varElem.getAttributeValue("Name");
		String type = varElem.getAttributeValue("Type");
		String arraySize = varElem.getAttributeValue("ArraySize");
		String initial = varElem.getAttributeValue("InitialValue");
		if (initial != null) {
			// Empty InitialValue creates difficulties later on---easier to make
			// it null,
			// as that is how it needs to be treated anyway
			initial = initial.trim();
			if (initial.isEmpty())
				initial = null;
		}
		String comment = varElem.getAttributeValue("Comment");

		// If var use custom data types... translate them
		if (!IEC61131Types.isIEC61131Type(type)
				&& !IEC61131Types.processedStructuredType(type)) {
			// TODO: Note this is kinda C specific
			// Trim type in case of Pointers :|
			String datatype = type.replace('*', ' ').trim();
			try {
				translate(parent + datatype + ".dtp", "", "", "", null, "");
			} catch (Exception e) {
				e.printStackTrace();
				OutputManager.closeOutput();
				System.exit(1);
			}
		}

		return new VarDeclaration(name, type, arraySize, initial, comment);
	}

	public static VarDeclaration[] getVars(Element varsElem) {
		List<?> content = varsElem.getChildren("VarDeclaration");
		int size = content.size();
		if (size > 0) {
			VarDeclaration[] vars = new VarDeclaration[size];

			Iterator<?> i = content.iterator();
			int j = 0;
			while (i.hasNext()) {
				vars[j] = getVar((Element) i.next());
				j++;
			}
			return vars;
		}
		return null;
	}

	static EnumeratedValue getEnumValue(Element valueElem) {
		String name = valueElem.getAttributeValue("Name");
		String comment = valueElem.getAttributeValue("Comment");

		return new EnumeratedValue(name, comment);
	}

	private static EnumeratedValue[] getEnumValues(Element valsElem) {
		List<?> content = valsElem.getChildren("EnumeratedValue");
		int size = content.size();
		if (size > 0) {
			EnumeratedValue[] values = new EnumeratedValue[size];

			Iterator<?> i = content.iterator();
			int j = 0;
			while (i.hasNext()) {
				values[j] = getEnumValue((Element) i.next());
				j++;
			}
			return values;
		}
		return null;
	}

	/**
	 * Parses the header information. If the header contains #include
	 * information, that will be stripped away, but the quotes ("") or angled
	 * brackets (<>) around the file name will be preserved. Otherwise, the
	 * header will be treated as a plain file name and any quotes around it will
	 * be stripped away.
	 * 
	 * @param header
	 * @return
	 */
	static String[] parseIncludes(String header) {
		String[] files = header.split("#[ \t]*include[ \t]*[\"\\<]");
		if (files.length == 1) {
			// No #include information was found. We treat this as a plain file
			// name.
			files[0] = files[0].trim();
			if (files[0].startsWith("\"") && files[0].endsWith("\""))
				files[0] = files[0].substring(1, files[0].length() - 1).trim();
		} else if (files.length > 1) {
			ArrayList<String> headers = new ArrayList<String>(files.length);
			for (String file : files) {
				file = file.trim();
				if (file.isEmpty())
					continue;
				if (file.endsWith("\""))
					file = "\"" + file;
				else if (file.endsWith(">"))
					file = "<" + file;
				headers.add(file);
			}
			files = headers.toArray(new String[0]);
		}
		return files;
	}

	public static String[] getCompilerHeaders(Element elem) {
		if (elem == null)
			return null;

		String header = elem.getAttributeValue("header");
		if (header == null)
			return null;

		// Assume that ';' is used as the file delimiter
		String[] heads = splitFileList(header, ";");
		if (heads.length == 0)
			return null;
		ArrayList<String> headers = new ArrayList<String>();
		for (String head : heads) {
			if (head.matches("package[ \t]+fb.rt.*")) {
				// This is an annoyance to support files that can interoperate
				// with FBDK
				continue;
			}
			String[] files = parseIncludes(head);
			for (String file : files) {
				if (!file.isEmpty())
					headers.add(file);
			}
		}

		if (headers.isEmpty())
			return null;
		else
			return headers.toArray(new String[0]);

		/*
		 * int index = header.indexOf('#'); if (index < 0) return null; String[]
		 * headers = header.split("\\s"); int size = 0; for (int i = 0; i <
		 * headers.length; i++) { if (headers[i].charAt(0) == '#') size++; }
		 * 
		 * heads = new String[size]; index = 0; for (int i = 0; i <
		 * headers.length; i++) { if (headers[i].charAt(0) == '#') {
		 * heads[index] = headers[i]; if (!headers[i].contains("<") &&
		 * !headers[i].contains("\"")) { i++; if (i < headers.length)
		 * heads[index] += " " + headers[i]; } index++; } } return heads;
		 */
	}

	public static String[] getCompilerClassDefs(Element elem) {
		if (elem == null)
			return null;

		String classdef = elem.getAttributeValue("classdef");
		if (classdef == null)
			return null;
		// return classdef.split("\\s");
		return splitFileList(classdef);
	}

	/**
	 * Parses and tokenizes a list of files given as a single string. If the
	 * string contains ';', we use ';' as the file delimiter (new version of the
	 * IDE). Otherwise, we assume whitespace as the file delimiter, provided the
	 * whitespace does not appear within quotes. If '"' appears as part of the
	 * filename, it will be preserved as part of the filename, provided it does
	 * not appear at the start or end of the filename, or immediately before a
	 * whitespace. Filenames with leading or trailing whitespaces will not be
	 * correctly handled.
	 * 
	 * @param fileString
	 *            string containing the list of file names
	 * @return Array containing the list of tokenized file names
	 */
	static String[] splitFileList(String fileString) {
		if (fileString.contains(";")) {
			// Assume that ';' is used as the file delimiter (new version of the
			// IDE)
			return splitFileList(fileString, ";");
		} else {
			// Assume that ' ' is used as the file delimiter (old version of the
			// IDE).
			// Now, we need to search for quoted strings.
			fileString = fileString.trim();
			ArrayList<String> files = new ArrayList<String>();
			boolean inString = false;
			boolean inQuote = false;
			StringBuilder file = new StringBuilder();
			int length = fileString.length();
			for (int i = 0; i < length; i++) {
				char c = fileString.charAt(i);
				if (!inString) {
					if (Character.isWhitespace(c))
						continue;
					else {
						inString = true;
						file.delete(0, file.length());
						if (c == '\"')
							inQuote = true;
						else
							file.append(c);
					}
				} else {
					if (inQuote) {
						if (c == '\"') {
							if (i >= (length - 1)) {
								// This is the end of the string, hence, the end
								// of the
								// filename
								inQuote = false;
								inString = false;
								if (file.length() > 0)
									files.add(file.toString().trim());
								continue;
							} else if (Character.isWhitespace(fileString
									.charAt(i + 1))) {
								// The next character is a whitespace, so this
								// is the end
								// of the filename
								inQuote = false;
								inString = false;
								if (file.length() > 0)
									files.add(file.toString().trim());
								continue;
							} else {
								// Assume that '\"' is part of the filename
							}
						}
					} else {
						if (Character.isWhitespace(c)) {
							inString = false;
							if (file.length() > 0)
								files.add(file.toString());
							continue;
						}
					}
					file.append(c);
				}
			}
			return files.toArray(new String[0]);
		}
	}

	/**
	 * Parses and tokenizes a list of files given as a single string according
	 * to 'delimiter'. If '"' appears as part of the filename, it will be
	 * preserved as part of the filename, provided it does not appear at the
	 * start or end of the filename, or immediately before a whitespace.
	 * Filenames with leading or trailing whitespaces will not be correctly
	 * handled.
	 * 
	 * @param fileString
	 *            string containing the list of file names
	 * @param delimiter
	 *            delimiter to be used for tokenizing file names
	 * @return Array containing the list of tokenized file names
	 */
	static String[] splitFileList(String fileString, String delimiter) {
		ArrayList<String> files = new ArrayList<String>();
		if (fileString.contains(delimiter)) {
			String[] tokens = fileString.split(delimiter);
			for (String file : tokens) {
				file = file.trim();
				if (file.isEmpty())
					continue;
				if (file.startsWith("\"") && file.endsWith("\"")) {
					// This is an explicitly quoted string---remove it because
					// make
					// doesn't like this
					file = file.substring(1, file.length() - 1).trim();
					if (file.isEmpty())
						continue;
				}
				files.add(file);
			}
		} else
			files.add(fileString);

		return files.toArray(new String[0]);
	}

	public static InterfaceList getInterfaceList(Element interfaceListElem) {
		if (interfaceListElem != null) {
			Event[] eventInputs = null;
			Event[] eventOutputs = null;
			VarDeclaration[] inputVars = null;
			VarDeclaration[] outputVars = null;
			// Kyle: MethodReference[] externalMethods = null;
			Plug[] plugs = null;
			Socket[] sockets = null;

			Element listElem;

			listElem = interfaceListElem.getChild("EventInputs");
			if (listElem != null)
				eventInputs = getEvents(listElem);

			listElem = interfaceListElem.getChild("EventOutputs");
			if (listElem != null)
				eventOutputs = getEvents(listElem);

			listElem = interfaceListElem.getChild("InputVars");
			if (listElem != null)
				inputVars = getVars(listElem);

			listElem = interfaceListElem.getChild("OutputVars");
			if (listElem != null)
				outputVars = getVars(listElem);

			/*
			 * Kyle: listElem = interfaceListElem.getChild("ExternalMethods");
			 * if (listElem != null) externalMethods =
			 * getExternalMethods(listElem);
			 */

			listElem = interfaceListElem.getChild("Plugs");
			if (listElem != null)
				plugs = getPlugs(listElem);

			listElem = interfaceListElem.getChild("Sockets");
			if (listElem != null)
				sockets = getSockets(listElem);

			return new InterfaceList(eventInputs, eventOutputs, inputVars,
					outputVars,
					/* Kyle: externalMethods, */plugs, sockets);
		} else {
			OutputManager.printError("", "`InterfaceList\' element not found.",
					OutputLevel.FATAL);
			System.exit(0);
			return null;
		}
	}

	/*
	 * Kyle: private static MethodReference[] getExternalMethods(Element
	 * listElem) { List<?> content = listElem.getChildren("MethodReference");
	 * int size = content.size(); if (size > 0) { MethodReference[] references =
	 * new MethodReference[size];
	 * 
	 * Iterator<?> i = content.iterator(); int j = 0; while ( i.hasNext() ) {
	 * references[j] = getMethodReference( (Element)i.next() ); j++; } return
	 * references; } return null; }
	 * 
	 * private static MethodReference getMethodReference(Element ref) { String
	 * name = ref.getAttributeValue("Name"); String comment =
	 * ref.getAttributeValue("Comment"); return new MethodReference(name,
	 * comment); }
	 */

	private static Plug[] getPlugs(Element listElem) {
		List<?> content = listElem.getChildren("AdapterDeclaration");
		int size = content.size();
		if (size > 0) {
			Plug[] plugs = new Plug[size];

			Iterator<?> i = content.iterator();
			int j = 0;
			while (i.hasNext()) {
				plugs[j] = getPlug((Element) i.next());
				j++;
			}
			return plugs;
		}
		return null;
	}

	private static Plug getPlug(Element plugElem) {
		String name = plugElem.getAttributeValue("Name");
		String type = plugElem.getAttributeValue("Type");
		return new Plug(name, type);
	}

	private static Socket[] getSockets(Element listElem) {
		List<?> content = listElem.getChildren("AdapterDeclaration");
		int size = content.size();
		if (size > 0) {
			Socket[] sockets = new Socket[size];

			Iterator<?> i = content.iterator();
			int j = 0;
			while (i.hasNext()) {
				sockets[j] = getSocket((Element) i.next());
				j++;
			}
			return sockets;
		}
		return null;
	}

	private static Socket getSocket(Element socketElem) {
		String name = socketElem.getAttributeValue("Name");
		String type = socketElem.getAttributeValue("Type");
		return new Socket(name, type);
	}

	public static ECAction getECAction(Element elem) {
		String alg = elem.getAttributeValue("Algorithm");
		String output = elem.getAttributeValue("Output");
		// Kyle: String isExitAction = elem.getAttributeValue("IsExitAction");
		return new ECAction(alg, output /*
										 * Kyle: ,
										 * Boolean.parseBoolean(isExitAction)
										 */);
	}

	static ECState getECState(Element elem) {
		String name = elem.getAttributeValue("Name");
		String comment = elem.getAttributeValue("Comment");
		ECAction[] actions = null;
		HCECC hcecc = null;
		List<?> content = elem.getChildren("ECAction");
		int size = content.size();
		if (size > 0) {
			actions = new ECAction[size];

			Iterator<?> i = content.iterator();
			int j = 0;
			while (i.hasNext()) {
				actions[j] = getECAction((Element) i.next());
				j++;
			}
		}

		// HCECC
		Element hceccElem = elem.getChild("HCECC");
		if (hceccElem != null) {
			hcecc = getHCECC(hceccElem);
			return new ECState(name, comment, actions, hcecc);
		}

		return new ECState(name, comment, actions);
	}

	static ECTransition getECTransition(Element elem) {
		String source = elem.getAttributeValue("Source");
		String destination = elem.getAttributeValue("Destination");
		String condition = elem.getAttributeValue("Condition");

		// Allowance for Actions on transitions to create Mealy / Hybrid ECCs
		ECAction[] actions = null;
		List<?> content = elem.getChildren("ECAction");
		int size = content.size();
		if (size > 0) {
			actions = new ECAction[size];

			Iterator<?> i = content.iterator();
			int j = 0;
			while (i.hasNext()) {
				actions[j] = getECAction((Element) i.next());
				j++;
			}
		}

		return new ECTransition(source, destination, condition, actions);
	}

	static ECC getECC(Element elem) {
		ECState[] states = null;
		ECTransition[] transitions = null;
		List<?> content;
		String name;
		int size;

		name = elem.getAttributeValue("Name");
		content = elem.getChildren("ECState");
		size = content.size();
		if (size > 0) {
			states = new ECState[size];

			Iterator<?> i = content.iterator();
			int j = 0;
			while (i.hasNext()) {
				states[j] = getECState((Element) i.next());
				j++;
			}
		}

		content = elem.getChildren("ECTransition");
		size = content.size();
		if (size > 0) {
			transitions = new ECTransition[size];

			Iterator<?> i = content.iterator();
			int j = 0;
			while (i.hasNext()) {
				transitions[j] = getECTransition((Element) i.next());
				j++;
			}
		}

		return new ECC(name, states, transitions);
	}

	public static HCECC getHCECC(Element elem) {
		// ECC ecc = null;
		ECC[] parallelECCs = null;
		int size;

		List<?> content = elem.getChildren("ECC");
		size = content.size();
		if (size > 0) {
			parallelECCs = new ECC[size];
			Iterator<?> i = content.iterator();
			/*
			 * ecc = getECC((Element)i.next());
			 */
			int j = 0;
			while (i.hasNext()) {
				parallelECCs[j] = getECC((Element) i.next());
				j++;
			}

		}

		return new HCECC(parallelECCs);

	}

	public static Algorithm getAlgorithm(Element elem) {
		String comment = elem.getAttributeValue("Comment");
		String name = elem.getAttributeValue("Name");
		String language = "";
		String prototype = null;
		String text = null;
		Element algChild;
		if ((algChild = elem.getChild("ST")) != null)
			language = "ST";
		else if ((algChild = elem.getChild("Other")) != null) {
			language = algChild.getAttributeValue("Language");
			prototype = algChild.getAttributeValue("Prototype");
		}

		if (!language.equals(""))
			text = algChild.getAttributeValue("Text");

		return new Algorithm(comment, name, language, prototype, text);
	}

	public static PretC getPretC(Element elem, InterfaceList ifList) {
		return new PretC(ifList, elem.getText());
	}

	public static BasicFB getBasicFB(Element elem, InterfaceList ifList) {
		VarDeclaration[] internalVars = null;
		ECC ecc = null;
		HCECC hcecc = null;
		Algorithm[] algs = null;

		Element childElem;

		childElem = elem.getChild("InternalVars");
		if (childElem != null)
			internalVars = getVars(childElem);

		childElem = elem.getChild("ECC");
		if (childElem != null)
			ecc = getECC(childElem);

		childElem = elem.getChild("HCECC");
		if (childElem != null)
			hcecc = getHCECC(childElem);

		List<?> content = elem.getChildren("Algorithm");
		int size = content.size();
		if (size > 0) {
			algs = new Algorithm[size];

			Iterator<?> i = content.iterator();
			int j = 0;
			while (i.hasNext()) {
				algs[j] = getAlgorithm((Element) i.next());
				j++;
			}
		}

		if (hcecc != null)
			return new BasicHCECCFB(internalVars, hcecc, algs);
		else
			return new BasicFB(internalVars, ecc, algs);
	}

	static Connection[] getConnections(Element elem) {
		if (elem == null)
			return null;

		Connection[] connections = null;
		List<?> content = elem.getChildren("Connection");
		int size = content.size();
		if (size > 0) {
			connections = new Connection[size];
			int j = 0;
			Iterator<?> i = content.iterator();
			while (i.hasNext()) {
				Element e = (Element) i.next();
				String unitDelayed = e.getAttributeValue("UnitDelayed");
				boolean delayed = false;
				if (unitDelayed != null)
					delayed = unitDelayed.equalsIgnoreCase("True");
				connections[j] = new Connection(e.getAttributeValue("Source"),
						e.getAttributeValue("Destination"), delayed);
				j++;
			}
		}
		return connections;
	}

	static Parameter[] getParameters(Element elem) {
		Parameter[] params = null;
		List<?> content = elem.getChildren("Parameter");
		int size = content.size();
		if (size > 0) {
			params = new Parameter[size];
			int i = 0;
			Iterator<?> j = content.iterator();
			while (j.hasNext()) {
				Element e = (Element) j.next();
				params[i] = new Parameter(e.getAttributeValue("Name"),
						e.getAttributeValue("Value"));
				i++;
			}
		}
		return params;
	}

	public static FBNetwork getFBNetwork(Element elem) {
		FBInstance[] instances = null;
		List<?> content = elem.getChildren("FB");
		int size = content.size();
		if (size > 0) {
			instances = new FBInstance[size];
			int j = 0;
			Iterator<?> i = content.iterator();
			while (i.hasNext()) {
				Element e = (Element) i.next();
				Parameter[] params = getParameters(e);
				instances[j] = new FBInstance(e.getAttributeValue("Name"),
						e.getAttributeValue("Type"), params);
				j++;
			}
		}
		Connection[] eventCon = getConnections(elem
				.getChild("EventConnections"));
		Connection[] dataCon = getConnections(elem.getChild("DataConnections"));
		// Kyle: Connection[] methodCon =
		// getConnections(elem.getChild("MethodConnections"));

		return new FBNetwork(instances, eventCon, dataCon /* Kyle: , methodCon */);
	}

	private static void printHelp() {
		System.out.println("\nFBC - Function Block Compiler\n");
		System.out.println("Usage: java -jar FBC.jar [OPTIONS] FILE");
		// System.out.println("Usage: FBC.exe [OPTIONS] FILE");
		System.out.println("where");
		System.out
				.println("  FILE is the input file to be compiled (*.fbt, *.res, *.dev, *.sys), and");
		System.out.println("  OPTIONS are as follows:");
		System.out.println("  --ccode             generates C code (default)");
		System.out.println("  --cpp               generates C++ code");
		System.out.println("  --strl              generates Esterel code");
		System.out.println("  --sysj              generates SystemJ code");
		System.out
				.println("  --rmc               generates LTS for Roopak's Model Checker");
		System.out
				.println("                      Options --ccode, --strl, --sysj, and --rmc are mutually exclusive.");
		System.out
				.println("                      The last option to be included is the one that would be in effect.");
		System.out
				.println("  --string=n          configures the maximum length for string variables (default n=32)");
		System.out
				.println("  --nxt               generates code from a nxtStudio specification");
		System.out
				.println("                      Includes handling of HMI, but not Watch/Alarm/Digiscope");
		System.out
				.println("  --v                 prints additional informative messages during code generation");
		// System.out.println("  --m                 machine interface: makes stdout and stderr easily machine-readable");
		System.out
				.println("  --sort              generates topologically sorted code");
		// System.out.println("  --acyc              same as --sort but automatically removes any cycles detected");
		System.out
				.println("  --run               generates C code that is ready-to-run (includes main function)");
		System.out
				.println("  --altsim            uses alternative lib for xml parsing for simulation");

		System.out
				.println("  --simul:<ipaddr>    generates simulator for C code with sockets");
		System.out
				.println("                      ipaddr is the IP addres of the simulator server (default 127.0.0.1)");
		// System.out.println("  --qtsimul           generates simulator for C code using QT");
		System.out
				.println("                      Options --run and --simul mutually exclusive. The last option");
		System.out
				.println("                      to be included is the one that would be in effect.");
		System.out
				.println("  --platform=<target> generates code for a specific platform target");
		System.out
				.println("                      Platforms that are currently supported are:");
		System.out
				.println("                      gnu - all GNU build environments (default)");
		System.out
				.println("                      gnu-ucos - for uCOS using GNU build tools*");
		System.out
				.println("                      iotapps - for IoTapps");
		// System.out.println("                      ms - for Windows build environments using nmake");
		System.out
				.println("                      ttpos - for TTP-OS using TTTech's tools*");
		System.out
				.println("                      d3 - for TTP on D3 platform*");
		System.out
				.println("                      The last specified platform is the one that would be in effect.");
		System.out
				.println("                      *Simulators cannot be generated for these platforms.");
		System.out
				.println("  --remote=<address>  remote deployment server address");
		System.out
				.println("  --uername=<name>    deployment service username");
		System.out
				.println("  --key=<key>         deployment service key");
		System.out
				.println("  --nomake            suppress the generation of makefiles");
		System.out
				.println("  --L=<dir>           adds the directory <dir> to be searched for function block files and library files");
		System.out
				.println("  --O=<dir>           sets the directory <dir> for the output files to be placed");
		System.out
				.println("  --lib=<library files>  adds a library file");
		System.out
				.println("  --startup=<startup function>  C file which contains the startup() function");
		System.out
				.println("  --makeConfig=<JSON make config file> JOSN file which adds to the make arguments");
		System.out
				.println("  --header=<header files>  adds a header file");
		System.out.println("  --version           prints the version of FBC");
		System.out.println("  --help              prints this help message");
		System.out.println("2016/06/21");
		System.out.println();
	}
	
	public static void resetStaticVariables() {
		opts = new Options();

		// List of all deferred program generators
		progGenerators = null;
		// Set of FBTypes that have already been processed
		processedFBTypes = new HashSet<FBType>();
		// Set of communication blocks that will result in global variables
		communicationBlocks = new HashSet<Block>();
		searchPath = new ArrayList<String>();
		outputFiles = new LinkedList<String>();
		headerFiles = new ArrayList<String>();
		libraryFiles = new ArrayList<String>();
		
		anyTypeMap = new HashMap<String, String>(); // FQInstanceName.Port,
													// Type
		varSizeMap = new HashMap<String, String>(); // FQInstanceName.CountName,
													// Value

		parent = null;
		topLevelFile = null;
		inputFileName = null;
		currentDeviceName = "";
		currentDeviceType = "";
		currentResourceName = "";


		// PretC
		pretCUsed = false;
		
	}
	public static void main(String[] args) {
		resetStaticVariables();
		try {
			if (args.length < 1) {
				printHelp();
				return;
			}

			boolean printInfo = false;
			String fileName = null;
			for (int i = 0; i < args.length; i++) {
				System.out.println("Args:"+args[i]);
				if (args[i].startsWith("-")) {
					if (args[i].equals("--ccode")) {
						opts.ccode = true;
						opts.cpp = false;
						opts.strl = false;
						opts.sysj = false;
						opts.rmc = false;
					} else if (args[i].equals("--cpp")) {
						opts.cpp = true;
						opts.ccode = false;
						opts.strl = false;
						opts.sysj = false;
						opts.rmc = false;
					} else if (args[i].equals("--strl")) {
						opts.strl = true;
						opts.ccode = false;
						opts.cpp = false;
						opts.sysj = false;
						opts.rmc = false;
					} else if (args[i].equals("--pretc")) {
						opts.ccode = false;
						opts.cpp = false;
						opts.strl = false;
						opts.sysj = false;
						opts.rmc = false;
						opts.pretc = true;
						opts.sorted = true;
					} else if (args[i].equals("--sysj")) {
						opts.sysj = true;
						opts.ccode = false;
						opts.cpp = false;
						opts.strl = false;
						opts.rmc = false;
					} else if (args[i].equals("--rmc")) {
						opts.rmc = true; 
						opts.ccode = false;
						opts.cpp = false;
						opts.strl = false;
						opts.sysj = false;
					} else if (args[i].equals("--sort")) {
						opts.sorted = true;
					} else if (args[i].equals("--altsim")) {
						opts.altSim = true;
						SimHelpers.extraCLibs[0] = "libmxml.a"; //replace xml lib for new version of GCC
					}
					// NOTE: Acyclic sorting disabled now that unit-delayed
					// connections exist
					/*
					 * else if (args[i].equals("--acyc")) { opts.sorted = true;
					 * opts.acyc = true; }
					 */
					else if (args[i].equals("--run")) {
						opts.runnable = true;
						opts.simul = false;
						opts.qtsimul = false;
					} else if (args[i].equals("--testbench")) {
						opts.runnable = true;
						opts.simul = false;
						opts.qtsimul = false;
						opts.testBench = true;
					} else if (args[i].startsWith("--simul")) {
						String ipAddr = null;
						boolean pass = false;
						int length = args[i].length();
						if (length > 8) {
							if (args[i].charAt(7) == ':') {
								ipAddr = args[i].substring(8);
								String[] ipSegments = ipAddr.split("\\.");
								if (ipSegments.length == 4) {
									int j;
									for (j = 0; j < 4; j++) {
										if (SIPath.isAllDigit(ipSegments[j])) {
											if (Integer.parseInt(ipSegments[j]) > 255)
												break;
										} else
											break;
									}
									if (j == 4)
										pass = true;
								}
							}
						} else if (length == 7)
							pass = true;

						if (pass) {
							Options.setSimulIPaddr(ipAddr);
							opts.simul = true;
							opts.qtsimul = false;
							opts.runnable = false;
						} else {
							OutputManager.printError("",
									"Unrecognized option: " + args[i],
									OutputLevel.FATAL);
							printHelp();
							System.exit(1);
						}
					} else if (args[i].equals("--qtsimul")) {
						opts.qtsimul = true;
						opts.simul = false;
						opts.runnable = false;
					} else if (args[i].equals("--nxt")) {
						opts.nxtControl = true;
						opts.machineInterface = true;
						opts.ccode = true;
						opts.cpp = false;
						opts.strl = false;
						opts.sysj = false;
						opts.rmc = false;
					} else if (args[i].equals("--v"))
						opts.verbose = true;
					else if (args[i].equals("--nomake"))
						opts.noMake = true;
					else if (args[i].startsWith("--platform=")) {
						if (args[i].length() > 11)
							opts.selectPlatform(args[i].substring(11));
					}else if (args[i].startsWith("--remote=")) {
							if (args[i].length() > 9)
								opts.deploymentServer = args[i].substring(9);
					}else if (args[i].startsWith("--username=")) {
						if (args[i].length() > 11)
							opts.deploymentUsername= args[i].substring(11);
					}else if (args[i].startsWith("--key=")) {
						if (args[i].length() > 6)
							opts.deploymentKey = args[i].substring(6);
					} else if (args[i].equals("--m")) {
						opts.machineInterface = true;
					} else if (args[i].equals("--timed")) {
						OutputManager
								.printNotice(
										"",
										"Calls to printf are removed for Timing Analysis",
										OutputLevel.INFO);
						opts.timeAnnotate = true;
						opts.ccode = true;
						opts.runnable = true;

						opts.cpp = false;
						opts.strl = false;
						opts.sysj = false;
						opts.rmc = false;
					} else if (args[i].equalsIgnoreCase("--S")) {
						i++;
						opts.sleepValue = Integer.valueOf(args[i]);
					} else if (args[i].equals("--L")) {
						// This has been deprecated. Use --L=<dir> instead.
						i++;
						String path = args[i];
						if (!path.endsWith(File.separator))
							path += File.separator;
						searchPath.add(path);
					} else if (args[i].startsWith("--L=")) {
						if (args[i].length() > 4) {
							String path = args[i].substring(4);
							if (!path.endsWith(File.separator))
								path += File.separator;
							searchPath.add(path);
						}
					} else if (args[i].startsWith("--lib=")) {
						if (args[i].length() > 6) {
							String file = args[i].substring(6);
							libraryFiles.add(file);
						}
					} else if (args[i].startsWith("--startup=")) {
						if (args[i].length() > 10) {
							String file = args[i].substring(10);
							startUpFile = file;
						}
					} else if (args[i].startsWith("--makeConfig=")) {
						if (args[i].length() > 13) {
							String file = args[i].substring(13);
							makeConfigFile = file;
							FileInputStream f = new FileInputStream(makeConfigFile);
							JSONObject jsonparser = new JSONObject(new JSONTokener(f));
							makeConfig = jsonparser.toMap();
						}
					} else if (args[i].startsWith("--header=")) {
						if (args[i].length() > 9) {
							String file = args[i].substring(9);
							headerFiles.add(file);
						}
					} else if (args[i].startsWith("--string=")) {
						if (args[i].length() > 9) {
							try {
								int len = Integer
										.parseInt(args[i].substring(9));
								if (len > 0)
									Options.setStrlen(len);
							} catch (NumberFormatException e) {
								// Catch and ignore
							}
						}
					} else if (args[i].equals("--O")) {
						// This has been deprecated. Use --O=<dir> instead.
						i++;
						opts.setOutputPath(args[i]);

						// Make path if needed
						File dir = new File(opts.outputpath());
						if (!dir.mkdirs()) {
							if (!dir.exists()) {
								OutputManager.printError("", "Output folder `"
										+ opts.outputpath()
										+ "' creation failed.",
										OutputLevel.FATAL);
								System.exit(1);
							}
						}
						OutputManager.printNotice(
								"",
								"Output files will be stored in "
										+ opts.outputpath(), OutputLevel.INFO);
					} else if (args[i].startsWith("--O=")) {
						if (args[i].length() > 4) {
							opts.setOutputPath(args[i].substring(4));
							// Make path if needed
							File dir = new File(opts.outputpath());
							if (!dir.exists()) {
								if (!dir.mkdirs()) {
									OutputManager.printError(
											"",
											"Output folder `"
													+ opts.outputpath()
													+ "' creation failed.",
											OutputLevel.FATAL);
									System.exit(1);
								}
							}
							OutputManager.printNotice(
									"",
									"Output files will be stored in "
											+ opts.outputpath(),
									OutputLevel.INFO);
						}
					} else if (args[i].equals("--version")) {
						printInfo = true;
						System.out.println("FBC version 1.0.1211052142");
					} else if (args[i].equals("--help")) {
						printInfo = true;
						printHelp();
					} else {
						OutputManager.printError("", "Unrecognized option: "
								+ args[i], OutputLevel.FATAL);
						printHelp();
						System.exit(1);
					}
				} else {
					fileName = args[i];
					File file = new File(fileName);
					inputFileName = file.getName();
				}
			}
			
			
			
			if (!opts.ccode && !opts.strl && !opts.sysj && !opts.rmc && !opts.pretc)
				opts.ccode = true; // default choice

			if (fileName == null || fileName.isEmpty()) {
				if (!printInfo) {
					OutputManager.printError("", "Error: No filename given",
							OutputLevel.FATAL);
					printHelp();
				}
				return;
			}

			boolean sysOrDevPresent = false;

			if (fileName.endsWith(".sys") || fileName.endsWith(".dev"))
				sysOrDevPresent = true;

			CProgGenerator factory = null; // this is a hack to enable
											// "handleLastRites"
											// to be called in a non-static
											// manner
			if (opts.gnuUCOS || opts.d3) {
				String option = null;
				if (opts.gnuUCOS) {
					// Create degenerate UCOSGenerator object
					factory = new UCOSGenerator(null, new HashSet<Block>(),
							null);
					option = "gnu-ucos";
				} else if (opts.d3) {
					// Create degenerate D3Generator object
					factory = new D3Generator(null, new HashSet<Block>(), null);
					option = "d3";
				}
				if (!sysOrDevPresent) {
					OutputManager.printError("",
							"Error: Input file must either be a system (.sys) or a device (.dev) for "
									+ option + "\n" + "platform.",
							OutputLevel.FATAL);
					return;
				}
				if (opts.qtsimul || opts.simul) {
					OutputManager.printError("",
							"Error: Simulation cannot be done for " + option
									+ " platform.", OutputLevel.FATAL);
					return;
				}
			}
			
			if (opts.iotapps == true) { //if plateform is iotapps
				//generate iota interface in output folder
				String iotaappsfilename = "iota_app.fbt";
				File outfile = new File(FBtoStrl.opts.outputpath() + iotaappsfilename);
				OutputStream out = new FileOutputStream(outfile);
				File file = new File(fileName);
				if (!file.exists()) {
					OutputManager.printError("",
							"File does not exist: " + fileName, OutputLevel.FATAL);
					return;
				}
				String path = file.getParent();
				if (!path.endsWith(File.separator))
					path += File.separator;
				searchPath.add(path);
				
				String data = 	"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
								"<!DOCTYPE FBType SYSTEM \"http://www.holobloc.com/xml/LibraryElement.dtd\">\n"+
								"<FBType Name=\"iota_app\" Comment=\"\" >\n" +
								"<Identification Standard=\"61499-2\" />\n" +
								"<VersionInfo Organization=\"IoTapps\" Version=\"0.1\" Author=\"IoTapps\" Date=\"\" />" +
								"<CompilerInfo header=\"\" classdef=\"\">" +
								"</CompilerInfo>\n" +
								"<InterfaceList>\n" +
								"</InterfaceList>\n" +
								"<FBNetwork>\n" +
								"<FB Name=\"top\" Type=\""+CodeGenerator.removeFileExt(file.getName())+"\" x=\"2231.25\" y=\"875\" />\n"+
								"</FBNetwork>\n"+
								"</FBType>\n";
				out.write(data.getBytes());
				out.close();
				fileName = outfile.getAbsolutePath();
				OutputManager.printNotice("", ("Generated IoTapps top level file: "+ fileName), OutputLevel.INFO);
			}

			if (factory == null) {
				// Create degenerate CProgGenerator object
				factory = new CProgGenerator(null, new HashSet<Block>(), null);
			}

			File file = new File(fileName);
			if (!file.exists()) {
				OutputManager.printError("",
						"File does not exist: " + fileName, OutputLevel.FATAL);
				return;
			}
			if (topLevelFile == null)
				topLevelFile = CodeGenerator.removeFileExt(file.getName());

			parent = file.getParent();
			if (parent == null)
				parent = "";
			else {
				parent += File.separator;
				// If nxtControl and the file is a system file in its own Subdir
				// of the 'IEC61499' folder
				if (opts.isNxtControl()
						&& parent.endsWith(topLevelFile + File.separator)) {
					parent = file.getParentFile().getParent();
					parent += File.separator;
				}
			}

			if (opts.outputpath() == null)
				opts.setOutputPath(parent); // use parent folder if output path
											// not set

			if (opts.isNxtControl()) {
				NXTControlHelpers.addExtraSearchPaths(parent, searchPath);
			}
			
			

			FBType fbt;
			if (opts.isSimul())
				fbt = translate(fileName, "root", "", null, null, null); // name
																			// the
																			// 1st
																			// instance
																			// "root"
			else
				fbt = translate(fileName, "", "", null, null, null);

			// Check for any remaining deferred code generators
			if (fbt != null) {
				fbt.runLastDeferredCG(true,
						communicationBlocks.isEmpty() ? null
								: communicationBlocks);
			}

			if (opts.isNxtControl()) {
				NXTControlHMIGenerator.closeConfigFiles();
			}

			if (!sysOrDevPresent && (opts.ccode || opts.pretc) && fbt != null) {
				// "Most recent" topLevelFile only :(
				// TODO: When compiling resources within simulation.. name will
				// be "root_"+fb.name
				factory.handleLastRites(opts, topLevelFile, fbt.name,
						CodeGenerator.srcDependencies,
						CodeGenerator.objDependencies,
						CodeGenerator.includePaths, CodeGenerator.globalVars);
			}
			if (headerFiles.size() > 0) {
				for (int i = 0; i < headerFiles.size(); i++) {
					String headerName = headerFiles.get(i);
					File headerFile = new File(headerName);
					if (headerFile.exists()) {
						OutputManager.printError("", "Header file is absolute path (not copied): " + headerName, OutputLevel.WARNING);
					}else{
						headerFile = new File(parent+headerName);
						String headerFileName = headerFile.getName();
						headerFile = isFBFileExists(headerFile, headerFileName);
						if (headerFile != null) {
							File outfile = new File(FBtoStrl.opts.outputpath() + headerName);
							if (outfile.exists()) {
								OutputManager.printError("", "Header file already exist in output directory (not overwritten): " + headerName, OutputLevel.WARNING);
							}else{
								InputStream in = new FileInputStream(headerFile);
								File filepath = new File(outfile.getParent());
								if (filepath.exists() == false) {
									filepath.mkdirs();
								}
								OutputStream out = new FileOutputStream(outfile);
								byte[] buf = new byte[1024];
								int len;
								while ((len = in.read(buf)) > 0){
								  out.write(buf, 0, len);
								}
								in.close();
								out.close();
							}
						}else{
							OutputManager.printError("", "Header file not found: " + headerName, OutputLevel.FATAL);
						}
					}
				}
			}
			if (libraryFiles.size() > 0) {
				for (int i = 0; i < libraryFiles.size(); i++) {
					String libraryName = libraryFiles.get(i);
					File libraryFile = new File(libraryName);
					if (libraryFile.exists()) {
						OutputManager.printError("", "Library file is absolute path (not copied): " + libraryName, OutputLevel.WARNING);
					}else{
						libraryFile = new File(parent+libraryName);
						String libraryFileName = libraryFile.getName();
						libraryFile = isFBFileExists(libraryFile, libraryFileName);
						if (libraryFile != null) {
							File outfile = new File(FBtoStrl.opts.outputpath() + libraryName);
							
							if (outfile.exists()) {
								OutputManager.printError("", "Library file already exist in output directory (not overwritten): " + libraryName, OutputLevel.WARNING);
							}else{
								InputStream in = new FileInputStream(libraryFile);
								File filepath = new File(outfile.getParent());
								if (filepath.exists() == false) {
									filepath.mkdirs();
								}
								OutputStream out = new FileOutputStream(outfile);
								byte[] buf = new byte[1024];
								int len;
								while ((len = in.read(buf)) > 0){
								  out.write(buf, 0, len);
								}
								in.close();
								out.close();
							}
						}else{
							OutputManager.printError("", "Library file not found: " + libraryName, OutputLevel.FATAL);
						}
					}
				}
			}
			if (opts.iotapps == true) { //if plateform is iotapps
				iotaAppsDeploy();
			}
			
			OutputManager.closeOutput();
		} catch (Exception e) {
			e.printStackTrace();
			OutputManager.closeOutput();
			System.exit(1);
		}
	}
	
	public static void iotaAppsDeploy() throws IOException 
	{
		//Generate Zip file
		
		String zipFile = FBtoStrl.opts.outputpath() + "IoTapp.zip";
		byte[] buffer = new byte[1024];
		ZipOutputStream zipstream = new ZipOutputStream(new FileOutputStream(zipFile));
		Hashtable<File,String> relativePath = new Hashtable<File,String>();
		File outputPath = new File(FBtoStrl.opts.outputpath());
		if (outputPath.isDirectory() == false) {
			OutputManager.printError("", "Error output path is not a directory", OutputLevel.FATAL);
		}else{
			ArrayList<File> files = new ArrayList<File>(Arrays.asList(outputPath.listFiles()));
			
			while(files.size() > 0) {
				File f = files.remove(0);
				if (f.isDirectory()) {
					ArrayList<File> nestedFiles = new ArrayList<File>(Arrays.asList(f.listFiles()));
					files.addAll(nestedFiles);
					String prefix = "";
					if (relativePath.get(f) != null) {
						prefix = relativePath.get(f)+File.separator;
					}
					for (File nf : nestedFiles) {
						relativePath.put(nf, prefix+f.getName());
					}
				}else{
					if (f.getPath().equals(zipFile)) { //ignore self
						continue;
					}
					FileInputStream inputstream = new FileInputStream(f);
					// begin writing a new ZIP entry, positions the stream to the start of the entry data
					String prefix = "";
					if (relativePath.get(f) != null) {
						prefix = relativePath.get(f)+File.separator;
					}
					String entryName = prefix + f.getName();
					zipstream.putNextEntry(new ZipEntry(entryName));
					int length;
					while ((length = inputstream.read(buffer)) > 0) {
						zipstream.write(buffer, 0, length);
					}
					zipstream.closeEntry();
					inputstream.close();
					
				}
				
			}
			zipstream.close();
			
			if (opts.deploymentServer.length() > 0) { //remote deployment
				if (opts.deploymentUsername.length()==0) {
					OutputManager.printError("", "Remote deployment set but no username specified - not deployed", OutputLevel.ERROR);
				}
				if (opts.deploymentKey.length() == 0) {
					OutputManager.printError("", "Remote deployment set but no deployment key specified - not deployed", OutputLevel.ERROR);
				}
				
				if (opts.deploymentServer.length() > 0 && opts.deploymentKey.length() > 0) {
					
	
					//prepare query
				
					/*String CRLF = "\r\n"; // Line separator required by multipart/form-data.
					String url = opts.deploymentServer;
					
					URLConnection connection = new URL(url).openConnection();
					HttpURLConnection httpConnection = (HttpURLConnection) new URL(url).openConnection();
					httpConnection.setRequestMethod("POST");
					httpConnection.setDoOutput(true); // Triggers POST.
					String boundary = Long.toHexString(System.currentTimeMillis()); // Just generate some unique random value.

					httpConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

			
					
					
		
					File binaryFile = new File(zipFile);
					InputStream response = null;
					try (OutputStream output = httpConnection.getOutputStream();
						PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8.name()), true);) {
						// Send username
					    writer.append("--" + boundary).append(CRLF);
					    writer.append("Content-Disposition: form-data; name=\"username\"").append(CRLF);
					    writer.append("Content-Type: text/plain; charset=" + StandardCharsets.UTF_8.name()).append(CRLF);
					    writer.append(CRLF).append(opts.deploymentUsername).append(CRLF).flush();
					    
					    // Send key
					    writer.append("--" + boundary).append(CRLF);
					    writer.append("Content-Disposition: form-data; name=\"deploymentKey\"").append(CRLF);
					    writer.append("Content-Type: text/plain; charset=" + StandardCharsets.UTF_8.name()).append(CRLF);
					    writer.append(CRLF).append(opts.deploymentKey).append(CRLF).flush();
					    

					    
					    // Send file
					    writer.append("--" + boundary).append(CRLF);
					    writer.append("Content-Disposition: form-data; name=\"binaryFile\"; filename=\"" + binaryFile.getName() + "\"").append(CRLF);
					    writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(binaryFile.getName())).append(CRLF);
					    writer.append("Content-Transfer-Encoding: binary").append(CRLF);
					    writer.append(CRLF).flush();
					    Files.copy(binaryFile.toPath(), output);
					    output.flush(); // Important before continuing with writer!
					    
					    // End of multipart/form-data.
					    writer.append("--" + boundary + "--").append(CRLF).flush();
					    writer.close();
					    output.close();
					    response = connection.getInputStream();
					}catch (IOException e) {
						e.printStackTrace();
					}
					for (Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
					    System.out.println(header.getKey() + "=" + header.getValue());
					}*/
					
			
					URL url = new URL(opts.deploymentServer + "?username="+opts.deploymentUsername+"&deployment_key="+opts.deploymentKey);
					URLConnection connection = url.openConnection();
					connection.setDoInput(true);
					connection.setDoOutput(true);

					connection.connect();

					OutputStream os = connection.getOutputStream();
					File binaryFile = new File(zipFile);
					Files.copy(binaryFile.toPath(), os);
					os.close();

					InputStream is = connection.getInputStream();
					BufferedReader reader = new BufferedReader(new InputStreamReader(is));
					String line = null;
					StringBuffer sb = new StringBuffer();
					while ((line = reader.readLine()) != null) {
					    sb.append(line);
					}
					is.close();
					String response = sb.toString();
					System.out.println(response);
					
					//connection.
				}
			}
			
			
		}
	}

	/*
	 * gareth stole this from http://dataml.net/articles/strings.htm
	 */
	public static String XmlEncode(String text) {

		int[] chars = { '&', '\"', '<', '>', '\n' };
		for (int i = 0; i < chars.length; i++) {
			text = text.replaceAll(String.valueOf((char) chars[i]), "&#"
					+ chars[i] + ";");
		}
		return text;
	}

	public static boolean isAdapter(String type) {
		return type.contains("Plug") || type.contains("Socket");
	}
}
