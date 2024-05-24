/* CodePrinter.java
 * Class for pretty printing code.
 * 
 * Written by: Li Hsien Yoong
 */

package ir;

import java.io.*;

import fbtostrl.*;

public class CodePrinter {
	protected BufferedWriter outBuf;
	private StringBuilder tempBuf;
	protected int indent = 0;
	private String indentWidth;
	private boolean bufMode;
	public static String newLine = System.getProperty("line.separator");
	
	public CodePrinter() {
		indentWidth = "    ";
	}
	
	public CodePrinter(String fileName) {
		try {
			outBuf = new BufferedWriter(new FileWriter(fileName));
		}
		catch (IOException e) {
			OutputManager.printError("", fileName + ": Could not be opened.\n", OutputLevel.FATAL);
			System.exit(-1);
		}
		indentWidth = "    ";
	}
	
	public CodePrinter(String fileName, boolean append) {
		try {
			outBuf = new BufferedWriter(new FileWriter(fileName, append));
		}
		catch (IOException e) {
			OutputManager.printError("", fileName + ": Could not be opened.\n", OutputLevel.FATAL);
			System.exit(-1);
		}
		indentWidth = "    ";
	}
	
	public CodePrinter(String fileName, int width) {
		try {
			outBuf = new BufferedWriter(new FileWriter(fileName));
		}
		catch (IOException e) {
			OutputManager.printError("", fileName + ": Could not be opened.\n", OutputLevel.FATAL);
			System.exit(-1);
		}
		indentWidth = makeIndent(width);
	}
	
	public final void close() throws IOException {
		outBuf.close();
	}
		
	public final void indent() {
		indent++;
	}
	
	public final void indent(int times) {
		indent = indent + times;
	}
	
	public final void unindent() {
		indent--;
	}
	
	public final void unindent(int times) {
		indent = indent - times;
	}
	
	public void flushBuf() throws IOException {
		if (tempBuf != null) {
			outBuf.write(tempBuf.toString());
			tempBuf.delete(0, tempBuf.length());
		}
	}
	
	public String getTempBuffer() {
		return tempBuf.toString();
	}
	
	// TODO: GARETH says: I don't like this function writing newLine's!! Matthew says: neither do I
	public final void print(String line) throws IOException {
		if (bufMode) {
			for (int i = 0; i < indent; i++)
				tempBuf.append(indentWidth);
			tempBuf.append(line);
			tempBuf.append(newLine);
		}
		else {
			for (int i = 0; i < indent; i++)
				outBuf.write(indentWidth);
			outBuf.write(line);
			outBuf.append(newLine);
		}
	}
	
	public final void print(String line, int l) throws IOException {
		print(line);
		if (bufMode) {
			for (int i = 0; i < l; i++) 
				tempBuf.append(newLine);
		}
		else {
			for (int i = 0; i < l; i++) 
				outBuf.write(newLine);
		}
	}
	
	public final void printNoIndent(String line) throws IOException {		
		outBuf.write(line);
	}
	
	// TODO: THIS function should print line + newLine...
	public final void println(String line) throws IOException
	{
		print(line); 
	}
	
	public final void println() throws IOException
	{
		printNoIndent(newLine);
	}
	
	public final void smartPrint(String line) throws IOException {
		if (line.startsWith("}"))
			unindent();
		if (!line.startsWith("#")) {
			for (int i = 0; i < indent; i++)
				outBuf.write(indentWidth);
		}
		outBuf.write(line);
		outBuf.append(newLine);
		if (line.endsWith("{"))
			indent();
	}
	
	public final void smartPrint(String line, int l) throws IOException {
		smartPrint(line);
		for (int i = 0; i < l; i++)
			outBuf.write(newLine);
	}
	
	public void writeToBuf() {
		bufMode = true;
		if (tempBuf == null)
			tempBuf = new StringBuilder();
	}
	
	public void writeToFile() {
		bufMode = false;
	}
	
	private final String makeIndent(int width) {
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < width; i++)
			str.append(' ');
		return str.toString();
	}
}
