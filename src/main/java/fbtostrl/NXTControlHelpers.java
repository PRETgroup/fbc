package fbtostrl;

import java.io.File;
import java.util.ArrayList;

public class NXTControlHelpers {
	public static File sourceFolder;
	
	public static void addExtraSearchPaths(String folder, ArrayList<String> searchPath) {
		
		sourceFolder = new File(folder);
		
		// Add sub-folders to searchpath
		 File[] fileList = sourceFolder.listFiles();

	        //for each file in the folder
	        for (int i = 0; i < fileList.length; i++) {
	        	
	        	//If the file is a Directory(folder) add it
	            File choose = fileList[i];
	            if ( choose.isDirectory() ) {
	            	searchPath.add(choose.getAbsolutePath()+File.separatorChar);
	            }
	        }
	     // Add special SIFB
	        searchPath.add("resources"+File.separatorChar+"sifb"+File.separatorChar+"nxtControl"+File.separatorChar);
	        
	}
}
