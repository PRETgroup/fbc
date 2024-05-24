/* XMLParser.java
 * Parses an XML file to create XML Document object.
 * 
 * Written by: Cheng Pang
 * Modified by: Li Hsien Yoong
 */

package fbtostrl;

import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.*;
import org.jdom2.transform.*;
import org.xml.sax.*;
import java.io.*;
import java.util.List;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamSource;

public class XMLParser {
	// Fields:
	public static XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());

	// Methods:
	public XMLParser() {}
	
	/**
	 * Extract the appropriate DTD file from the input XML file.
	 * @param inputXML function block file to be parsed
	 * @return The DTD file that ought to be used to validate the given input file.
	 * @throws IOException
	 */
	private static String getDTDfile(File inputXML) {
		try {
			BufferedReader bufInput = new BufferedReader(new FileReader(inputXML));
			String line;
			while ((line = bufInput.readLine()) != null) {
				int index = line.indexOf(".dtd\"");
				if (index >= 0) {
					line = line.substring(0, index + 4);
					for (int i = index - 1; i >= 0; i--) {
						char c = line.charAt(i);
						if (c == '\"' || c == '/') {
							line = line.substring(i + 1);
							break;
						}
					}
					break;
				}
			}
			bufInput.close();
			return line;
		} catch (FileNotFoundException e) {
			OutputManager.printError("", "Could not open `" + inputXML.getPath() + "\'", OutputLevel.FATAL);
		} catch (IOException e) {
			OutputManager.printError("", "Could not read `" + inputXML.getPath() + "\'", OutputLevel.FATAL);
		}
		
		// WILL NEVER REACH HERE UNLESS AN ERROR HAS OCCURRED!
		System.exit(1);
		return null;
	}
	
	/* Parse the file "inputXMLFile" with/without "validation" and builds a 
	 * corresponding Document object and returns it. 
	 */
	public final static Document parse(String inputXMLFile, boolean validation) {
		Document xmlDoc = null;
		File inputXML = new File(inputXMLFile);
		if(!inputXML.exists()) {
			OutputManager.printError("", inputXMLFile + " does not exist.", OutputLevel.FATAL);
			System.exit(0);
		}
		else {
			// Issue # 6 https://bitbucket.org/GarethNZ/fbc/issue/6/xml-validation-to-ensure-correct-code-will
			// Parser should validate and fail, showing error
			// But then re-try without validation
			File path = new File("." + File.separator + "resources" + File.separator + "dtd" + 
					File.separator + getDTDfile(inputXML));
			if ( !path.exists() ) {
				// O-oh... this is unexpected! The required DTD does not exist in our 
				// resources folder. We simply default to LibraryElement.dtd and hope for
				// the best.
				path = new File("." + File.separator + "resources" + File.separator + "dtd" + 
						File.separator + "LibraryElement.dtd");
			}
			
			try {
				//URL dtdURL = XMLParser.class.getResource("/fbtostrl"); 
				//File dtdPath = new File(dtdURL.toString());
				//dtdPath = dtdPath.getParentFile().getParentFile();
				SAXBuilder saxBuilder = getSaxBuilder(validation);
				saxBuilder = changeDTD(saxBuilder, path);
				return saxBuilder.build(inputXMLFile);
			} catch(JDOMException e) {
				// Print a nicer validation error output
				OutputManager.printNotice(inputXML.getName(), "Warning invalid XML found: " + 
						e.getMessage() + "\nAttempting to generate code anyway.", 
						OutputLevel.WARNING);
				if (validation) {
					// Ignore exception and redo without
					SAXBuilder saxBuilder = getSaxBuilder(false);
					saxBuilder = changeDTD(saxBuilder, path);
					
					try {
						xmlDoc = saxBuilder.build(inputXMLFile);	// if this fails, give up
					} catch (JDOMException f) {
						OutputManager.printError(inputXML.getName(), "XML parsing failed: " + 
								f.getMessage(), OutputLevel.FATAL);
						System.exit(1);
					} catch (IOException f) {
						OutputManager.printError(inputXML.getName(), "XML parsing failed: " + 
								f.getMessage(), OutputLevel.FATAL);
						System.exit(1);
					}
				}
				else {
					// If parsing failed even without validation, there's nothing left to be done.
					OutputManager.printError(inputXML.getName(), "XML parsing failed: " + 
							e.getMessage(), OutputLevel.FATAL);
					System.exit(1);
				}
			} catch (IOException e) {
				OutputManager.printError(inputXML.getName(), "XML parsing failed: " + 
						e.getMessage(), OutputLevel.FATAL);
				System.exit(1);
			}
		}
		return xmlDoc;
	}

	/* Parse the stream "inputXML" with/without "validation" and builds a 
	 * corresponding Document object and returns it.
	 */
	public final static Document parse(StringReader inputXML, boolean validation)
			throws IOException, JDOMException {
		return getSaxBuilder(validation).build(inputXML);
	}

	/* Creates a SAX2 parser and returns it. */
	public final static SAXBuilder getSaxBuilder(boolean validation) {
		SAXBuilder saxBuilder = new SAXBuilder("org.apache.xerces.parsers.SAXParser", 
				!FBtoStrl.opts.isNxtControl() && validation); // no validation for nxtControl atm
		
		if( FBtoStrl.opts.isNxtControl() )
		{
		// Set schema support
			saxBuilder.setFeature("http://apache.org/xml/features/validation/schema", 
					validation);
		}
		
		return saxBuilder;
	}
	
	/**
	 * Use a new DTD, usually a local copy of the original remote DTD, to 
	 * validate the XML file. This method won't change the XML's DOCTYPE 
	 * declaration, it only validates it against the new DTD.
	 * 
	 * @param SAXBuilder saxBuilder
	 * @param String newDTD
	 * @return
	 */
	public final static SAXBuilder changeDTD(SAXBuilder saxBuilder, final File newDTD) {
		saxBuilder.setEntityResolver(new EntityResolver() {
			//@Override
			public InputSource resolveEntity(String publicId, String systemId) throws SAXException,
					IOException {
				InputStream inputStream = new FileInputStream(newDTD);
				InputSource inputSource = new InputSource(inputStream);
				inputSource.setPublicId(publicId);
				inputSource.setSystemId(systemId);
				return inputSource;
			}
		});
		return saxBuilder;
	}
	
	
	

	/**
	 *  Write the XML Document "doc" to an external file "outputXMLFile"
	 *  @param outputXMLFile - output filename
	 *  @param doc - DOM document
	*/
	public final static void writeToFile(String outputXMLFile, Document doc)
			throws IOException {
		FileOutputStream outStream = new FileOutputStream(outputXMLFile);
		xmlOutputter.output(doc, outStream);
		outStream.flush();
		outStream.close();
	}

	/* Create an XML Document from an XML string "xmlString" and write it to an 
	 * external file "outputXMLFile".
	 */
	public final static void writeToFile(String outputXMLFile, String xmlString)
			throws IOException, JDOMException {
		writeToFile(outputXMLFile, XMLParser.parse(new StringReader(xmlString), false));
	}

	/* Print out the XML Document to the standard output */
	public final static void writeToScreen(Document doc) {
		try {
			xmlOutputter.output(doc, System.out);
			System.out.println();
		}
		catch(IOException e) {
			OutputManager.printError("", e.getMessage(), OutputLevel.FATAL);
			System.exit(0);
		}
	}

	/* Print out the XML Element to the standard output */
	public final static void writeToScreen(Element elem) {
		try {
			xmlOutputter.output(elem, System.out);
			System.out.println();
		}
		catch(IOException e) {
			OutputManager.printError("", e.getMessage(), OutputLevel.FATAL);
			System.exit(0);
		}
	}

	/* Validates the XML Document "doc". */ 	 
	public final static Document validator(Document doc) throws JDOMException,
			IOException {
		return getSaxBuilder(true).build(
				new StringReader(xmlOutputter.outputString(doc)) );
		// return saxBuilder.build(new
		// StringReader(xmlOutputter.outputString(doc)));
	}

	/* Validates the XML string "input". */ 
	public final static Document validator(String input) throws JDOMException,
			IOException {
		return getSaxBuilder(true).build(new StringReader(input));
	}

	/* Validates the XML stream "strReader". */ 
	public final static Document validator(StringReader strReader)
			throws JDOMException, IOException {
		return getSaxBuilder(true).build(strReader);
	}

	// Transform the given Document object according to the specified XSLT file
	// This function utilises the JDOMResult and JDOMSource objects
	public final static Document transformer(String xsltFile, Document inputXML) {
		Document outputXML = null;
		StreamSource streamSrc = new StreamSource(xsltFile);
		TransformerFactory tFactory = TransformerFactory.newInstance();

		try {
			Transformer transformer = tFactory.newTransformer(streamSrc);
			JDOMResult jdomRes = new JDOMResult();
			JDOMSource jdomSrc = new JDOMSource(inputXML);
			transformer.transform(jdomSrc, jdomRes);

			outputXML = jdomRes.getDocument();
		}
		catch(TransformerConfigurationException e) {
			e.printStackTrace();
			OutputManager.closeOutput();
			System.exit(0);
		}
		catch(TransformerException e) {
			e.printStackTrace();
			OutputManager.closeOutput();
			System.exit(0);
		}

		return outputXML;
	}

	// Alternative approach to last funciton
	public final static Document simpleTransformer(String xsltFile,
			Document inputXML) {
		Document outputXML = null;

		XSLTransformer transformer;
		try {
			transformer = new XSLTransformer(xsltFile);
			outputXML = transformer.transform(inputXML);
		}
		catch(XSLTransformException e) {
			e.printStackTrace();
		}
		return outputXML;
	}

	// This function searches for the elements in the "inputDoc" with the name
	// "nodeName" and type "nodeType"
	public final static List<Element> search(String nodeName, Object nodeType,
			Document inputDoc) {

		List<Element> searchResult = null;

		return searchResult;
	}

}
