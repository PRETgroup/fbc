package fbtostrl;

public class OutputManager {
	private static boolean errorPrinted = false;
	private static boolean noticePrinted = false;

	public OutputLevel level = OutputLevel.DEBUG;
	
	// blockid can be Name OR InstanceName
	public static void printError(String blockid, String message, OutputLevel outputLevel) 
	{
		if( !errorPrinted  && FBtoStrl.opts.machineInterface )
		{
			System.err.println("<Errors>");
			errorPrinted = true;
		}
		
		
		if(FBtoStrl.opts.machineInterface)
		{
			System.err.println("<Error><BlockID=\""+blockid+"\"/><Message=\""+message+"\"/></Error>");
		}
		else
		{
			System.err.println("Error in block " + blockid+": " + message);
		}
		
		if( outputLevel == OutputLevel.FATAL)
		{
			closeOutput(); // assume this comes before a system.exit()
		}
	}
	
	// blockid can be Name OR InstanceName
	public static void printNotice(String blockid, String message, OutputLevel outputLevel)
	{
		if( !noticePrinted && FBtoStrl.opts.machineInterface)
		{
			System.out.println("<Notices>");
			noticePrinted = true;
		}
		
		if(FBtoStrl.opts.machineInterface)
		{
			System.out.println("<Notice><BlockID=\""+blockid+"\"/><Message=\""+message+"\"/></Notice>");
		}
		else
		{
			System.out.println("Notice: " + blockid + " - " + message);
		}
	}
	
	public static void closeOutput()
	{
		if(FBtoStrl.opts.machineInterface)
		{
			if( errorPrinted )
				System.err.println("</Errors>");
			if( noticePrinted )
				System.out.println("</Notices>");
		}
		
	}
	
}
