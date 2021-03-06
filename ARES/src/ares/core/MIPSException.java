package ares.core;

/**
 * Represents a MIPS exception.
 * It is intended to be instantiated in statements like
 * 		<p>   exceptionVector.add(new MIPSException(...).setXX(...).setXY(...));   <p>
 * and, therefore, its setter methods all return a MIPSException.
 * (These calls do not create new objects; they all return {@code this}.)
 * @author James Hester
 *
 */
public class MIPSException
{
	private int pc, cause, badvaddr = 0, coprocessorUnusable = 0;
	private boolean branchDelay = false;
	
	public static final int INTERRUPT 					= 0;
	public static final int TLB_PROTECTION_FAULT 		= 1;
	public static final int TLB_MISS_ON_LOAD 			= 2;
	public static final int TLB_MISS_ON_STORE 			= 3;
	public static final int ADDRESS_ERROR_ON_LOAD 		= 4;
	public static final int ADDRESS_ERROR_ON_STORE 		= 5;
	public static final int EXTERNAL_BUS_ERROR_ON_FETCH = 6;
	public static final int EXTERNAL_BUS_ERROR_DATA 	= 7;
	public static final int SYSCALL 					= 8;
	public static final int BREAK 						= 9;
	public static final int ILLEGAL_INSTRUCTION 		= 10;
	public static final int COPROCESSOR_UNUSABLE 		= 11;
	public static final int OVERFLOW 					= 12;
	
	
	public MIPSException(int cause, int pc, boolean branchDelay)
	{
		this.pc = pc;
		this.cause = cause;
		this.branchDelay = branchDelay;
		
	}
	
	public MIPSException setBadVAddr(int badvaddr)
	{
		this.badvaddr = badvaddr;
		return this;
	}
	
	public MIPSException setCoprocessorUnusable(int which)
	{
		this.coprocessorUnusable = which;
		return this;
	}
	
	/**
	 * Gets the EPC:
	 * "The return instruction in the EPC is the address of the instruction that caused the exception or,
	 * if the instruction was executing in a branch delay slot, the address of <b>the branch instruction
	 * immediately preceding the delay slot.</b> (emphasis added)" (MIPS RISC Architecture, Kane & Heinrich)
	 * @return the EPC
	 */
	public int getPC()
	{
		return (branchDelay ? pc - 4 : pc);
	}
	
	public int getCause()
	{
		int causeReg = 0;
		causeReg += (branchDelay ? (1 << 31) : 0);
		causeReg += ((coprocessorUnusable & 0b11) << 28); 
		causeReg += (cause << 2);
		return causeReg;
	}
	
	public int getBadVAddr()
	{
		return badvaddr;
	}
	
}
