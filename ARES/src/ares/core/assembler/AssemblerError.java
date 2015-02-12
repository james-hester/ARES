package ares.core.assembler;


public class AssemblerError extends Exception {

	private static final long serialVersionUID = 6519463169427192923L;

	private String filename, errDetail = "";
	private int line, errID;
	
	
	/**
	 * Creates a new AssemblerError, signifying any fatal error in assembly.
	 * AssemblerErrors are designed to be thrown even from deep within the bowels of
	 * FirstPass and SecondPass, and then caught by an error handler which has access
	 * to the data necessary to construct a meaningful error message (viz. the filename,
	 * line number, etc.)
	 * All that is necessary to construct an AssemblerError is an error ID: these are
	 * listed below.
	 * @param errID the error ID.
	 */
	public AssemblerError(int errID)
	{
		super();
		this.errID = errID;
		line = 0;
		filename = "<unknown>";
	}
	
	/**
	 * Occasionally there is some information necessary in creating a good error message
	 * that the main error handler does not have access to. 
	 * @param errID
	 * @param errDetail
	 */
	public AssemblerError(int errID, String errDetail)
	{
		this(errID);
		this.errDetail = errDetail;
	}
	
	/**
	 * 
	 * @param filename
	 * @param line
	 */
	public void setFileInformation(String filename, int line)
	{
		this.filename = filename;
		this.line = line;
	}
	
	
	@Override
	public String toString()
	{
		String result = "";
			result += "- ASSEMBLER ERROR\n";
			result += "-\tFile:\t" + filename + "\n";
			result += "-\tLine:\t" + Integer.toString(line) + "\n";
			result += "-\tCause:\t" + getErrorMessage() + "\n";
		return result;
	}
	
	public String getErrorMessage()
	{
		String result = "";
		switch(errID)
		{
		case 0:
			return result + "Segment declared twice. .data, .text, etc. can appear only once.";
		case 1:
			return result + "Argument for .align invalid (must be an integer between 0 and 3.)";
		case 2:
			return result + "Argument for .align out of bounds (must be between 0 and 3.)";
		case 3:
			return result + "Invalid/unsupported directive in data segment.";
		case 4:
			return result + "Arguments for pseudoinstructions may only be integers or registers.";
		case 5:
			return result + "Unrecognized postfix in integer literal: " + errDetail;
		default:
			return result + "<unknown error: " + Integer.toString(errID) + ">";	
		}
	}
	
}
