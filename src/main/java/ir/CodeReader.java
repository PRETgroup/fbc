package ir;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * File reader class to read text files
 * @author Matthew Kuo
 *
 */
public class CodeReader {
	public String fullfilename = "";
	public String path = "";
	public String filename = "";
	public String data = "";
	public ArrayList<String> dataLines = new ArrayList<String>();
	/**
	 * Opens the file and store the file information in the public variables
	 * then closes the file , this reads the file line by line
	 * @param fullfilename file name including path
	 */
	public CodeReader(String fullfilename) {
		this.fullfilename = fullfilename;
		try {
			File CfileFILE = new File(fullfilename);
			BufferedReader Cfile = new BufferedReader(new FileReader(CfileFILE));
			path = CfileFILE.getParent()+File.separator;
			filename = CfileFILE.getName().substring(0, CfileFILE.getName().lastIndexOf('.'));
			String line = "";
			for(;;line = Cfile.readLine()) {
				if (line == null) {
					break;
				}
				data += line+'\n';
				dataLines.add(line+'\n');
			}
			Cfile.close();
		}catch(IOException e){
			System.out.println("file open error" + e);
		}
		//line = "";
	}
	
	/**
	 * Opens the file and store the file information in the public variables
	 * then closes the file , this reads the file in chucks for faster access
	 * @param fullfilename file name including path
	 * @param mode any integer number (currently not used) number is to invoke this method
	 */
	public CodeReader(String fullfilename, int mode) {
		this.fullfilename = fullfilename;
        InputStream in = null;

        byte[] buf = null; // output buffer

        int    bufLen = 10000*1024;
		try {
	         in = new BufferedInputStream(new FileInputStream(fullfilename));

	         buf = new byte[bufLen];

	         byte[] tmp = null;

	         int len    = 0;

	         //List data  = new ArrayList(24); // keeps peaces of data

	         while((len = in.read(buf,0,bufLen)) != -1){

	            //tmp = new byte[len];

	            //System.arraycopy(buf,0,tmp,0,len); // still need to do copy
	        	data += new String(buf,0,len);
	            //data.add(tmp);

	         }
		}catch(IOException e){
			System.out.println("file open error" + e);
		}
		//line = "";
	}	
	
	
	
	/**
	 * Opens the file and store the file information in the public variables
	 * then closes the file , this reads the file line by line
	 * extra option to not read the data but only file path information is provided
	 * @param fullfilename file name including path
	 * @param readdata true if data is to be read
	 */
	public CodeReader(String fullfilename,boolean readdata) {
		this.fullfilename = fullfilename;
		try {
			File CfileFILE = new File(fullfilename);
			BufferedReader Cfile = new BufferedReader(new FileReader(CfileFILE));
			path = CfileFILE.getParent()+File.separator;
			filename = CfileFILE.getName().substring(0, CfileFILE.getName().lastIndexOf('.'));
			String line = "";
			if (readdata) {
				for(;;line = Cfile.readLine()) {
					if (line == null) {
						break;
					}
					data += line+'\n';
				}
			}
			Cfile.close();
		}catch(IOException e){
			System.out.println("file open error" + e);
		}
		//line = "";
	}
	
	/**
	 * read the file data again or reads the file data when created with
	 * the constructor CFileReader(String fullfilename,boolean readdata)
	 * with readdata set to false
	 */
	public void readData() {
		try {
			File CfileFILE = new File(fullfilename);
			BufferedReader Cfile = new BufferedReader(new FileReader(CfileFILE));
			String line = "";
			for(;;line = Cfile.readLine()) {
				if (line == null) {
					break;
				}
				data += line+'\n';
			}
			Cfile.close();
		}catch(IOException e){
			System.out.println("file open error" + e);
		}
	}
}
