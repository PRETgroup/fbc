/* ProgramInfo.java
 * Helper class to store global program information to allow deferred 
 * device-level code generation.
 *
 * Written by: Li Hsien Yoong
 */

package ccode;

import fb.Device;
import fbtostrl.Options;
import ir.GlobalVar;

import java.io.IOException;
import java.util.*;

public class ProgramInfo {
	private CProgGenerator progGenerator;
	private HashMap<String, GlobalVar> globalVars;
	private HashSet<String> srcDependencies;
	private HashSet<String> objDependencies;
	private HashSet<String> includePaths;
	
	public ProgramInfo(CProgGenerator prog, HashMap<String, GlobalVar> globals,
			HashSet<String> srcs, HashSet<String> objs, HashSet<String> incs) {
		progGenerator = prog;
		globalVars = globals;
		srcDependencies = srcs;
		objDependencies = objs;
		includePaths = incs;
	}
	
	public final Device getDevice() {
		return progGenerator.getDevice();
	}
	
	public final String getDeviceName() {
		return progGenerator.getDevice().name;
	}
	
	public final String outputpath() {
		return progGenerator.outputpath(); 
	}
	
	public final String sysOutputPath() {
		return progGenerator.sysOutputPath();
	}
	
	public final HashMap<String, GlobalVar> getGlobalVars() {
		return globalVars;
	}
	
	public final HashSet<String> getSrcDependencies() {
		return srcDependencies;
	}
	
	public final HashSet<String> getObjDependencies() {
		return objDependencies;
	}
	
	public final HashSet<String> getIncludePaths() {
		return includePaths;
	}
	
	public void handleLastRites(Options opts, Device devType, String insName) 
			throws IOException {
		progGenerator.handleLastRites(outputpath(), opts, devType, insName, 
				srcDependencies, objDependencies, includePaths, globalVars);
	}
}
