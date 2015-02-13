package ares.core;

public class Coprocessor0 extends AbstractCoprocessor
{
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
		super(15);
		writeRegister(STATUS, 0x0000FF03);
	}

	public boolean interruptsEnabled()
	{
		return (readRegister(STATUS) % 2 == 1);
	}
		
	public boolean inUserMode()
	{
		return ((readRegister(STATUS) >> 1) % 2 == 1);
	}
	
	public int exceptionAddress(MIPSException currentException)
	{
		return 0x80000080;
	}

	@Override
	public void doOperation(int which)
	{
		return;	
	}
}
