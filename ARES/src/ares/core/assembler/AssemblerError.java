package ares.core.assembler;


public class AssemblerError extends Exception {

	private static final long serialVersionUID = 6519463169427192923L;

	private String filename, errDetail = "";
	private int line;
	private ErrorID errID;
	
	public enum ErrorID
	{
		SEG_DECLARED_TWICE,
		ALIGN_ARG_NOT_INTEGER,
		ALIGN_ARG_INVALID,
		DATA_SEG_DIRECTIVE_INVALID,
		PSI_ARGUMENT_TYPE_INVALID,
		LITERAL_POSTFIX_INVALID,
		CONST_NOT_INTEGER,
		SPACE_NEGATIVE,
		SPACE_ARG_INVALID,
	}
	
	
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
	public AssemblerError(ErrorID errID)
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
	public AssemblerError(ErrorID errID, String errDetail)
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
		case SEG_DECLARED_TWICE:
			return result + "Segment declared twice. .data, .text, etc. can appear only once.";
		case ALIGN_ARG_NOT_INTEGER:
			return result + "Argument for .align invalid (must be an integer between 0 and 3.)";
		case ALIGN_ARG_INVALID:
			return result + "Argument for .align out of bounds (must be between 0 and 3.)";
		case DATA_SEG_DIRECTIVE_INVALID:
			return result + "Invalid/unsupported directive in data segment.";
		case SPACE_NEGATIVE:
			return result + "Argument for .space cannot be a negative number.";
		case SPACE_ARG_INVALID:
			return result + "Argument for .space must be a non-negative integer.";
		case PSI_ARGUMENT_TYPE_INVALID:
			return result + "Arguments for pseudoinstructions may only be integers or registers.";
		case LITERAL_POSTFIX_INVALID:
			return result + "Unrecognized postfix in integer literal: " + errDetail;
		default:
			return result + "<unknown error: " + errID.name() + ">";	
		}
	}
	
}
