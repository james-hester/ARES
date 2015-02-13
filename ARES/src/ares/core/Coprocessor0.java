package ares.core;

public class Coprocessor0 
{
	private int[] registers = new int[32];
	

	public static final int INDEX = 0;
	public static final int	RANDOM = 1;
	public static final int	ENTRYLO = 2;
	public static final int	CONTEXT = 4;
	public static final int	BADVADDR = 8;
	public static final int	ENTRYHI = 10;
	public static final int	STATUS = 12;
	public static final int	CAUSE = 13;
	public static final int	EPC = 14;

	
	public Coprocessor0()
	{
		registers[STATUS] = 0x0000FF11;
	}
	
	public void writeRegister(int which, int data)
	{
		registers[which] = data;
	}
	
	public int readRegister(int which)
	{
		return registers[which];
	}

	public int exceptionAddress(MIPSException currentException)
	{
		return 0x80000080;
	}
}
